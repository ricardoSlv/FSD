import java.nio.charset.StandardCharsets;

import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

import java.net.Socket;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import spullara.nio.channels.FutureServerSocketChannel;
import spullara.nio.channels.FutureSocketChannel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatPeerServer {

    public static void main(String[] args) throws Exception {

        String port = System.console().readLine();
        String peer1 = System.console().readLine();
        String peer2 = System.console().readLine();
        String[] peers = { port, peer1, peer2 };
        System.out.println(port);

        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        NettyMessagingService ms = new NettyMessagingService("chatpeer", Address.from(Integer.parseInt(port)),
                new MessagingConfig());

        new CompFutServer().start();
        try {
            Thread.sleep(100, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Socket socket = new java.net.Socket("localhost", 3000);
        if(socket.toString().equals("VsCodeIsAnnoyingMeBecauseImNotClosingTheSocket"))
            socket.close();
        
        PrintWriter socketOut = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // PrevMsg tries to handle message repetition and endless cicles,
        // Those occur because when i receive a msg from the server i send it to all peers, wich includes myself
        // Also when i send a msg to the server, it send it back to me
        // In both of these cases i'm checking it against the previous message before sending
        // May make mistakes when a user send the same msg twice, wouldnt happen if msgs had ids
        final ArrayList<String> prevMsg = new ArrayList<>(1);
        prevMsg.add("");

        ms.registerHandler("chatpeer", (adress, msg) -> {
            String decodedMsg = new String(msg, StandardCharsets.UTF_8);
            System.out.println("decoded from " + adress);
            System.out.println(decodedMsg);
            if (decodedMsg.equals(prevMsg.get(0)) == false)
                socketOut.println(decodedMsg);
            prevMsg.set(0, decodedMsg);
        }, es);

        ms.start();

        while (true) {
            String msg = socketIn.readLine();
            if (msg.equals(prevMsg.get(0)) == false) {
                prevMsg.set(0, msg);
                for (String peer : peers) {
                    ms.sendAsync(Address.from("localhost", Integer.parseInt(peer)), "chatpeer", msg.getBytes())
                            .exceptionally(t -> {
                                t.printStackTrace();
                                return null;
                            });
                }
            }
        }
    }
}

class CompFutServer extends Thread {

    private static ConcurrentMap<FutureSocketChannel, Boolean> clientsWriting = new ConcurrentHashMap<>();
    private static ConcurrentMap<FutureSocketChannel, ArrayList<ByteBuffer>> delayedMsgs = new ConcurrentHashMap<>();

    private static void acceptConnectRec(FutureServerSocketChannel ssc) {
        CompletableFuture<FutureSocketChannel> sc = ssc.accept();
        sc.thenAccept(s -> {
            ByteBuffer buf = ByteBuffer.allocate(1000);
            clientsWriting.put(s, false);
            delayedMsgs.put(s, new ArrayList<ByteBuffer>());
            System.out.println(clientsWriting.size() + " Clients");
            readSocketRec(s, buf);
        }).thenRun(() -> acceptConnectRec(ssc));
    }

    private static void readSocketRec(FutureSocketChannel s, ByteBuffer buf) {

        CompletableFuture<Integer> read = s.read(buf);
        read.thenAccept(i -> {
            buf.flip();
            if (i < 0) {
                synchronized (s) {
                    System.out.println("Couldnt read, goodbye");
                    s.close();
                    clientsWriting.remove(s);
                    delayedMsgs.remove(s);
                }
            } else {
                writeToAll(buf.duplicate());
                buf.clear();
                readSocketRec(s, buf);
            }
        });
    }

    private static void writeToAll(ByteBuffer buf) {
        for (FutureSocketChannel s : clientsWriting.keySet()) {

            Boolean canWrite;
            synchronized (s) {
                if (clientsWriting.get(s) == true)
                    canWrite = false;
                else {
                    canWrite = true;
                    clientsWriting.put(s, true);
                }
            }

            if (canWrite == false)
                synchronized (s) {
                    delayedMsgs.get(s).add(buf);
                }
            else {
                ArrayList<ByteBuffer> msgList = new ArrayList<>();
                if (delayedMsgs.size() > 0) {
                    synchronized (s) {
                        msgList = delayedMsgs.get(s);
                        delayedMsgs.put(s, new ArrayList<ByteBuffer>());
                    }
                }
                msgList.add(buf.duplicate());
                sendMessageList(msgList, s);
            }
        }
    }

    private static void sendMessageList(ArrayList<ByteBuffer> msgList, FutureSocketChannel s) {

        if (msgList.size() == 0) {
            synchronized (s) {
                clientsWriting.put(s, false);
            }
        } else {
            ByteBuffer msg = msgList.get(0);
            msgList.remove(0);

            s.write(msg.duplicate()).thenAccept(nw -> {
                if (nw == 0) {
                    synchronized (s) {
                        s.close();
                        clientsWriting.remove(s);
                        delayedMsgs.remove(s);
                    }
                }
                // String str = new String(Arrays.copyOf(msg.array(), nw),
                // StandardCharsets.UTF_8);
                // System.out.println("Wrote: " + str + "Size: " + nw + "\nDelayedMsgs: " +
                // msgList.size());
            }).thenRun(() -> sendMessageList(msgList, s));
        }
    }

    @Override
    public void run() {

        FutureServerSocketChannel ssc;
        try {
            ssc = new FutureServerSocketChannel();
            ssc.bind(new InetSocketAddress(3000));

            acceptConnectRec(ssc);
            System.out.println("Server listening on port 3000");

            // Impedir que o programa morra
            Thread.sleep(Integer.MAX_VALUE);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
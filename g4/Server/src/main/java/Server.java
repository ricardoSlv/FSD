import spullara.nio.channels.FutureServerSocketChannel;
import spullara.nio.channels.FutureSocketChannel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Server {

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
                sendMessageRec(msgList, s);
            }
        }
    }

    private static void sendMessageRec(ArrayList<ByteBuffer> msgList, FutureSocketChannel s) {

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
                String str = new String(Arrays.copyOf(msg.array(), nw), StandardCharsets.UTF_8);
                System.out.println("Wrote: " + str + "Size: " + nw + "\nDelayedMsgs: " + msgList.size());
            }).thenRun(() -> sendMessageRec(msgList, s));
        }
    }

    public static void main(String[] args) throws Exception {

        FutureServerSocketChannel ssc = new FutureServerSocketChannel();
        ssc.bind(new InetSocketAddress(3000));

        acceptConnectRec(ssc);
        System.out.println("Server listening on port 3000");

        // Impedir que o programa morra
        Thread.sleep(Integer.MAX_VALUE);
    }
}

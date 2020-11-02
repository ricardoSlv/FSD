import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.defaultThreadFactory;

//TODO: IMPEDIR QUE DUAS THREADS ESCREVAM NO MESMO SOCKET

public class Server {

    private static class ClientContext {

        ByteBuffer buf;
        AsynchronousSocketChannel sc;
        ArrayList<AsynchronousSocketChannel> connectedClientList;

        public ClientContext(ByteBuffer buf, AsynchronousSocketChannel sc,
                ArrayList<AsynchronousSocketChannel> connectedClientList) {
            this.buf = buf;
            this.sc = sc;
            this.connectedClientList = connectedClientList;
        }

    }

    private static class ServerContext {

        AsynchronousServerSocketChannel ssc;
        ArrayList<AsynchronousSocketChannel> connectedClientList;

        public ServerContext(AsynchronousServerSocketChannel ssc,
                ArrayList<AsynchronousSocketChannel> connectedClientList) {
            this.ssc = ssc;
            this.connectedClientList = connectedClientList;
        }

    }

    private static final CompletionHandler<AsynchronousSocketChannel, ServerContext> ach = new CompletionHandler<AsynchronousSocketChannel, ServerContext>() {
        @Override
        public void completed(AsynchronousSocketChannel sc, ServerContext serverContext) {
            System.out.println("Accepted!");

            ByteBuffer buf = ByteBuffer.allocate(1000);
            ClientContext c = new ClientContext(buf, sc, serverContext.connectedClientList);
            serverContext.connectedClientList.add(sc);

            readMsgRec(c);
            acceptRec(serverContext);
        }

        @Override
        public void failed(Throwable throwable, ServerContext serverContext) {
        }
    };

    public static void acceptRec(ServerContext serverContext) {
        serverContext.ssc.accept(serverContext, ach);
    };

    public static final CompletionHandler<Integer, ClientContext> writeHandler = new CompletionHandler<Integer, ClientContext>() {
        @Override
        public void completed(Integer integer, ClientContext c) {
            if (integer == 0) {
                try {
                    c.sc.close();
                    c.connectedClientList.remove(c.sc);
                } catch (Exception e) {
                    System.out.println("Socket close error");
                }
            } else {
                System.out.println("Feito! " + integer);
                c.buf.clear();
            }
        }

        @Override
        public void failed(Throwable throwable, ClientContext context) {
            System.out.println("Write failed");
        }
    };

    public static final CompletionHandler<Integer, ClientContext> readHandler = new CompletionHandler<Integer, ClientContext>() {
        @Override
        public void completed(Integer integer, ClientContext c) {

            if (integer == 0) {
                try {
                    c.sc.close();
                    c.connectedClientList.remove(c.sc);
                } catch (Exception e) {
                    System.out.println("Socket was closed i think");
                }
            } else {
                c.buf.flip();
                // todo TRY CATCH aqui para ver a exceçao de 2 writes simultaneos no mesmo socket
                try{
                for (AsynchronousSocketChannel socketC : c.connectedClientList) {
                    try {
                        socketC.write(c.buf.duplicate(), c, writeHandler);
                    } catch (Exception e) {
                        System.out.println("Double write error");
                    }
                }}catch(Exception e){
                    // Ocorrre quando o arraylist é modificado durante a iteração
                    // "It happens due to array list is modified after creation of Iterator."
                    System.out.println("Error iterating socket list");
                    e.printStackTrace();
                }
                readMsgRec(c);
            }
        }

        @Override
        public void failed(Throwable throwable, ClientContext c) {
            System.out.println("Read Failed");
            // System.out.println(throwable.getCause());
        }
    };

    public static void readMsgRec(ClientContext c) {
        try{
            c.sc.read(c.buf, c, readHandler);
        }catch(Exception e){
            System.out.println("Read couldnt start");
        }
    }

    public static void main(String[] args) throws Exception {

        AsynchronousChannelGroup g = AsynchronousChannelGroup.withFixedThreadPool(1, defaultThreadFactory());

        AsynchronousServerSocketChannel ssc = AsynchronousServerSocketChannel.open(g);
        ssc.bind(new InetSocketAddress(3000));

        ServerContext serverContext = new ServerContext(ssc, new ArrayList<AsynchronousSocketChannel>());
        acceptRec(serverContext);

        g.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        System.out.println("Terminei!");
    }
}

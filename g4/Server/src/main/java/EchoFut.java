import spullara.nio.channels.FutureServerSocketChannel;
import spullara.nio.channels.FutureSocketChannel;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

public class EchoFut {

    private static void readsocket(FutureSocketChannel s, ByteBuffer buf) {

        CompletableFuture<Integer> read = s.read(buf);
        read.thenAccept(i -> {
            buf.flip();
            CompletableFuture<Integer> write = s.write(buf.duplicate());
            write.thenAccept(nw -> {
                if (nw == 0)
                    s.close();

                buf.clear();
                System.out.println("Wrote" + nw);
            });
        }).thenRun(() -> readsocket(s, buf));

    }

    private static void acceptConnect(FutureServerSocketChannel ssc) {
        CompletableFuture<FutureSocketChannel> sc = ssc.accept();
        sc.thenAccept(s -> {
            ByteBuffer buf = ByteBuffer.allocate(1000);
            readsocket(s, buf);
            acceptConnect(ssc);
        });
    }

    public static void main(String[] args) throws Exception {
        FutureServerSocketChannel ssc = new FutureServerSocketChannel();
        ssc.bind(new InetSocketAddress(3000));

        acceptConnect(ssc);
        System.out.println("Server listening on port 3000");

        // Impedir que o programa morra
        Thread.sleep(Integer.MAX_VALUE);
    }
}

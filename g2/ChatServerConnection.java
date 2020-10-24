import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServerConnection extends Thread {
    private SocketChannel socket;
    private ReentrantLock lock;
    private Condition writerAlert;
    private Map<SocketChannel, ArrayList<ByteBuffer>> connections;

    public static class ChatServerConnectionReader extends Thread{
        private SocketChannel socket;
        private ReentrantLock lock;
        private Condition writerAlert;
        private Map<SocketChannel, ArrayList<ByteBuffer>> connections;

        public ChatServerConnectionReader(SocketChannel socketIn, ReentrantLock lockIn, Condition writerAlert,
                Map<SocketChannel, ArrayList<ByteBuffer>> connectionQueues) {
            this.socket = socketIn;
            this.lock = lockIn;
            this.writerAlert = writerAlert;
            this.connections = connectionQueues;
        }
         public void run(){
            try {
                System.out.println("Reader running");
                ByteBuffer buf = ByteBuffer.allocate(100);
                while ((this.socket.read(buf)) != -1) {
                    this.lock.lock();
                    buf.flip();
                    for (SocketChannel s : this.connections.keySet()) {
                        if (s != this.socket) {
                            this.connections.get(s).add(buf.duplicate());
                            // s.write(buf.duplicate());
                        }
                    }
                    buf.clear();
                    synchronized (writerAlert){
                        writerAlert.signalAll();
                        System.out.println("ACORDAAAAR");
                    }
                    this.lock.unlock();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println("Reader died");
                this.lock.lock();
                this.connections.remove(this.socket);
                this.lock.unlock();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
         } 
    }

    public static class ChatServerConnectionWriter extends Thread{
        private SocketChannel socket;
        private ReentrantLock lock;
        private Condition writerAlert;
        private Map<SocketChannel, ArrayList<ByteBuffer>> connections;

        public ChatServerConnectionWriter(SocketChannel socketIn, ReentrantLock lockIn, Condition writerAlert,
                Map<SocketChannel, ArrayList<ByteBuffer>> connectionQueues) {
            this.socket = socketIn;
            this.lock = lockIn;
            this.writerAlert = writerAlert;
            this.connections = connectionQueues;
        }
         public void run(){
            try {
                System.out.println("Writer running");
                while (this.connections.get(this.socket)!=null) {
                    System.out.println("Acordei");

                    ArrayList<ByteBuffer> localQueue = new ArrayList<>();
                    this.lock.lock();
                    for (ByteBuffer b : this.connections.get(this.socket)) {
                        localQueue.add(b);
                    }
                    this.connections.get(this.socket).clear();
                    this.lock.unlock();

                    for (ByteBuffer b : localQueue) {
                        this.socket.write(b.duplicate());
                    }
                    //TODO: CRIAR MANEIRA MELHOR DE ELIMINAR AS QUE JA LI
                    //Melhor maneira deve ser criar um repositorio global de mensagens, com limite de ex:500
                    //Ou ter um lock para cada queue
                    System.out.println("Vou dormir");
                    this.lock.lock();
                    writerAlert.await();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                System.out.println("Writer died");
                this.lock.lock();
                this.connections.remove(this.socket);
                this.lock.unlock();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
         }
        
    }

    public ChatServerConnection(SocketChannel socketIn, ReentrantLock lockIn, Condition writerAlert,
            Map<SocketChannel, ArrayList<ByteBuffer>> connectionQueues) {
        this.socket = socketIn;
        this.lock = lockIn;
        this.writerAlert = writerAlert;
        this.connections = connectionQueues;
    }

    public void run() {
        try {
            System.out.println("Connection running");
            Thread reader = new ChatServerConnectionReader(this.socket, this.lock, this.writerAlert, this.connections);
            Thread writer = new ChatServerConnectionWriter(this.socket, this.lock, this.writerAlert, this.connections);
            reader.start();
            writer.start();

            reader.join();
            writer.join();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("Connection died");
        }
    }
}

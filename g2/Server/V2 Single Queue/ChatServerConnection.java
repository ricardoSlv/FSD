import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServerConnection extends Thread {
    private final SocketChannel socket;
    private final ReentrantLock globalLock;
    private final Condition writerAlert;
    private final ArrayList<SocketChannel> connectedUsers;
    private final MsgBuffer msgBuffer;

    public class ChatServerConnectionReader extends Thread {
        private final SocketChannel socket;
        private final ReentrantLock globalLock;
        private final ArrayList<SocketChannel> connectedUsers;
        private final Condition writerAlert;
        private final MsgBuffer msgBuffer;

        public ChatServerConnectionReader(SocketChannel socketIn, ReentrantLock lockIn, Condition writerAlert,
                ArrayList<SocketChannel> connectedUsers, MsgBuffer msgBuffer) {
            this.socket = socketIn;
            this.globalLock = lockIn;
            this.connectedUsers = connectedUsers;
            this.writerAlert = writerAlert;
            this.msgBuffer = msgBuffer;
        }

        public void run() {
            try {
                System.out.println("Reader running");
                ByteBuffer buf = ByteBuffer.allocate(100);
                while ((this.socket.read(buf)) != -1) {
                    buf.flip();
                    this.msgBuffer.addMessage(buf.duplicate());
                    buf.clear();
                    System.out.println("Quero a merda do lock");
                    System.out.println(this.globalLock.isLocked());
                    System.out.println(this.globalLock.isHeldByCurrentThread());
                    this.globalLock.lock();
                    System.out.println("Ja tenho");
                    writerAlert.signalAll();
                    this.globalLock.unlock();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //System.out.println("Quero morrer");
                this.globalLock.lock();
                this.connectedUsers.remove(this.socket);
                writerAlert.signalAll();
                System.out.println(this.globalLock.isHeldByCurrentThread());
                System.out.println("Vou largar o lock");
                this.globalLock.unlock();
                System.out.println("Reader died");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println(this.globalLock.isHeldByCurrentThread());
            }
        }
    }

    public class ChatServerConnectionWriter extends Thread {
        private final SocketChannel socket;
        private final ReentrantLock globalLock;
        private final ArrayList<SocketChannel> connectedUsers;
        private final Condition writerAlert;
        private final MsgBuffer msgBuffer;
        private int lastMsgRead;

        public ChatServerConnectionWriter(SocketChannel socketIn, ReentrantLock lockIn, Condition writerAlert,
                ArrayList<SocketChannel> connectedUsers, MsgBuffer msgBuffer) {
            this.socket = socketIn;
            this.globalLock = lockIn;
            this.connectedUsers = connectedUsers;
            this.writerAlert = writerAlert;
            this.msgBuffer = msgBuffer;
        }

        public void run() {
            try {
                System.out.println("Writer running");
                while (this.connectedUsers.contains(this.socket)) {
                    // The thread can lock the lock 🔒 multiple times 🤯🤯🤯, and although await releases them all ,
                    // when signaled the thread reaquires them all back, this is making sure i lock the lock only once,
                    // like a 🔒 is supposed to be
                    if(this.globalLock.isHeldByCurrentThread()){
                        this.globalLock.unlock();
                    }

                    System.out.println("Acordei");
                    ByteBuffer b;
                    while (this.lastMsgRead < this.msgBuffer.getMsgCount()) {
                        // System.out.println("Reading");
                        b = this.msgBuffer.readMessage(this.lastMsgRead);
                        // System.out.println("Read");
                        // System.out.println(b.toString());
                        if (b != null)
                            this.socket.write(b.duplicate());
                        this.lastMsgRead++;
                    }
                    
                    this.globalLock.lock();
                    System.out.println("Consegui o lock");
                    writerAlert.await();
                }
                System.out.println("Vou embora");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //System.out.println("Quero morrer");
                System.out.println(this.globalLock.isHeldByCurrentThread());

                if(this.connectedUsers.contains(this.socket)){
                    this.connectedUsers.remove(this.socket);
                }
                System.out.println("Vou largar o lock");

                this.globalLock.unlock();

                System.out.println("Writer died");
                try {
                    System.out.println("closing");
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public ChatServerConnection(SocketChannel socketIn, ReentrantLock lockIn, Condition writerAlert,
            ArrayList<SocketChannel> connectedUsers, MsgBuffer msgBuffer) {
        this.socket = socketIn;
        this.globalLock = lockIn;
        this.connectedUsers = connectedUsers;
        this.writerAlert = writerAlert;
        this.msgBuffer = msgBuffer;
    }

    public void run() {
        try {
            System.out.println("Connection running");
            Thread reader = new ChatServerConnectionReader(this.socket, this.globalLock, this.writerAlert,
                    this.connectedUsers, this.msgBuffer);
            Thread writer = new ChatServerConnectionWriter(this.socket, this.globalLock, this.writerAlert,
                    this.connectedUsers, this.msgBuffer);
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

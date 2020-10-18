import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServerConnectionReader extends Thread {
    private Socket socket;
    private ReentrantLock lock;
    private ArrayList<Socket> connections;

    public ChatServerConnectionReader(Socket socketIn, ReentrantLock lockIn, ArrayList<Socket> connections) {
        this.socket = socketIn;
        this.lock = lockIn;
        this.connections = connections;
    }

    String message;

    public void run() {
        try {
            System.out.println("Reader running");
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            while ((message = socketIn.readLine()) != null) {
                this.lock.lock();
                for (Socket s : this.connections) {
                    if(s!=this.socket){
                        PrintWriter socketOut = new PrintWriter(s.getOutputStream());
                        socketOut.println(message);
                        socketOut.flush();
                    }
                }
                this.lock.unlock();
            }
            System.out.println("Reader died");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServerConnectionReader extends Thread {
    private Socket socket;
    private ReentrantLock lock;
    private PrintWriter socketPrinter;
    private BufferedReader inputStream;
    private ArrayList<PrintWriter> connections;

    public ChatServerConnectionReader(Socket socketIn, BufferedReader inputIn, ReentrantLock lockIn,
            PrintWriter socketPrinterIn, ArrayList<PrintWriter> connectionsIn) {
        this.socket = socketIn;
        this.lock = lockIn;
        this.socketPrinter = socketPrinterIn;
        this.inputStream = inputIn;
        this.connections = connectionsIn;
    }

    String message;

    public void run() {
        try {
            System.out.println("Reader running");
            while ((message = inputStream.readLine()) != null) {
                System.out.println(message);
                this.lock.lock();
                for (PrintWriter out : this.connections) {
                    System.out.println("sending");
                    out.println(message);
                }
                this.lock.unlock();
            }
            System.out.println("Reader died");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.lock.lock();
            this.connections.remove(this.socketPrinter);
            this.lock.unlock();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

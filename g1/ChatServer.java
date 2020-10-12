import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer extends Thread{
    private ServerSocket serverSocket;
    private ReentrantLock connectionLock = new ReentrantLock();
    private ArrayList<PrintWriter> connections;

    
    public ChatServer(int port) throws IOException {
       serverSocket = new ServerSocket(port);
       connections = new ArrayList<PrintWriter>();
    }
 
    public void run() {
       while(true) {
          try {
            System.out.println("Waiting for client on port " + 
            serverSocket.getLocalPort() + "...");
            Socket socket = serverSocket.accept();
            System.out.println("Just connected to " + socket.getRemoteSocketAddress());

            PrintWriter socketOut = new PrintWriter(socket.getOutputStream());
            BufferedReader socketIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            connectionLock.lock();
            connections.add(socketOut);
            System.out.println(connections.size());
            connectionLock.unlock();
            Thread c = new ChatServerConnectionReader(socket, socketIn , this.connectionLock, socketOut, this.connections);
            c.start();

          } catch (IOException e) {
            e.printStackTrace();
            break;
          }
       }
    }
    
    public static void main(String [] args) {
       int port = Integer.parseInt(args[0]);
       try {
         Thread t = new ChatServer(port);
         t.start();
       } catch (IOException e) {
         e.printStackTrace();
       }
    }
 }

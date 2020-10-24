import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer extends Thread{
    private ServerSocketChannel serverSocket;
    private ReentrantLock connectionLock = new ReentrantLock();
    private Map<SocketChannel,ArrayList<ByteBuffer>> connections;
    private Condition writerAlert;

    
    public ChatServer(int port) throws IOException {
      serverSocket=ServerSocketChannel.open();
      serverSocket.bind(new InetSocketAddress(port));
      connections = new HashMap<SocketChannel,ArrayList<ByteBuffer>>();
      writerAlert = this.connectionLock.newCondition();
    }
 
    public void run() {
       while(true) {
          try {
            System.out.println("Waiting for client on port " + serverSocket.getLocalAddress() + "...");
            SocketChannel socket = serverSocket.accept();
            System.out.println("Just connected to " + socket.getRemoteAddress());

            connectionLock.lock();
            connections.put(socket, new ArrayList<ByteBuffer>());
            System.out.println(connections.size());
            connectionLock.unlock();
            Thread c = new ChatServerConnection(socket, this.connectionLock,this.writerAlert , this.connections);
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

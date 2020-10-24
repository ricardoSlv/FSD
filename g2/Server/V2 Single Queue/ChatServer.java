import java.net.*;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.io.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ChatServer extends Thread{
    private ServerSocketChannel serverSocket;
    private ReentrantLock connectionLock = new ReentrantLock();
    private Condition writerAlert;
    private ArrayList<SocketChannel> connectedUsers;
    private MsgBuffer msgBuffer;

    
    public ChatServer(int port) throws IOException {
      serverSocket=ServerSocketChannel.open();
      serverSocket.bind(new InetSocketAddress(port));
      writerAlert = this.connectionLock.newCondition();
      connectedUsers = new ArrayList<>();
      msgBuffer = new MsgBuffer(200);
    }
 
    public void run() {
       while(true) {
          try {
            System.out.println("Waiting for client on port " + serverSocket.getLocalAddress() + "...");
            SocketChannel socket = serverSocket.accept();
            System.out.println("Just connected to " + socket.getRemoteAddress());

            connectionLock.lock();
            connectedUsers.add(socket);
            System.out.println(connectedUsers.size());
            connectionLock.unlock();

            Thread c = new ChatServerConnection(socket, this.connectionLock,this.writerAlert , this.connectedUsers, this.msgBuffer);
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

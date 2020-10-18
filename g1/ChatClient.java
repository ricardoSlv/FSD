import java.net.*;
import java.io.*;
import java.util.*;

public class ChatClient {

    public static class ChatClientOutput extends Thread {
        private Socket socket;
        private String name;

        public ChatClientOutput(Socket socket,String name) throws IOException {
            this.socket = socket;
            this.name = name;
        }

        public void run() {
            try {
                PrintWriter socketOut = new PrintWriter(this.socket.getOutputStream(), true);
                BufferedReader inputIn = new BufferedReader(new InputStreamReader(System.in));
                String inputLine;

                while ((inputLine = inputIn.readLine()) != null) {

                    if (inputLine.equals("quit"))
                        break;
                    else
                        socketOut.println(name+": "+inputLine);
                        socketOut.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ChatClientInput extends Thread {
        private Socket socket;

        public ChatClientInput(Socket socket) throws IOException {
            this.socket = socket;
        }

        public void run() {
            try {
                BufferedReader socketIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String inputLine;

                while ((inputLine = socketIn.readLine()) != null)
                    System.out.println(inputLine);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String name = args[1];
        try {
            Socket socket = new Socket("127.0.0.1", port);
            Thread tIn = new ChatClientInput(socket);
            Thread tOut = new ChatClientOutput(socket,name);
            tIn.start();
            tOut.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

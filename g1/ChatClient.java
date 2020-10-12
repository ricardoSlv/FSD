import java.net.*;
import java.io.*;
import java.util.*;

public class ChatClient {

    public static class ChatClientOutput extends Thread {
        private Socket socket;

        public ChatClientOutput(Socket socket) throws IOException {
            this.socket = socket;
        }

        public void run() {
            try {
                PrintWriter socketOut = new PrintWriter(this.socket.getOutputStream(), true);
                BufferedReader inputIn = new BufferedReader(new InputStreamReader(System.in));
                String inputLine, outputLine = "";

                socketOut.println(outputLine);

                while ((inputLine = inputIn.readLine()) != null) {

                    if (inputLine.equals("stop"))
                        break;
                    else
                        socketOut.println(inputLine);
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
                    System.out.println("got a message");
                    System.out.println(inputLine);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        try {
            Socket socket = new Socket("127.0.0.1", port);
            Thread tIn = new ChatClientInput(socket);
            Thread tOut = new ChatClientOutput(socket);
            tIn.start();
            tOut.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

import java.net.*;
import java.io.*;

public class ChatBot {

    public static class ChatBotOutput extends Thread {
        private Socket socket;
        private String name;
        private int interval;
        private int counter;

        public ChatBotOutput(Socket socket, String name, int intervalSend) throws IOException {
            this.socket = socket;
            this.name = name;
            this.interval = intervalSend;
            this.counter = 0;
        }

        public void run() {
            try {
                PrintWriter socketOut = new PrintWriter(this.socket.getOutputStream(), true);

                while (true) {
                    socketOut.println(name + ": " + this.counter + " Sheep");
                    socketOut.flush();
                    this.counter++;
                    sleep(interval);
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ChatBotInput extends Thread {
        private Socket socket;
        private int interval;

        public ChatBotInput(Socket socket, int intervalRead) throws IOException {
            this.socket = socket;
            this.interval = intervalRead;
        }

        public void run() {
            try {
                BufferedReader socketIn = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                String inputLine;

                while ((inputLine = socketIn.readLine()) != null){
                    System.out.println(inputLine);
                    sleep(this.interval);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        int port = Integer.parseInt(args[0]);
        String name = args[1];
        int intervalSend = Integer.parseInt(args[2]);
        int intervalRead = Integer.parseInt(args[3]); 

        try {
            Socket socket = new Socket("127.0.0.1", port);
            Thread tIn = new ChatBotInput(socket,intervalRead);
            Thread tOut = new ChatBotOutput(socket,name,intervalSend);
            tIn.start();
            tOut.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


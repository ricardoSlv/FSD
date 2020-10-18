import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class SimpleSocket extends Socket {
    private PrintWriter lineWriter;
    private BufferedReader lineReader;

    SimpleSocket(String host, int port) throws UnknownHostException, IOException {
        super(host,port);
        this.lineReader = new BufferedReader(new InputStreamReader(this.getInputStream()));
        this.lineWriter = new PrintWriter(this.getOutputStream());
    }

    void sendLine(String line){
        this.lineWriter.print(line);
        this.lineWriter.flush();
    }

    String readLine() throws IOException {
        String line = this.lineReader.readLine();
        return line;
    }


}

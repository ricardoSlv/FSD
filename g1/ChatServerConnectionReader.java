import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;

public class ChatServerConnectionReader extends Thread {
    private BufferedReader inputStream;
    private ArrayList<String> messageList;

    

    public ChatServerConnectionReader(BufferedReader input){
        this.inputStream=input;
    }

    public void run(){
        try{
            while(inputStream.readLine()!=null){

            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}

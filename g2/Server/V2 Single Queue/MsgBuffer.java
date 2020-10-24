import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class MsgBuffer {
    private final int msgCapacity;
    private int lastAvailableMsg;
    private int msgCount;
    private Map<Integer,ByteBuffer> msgBuffer;

    public MsgBuffer(int msgLimit){
        this.msgCapacity= msgLimit;
        this.lastAvailableMsg=0;
        this.msgCount=0;
        this.msgBuffer = new HashMap<>();
    }

    public int getLastAvailableMsg(){
        return this.lastAvailableMsg;
    }

    public int getMsgCount(){
        return this.msgCount;
    }

    public synchronized void addMessage(ByteBuffer msg){
        this.msgBuffer.put(this.msgCount, msg);
        this.msgCount++;
        if((this.msgCount-this.lastAvailableMsg)>this.msgCapacity){
            this.msgBuffer.remove(this.lastAvailableMsg);
            this.lastAvailableMsg++;
        }
    }

    public ByteBuffer readMessage(int msgNumber){
        return this.msgBuffer.get(msgNumber);
    }
}

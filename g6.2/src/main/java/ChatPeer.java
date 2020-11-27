import java.nio.charset.StandardCharsets;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class ChatPeer {

    private static Boolean isUpdated(int peerIndex, ArrayList<Integer> peerVector, ArrayList<Integer> myVector) {
        
        System.out.println(peerIndex+" "+myVector.toString()+" "+peerVector.toString());
        
        for (int i = 0; i < myVector.size(); i++) {

            if (i == peerIndex && ((myVector.get(i) + 1) != peerVector.get(i)))
                return false;
            if (i != peerIndex && (myVector.get(i) < peerVector.get(i)))
                return false;
        }
        return true;
    }

    private static void processMessage(int peerIndex, ArrayList<Integer> peerVector, ArrayList<Integer> myVector,
            String msg, ArrayList<msgInfo> msgList) {
        

        if (isUpdated(peerIndex, peerVector, myVector)) {
            System.out.println(msg);
            myVector.set(peerIndex, myVector.get(peerIndex) + 1);
            for (msgInfo msgI : msgList) {
                if (isUpdated(msgI.peerIndex, msgI.vector, myVector)) {
                    msgList.remove(msgI);
                    processMessage(msgI.peerIndex, msgI.vector, myVector, msgI.msg, msgList);
                }
            }
        }
        else{
            System.out.println("Was not updated");
            msgList.add(new msgInfo(peerIndex, peerVector, msg));
        }
    }

    public static void main(String[] args) {

        // 0||1||2
        int id = Integer.parseInt(System.console().readLine());

        //3001,3002,3003
        ArrayList<String> peers = new ArrayList<>(Arrays.asList("0", "1", "2"));

        ArrayList<Integer> myVector = new ArrayList<>(Arrays.asList(0, 0, 0));

        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        NettyMessagingService ms = new NettyMessagingService("chatpeer", Address.from(Integer.parseInt("300" + id)),
                new MessagingConfig());

        ArrayList<msgInfo> msgList = new ArrayList<>();

        try {
            Thread.sleep(100, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ms.registerHandler("chatpeer", (address, msg) -> {
            // "[3,4,1,5]__Ola joao!"
            String decodedMsg = new String(msg, StandardCharsets.UTF_8);

            String[] messagefields = decodedMsg.split("__");

            ArrayList<Integer> peerVector = new ArrayList<>(Arrays.asList(
                messagefields[0]
                .replaceAll("(\\]|\\ |\\[)", "")
                .split(","))
                .stream()
                .map(s -> Integer.parseInt(s))
                .collect(Collectors.toList()));

            // "hostname:3000"
            int peerIndex = Character.getNumericValue(address.toString().split(":")[1].charAt(3));

            System.out.println(peerIndex);
            if(peerIndex==id)
                System.out.println(messagefields[1]);
            else
                processMessage(peerIndex, peerVector, myVector, messagefields[1], msgList);

            // System.out.println(decodedMsg);
        }, es);

        ms.start();

        while (true) {
            String msg = System.console().readLine();
            myVector.set(id, myVector.get(id)+1);
            for (String peer : peers) {
                ms.sendAsync(Address.from(Integer.parseInt("300" + peer)), "chatpeer", (myVector.toString()+"__"+msg).getBytes())
                        .exceptionally(t -> {
                            t.printStackTrace();
                            return null;
                        });
            }
        }
    }
}

class msgInfo {
    public int peerIndex;
    public ArrayList<Integer> vector;
    public String msg;

    msgInfo(int peerIndex, ArrayList<Integer> vector, String msg) {
        this.peerIndex = peerIndex;
        this.vector = vector;
        this.msg = msg;
    }
}
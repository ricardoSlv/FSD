//import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;

public class ChatPeer {


    public static void main(String[] args)  {
        
        String port = System.console().readLine();
        String peer1 = System.console().readLine();
        String peer2 = System.console().readLine();
        String[] peers = {port,peer1,peer2};

        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        NettyMessagingService ms = new NettyMessagingService("chatpeer", Address.from(Integer.parseInt(port)),new MessagingConfig()); 
        
        try {
            Thread.sleep(100, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ms.registerHandler("chatpeer", (adress, msg) -> {
            String decodedMsg = new String(msg, StandardCharsets.UTF_8);
            System.out.println(decodedMsg);
        }, es);

        ms.start();

        while (true) {
            String msg = System.console().readLine();
            for(String peer : peers){
                ms.sendAsync(
                    Address.from("localhost", Integer.parseInt(peer)), 
                    "chatpeer", 
                    msg.getBytes()
                )
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
            }
        }
    }
}



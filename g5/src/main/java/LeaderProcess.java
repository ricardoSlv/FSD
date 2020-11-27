import java.math.BigInteger;
import java.util.ArrayList;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LeaderProcess extends Thread {

    private int port;
    private int totalProcesses;
    private int minPort;
    private int leader = 0;
    private ArrayList<Integer> peersAtented = new ArrayList<>();

    public LeaderProcess(int port, int minPort, int totalProcesses) {
        super();
        this.port = port;
        this.minPort = minPort;
        this.totalProcesses = totalProcesses;
    }

    @Override
    public void run() {
        ScheduledExecutorService es = Executors.newScheduledThreadPool(1);
        NettyMessagingService ms = new NettyMessagingService("leader", Address.from(this.port), new MessagingConfig());
        
        try {
            Thread.sleep(100, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        

        ms.registerHandler("leader", (adress, msg) -> {
            int peerPort = new BigInteger(msg).intValue();

            if (this.peersAtented.contains(peerPort) == false) {
                this.peersAtented.add(peerPort);
                this.leader = peerPort > this.leader ? peerPort : this.leader;
            }
        }, es);

        ms.start();

        es.schedule(() -> {
            System.out.println("Process" + this.port + " says the leader is " + this.leader);
        }, 1, TimeUnit.SECONDS);

        for (int i = 0; i < this.totalProcesses; i++) {
            if (this.minPort + i != this.port) {
                ms.sendAsync(
                    Address.from("localhost", this.minPort + i), 
                    "leader", 
                    BigInteger.valueOf(this.port).toByteArray()
                    )
                .thenRun(() -> {
                    System.out.println("Mensagem enviada!");
                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
            }
        }
    }
}

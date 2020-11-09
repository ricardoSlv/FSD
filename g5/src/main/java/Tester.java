import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Tester {
    public static void main(String[] args) throws Exception {
        int minPort=10000;
        for(int i=0;i<10;i++){
            LeaderProcess p = new LeaderProcess(minPort+i, minPort, 10);
            p.start();
        }
    }
}

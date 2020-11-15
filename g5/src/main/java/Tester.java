// import io.atomix.cluster.messaging.MessagingConfig;
// import io.atomix.cluster.messaging.impl.NettyMessagingService;
// import io.atomix.utils.net.Address;

// import java.util.concurrent.Executors;
// import java.util.concurrent.ScheduledExecutorService;
// import java.util.concurrent.TimeUnit;

public class Tester {
    public static void main(String[] args) throws Exception {
        int minPortSync=10000;
        int minPortAsync=11000;
        for(int i=0;i<10;i++){
            LeaderProcess p = new LeaderProcess(minPortSync+i, minPortSync, 10);
            p.start();
        }
        for(int i=0;i<10;i++){
            LeaderProcessAsync p = new LeaderProcessAsync(minPortAsync+i, minPortAsync, 10);
            p.start();
        }
    }
}

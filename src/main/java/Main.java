import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.ACL;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Created by Anyang on 2019/1/20.
 */
public class Main implements Watcher {

    private static CountDownLatch connectedSemaphore = new CountDownLatch(1);

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        String serviceName = "test";

        //https://www.jianshu.com/p/f684ef537ede

        ZooKeeper zooKeeper = new ZooKeeper("112.74.62.29:2181", 1000, new Main());
        System.out.println(zooKeeper.getState());
        connectedSemaphore.await();

        List<String> result = zooKeeper.getChildren("/dubbo/com.fangdd.finance.org.auth.api.service.OrgApiService", false);
        System.out.println(result);

        createIfNotExist(zooKeeper, "/" + serviceName, "this is a test service node".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        //create provider
        createIfNotExist(zooKeeper, "/" + serviceName + "/" + "provider", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        //create consumer
        createIfNotExist(zooKeeper, "/" + serviceName + "/" + "consumer", null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

        String providerName = "test-provider";
        //creare provider
        createIfNotExist(zooKeeper, "/" + serviceName + "/" + "consumer" + "/" + providerName, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);


        zooKeeper.getChildren("/" + serviceName + "/" + "consumer" + "/" + providerName, new Watcher() {
            public void process(WatchedEvent event) {
                System.out.println(event.getState());
            }
        });

        zooKeeper.setData("/" + serviceName + "/" + "consumer" + "/" + providerName, "changeData".getBytes(), 0);

        Thread.sleep(20000);
    }

    public static void createIfNotExist(ZooKeeper zooKeeper, String path, byte[] data, List<ACL> acl, CreateMode createMode) throws KeeperException, InterruptedException {
        if (zooKeeper.exists(path, false) == null) {
            zooKeeper.create(path, data, acl, createMode);
        }
    }

    public void process(WatchedEvent watchedEvent) {
        System.out.println("process");
        if (Event.KeeperState.SyncConnected == watchedEvent.getState()) {
            connectedSemaphore.countDown();
        }
    }
}

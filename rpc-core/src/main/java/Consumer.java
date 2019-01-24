import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Anyang on 2019/1/20.
 */
@Slf4j
public class Consumer {

    public static final String root = "zubbo";

    public static void main(String[] args) throws Exception {

        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString("112.74.62.29:2181")
                .sessionTimeoutMs(5000)
                .connectionTimeoutMs(3000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .namespace(root)
                .build();
        client.start();


        CreateBuilder createBuilder = client.create();

        String serviceName = "com.anyang.service";

        //build service node
        createServiceNode(client, createBuilder, serviceName);

        String ip = "127.0.0.1?consumer";
        //subscribe to service
//        createBuilder = client.create();
        createBuilder.withMode(CreateMode.EPHEMERAL);
        createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "consumers" + "/" + ip, null);


        /**
         * 在注册监听器的时候，如果传入此参数，当事件触发时，逻辑由线程池处理
         */
        ExecutorService pool = Executors.newFixedThreadPool(2);

        /**
         * 监听子节点的变化情况
         */
        final PathChildrenCache childrenCache = new PathChildrenCache(client, "/" + serviceName + "/" + "providers", true);
        childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        childrenCache.getListenable().addListener(
                new PathChildrenCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
                            throws Exception {
                        switch (event.getType()) {
                            case CHILD_ADDED:
                                System.out.println("CHILD_ADDED: " + event.getData().getPath());
                                break;
                            case CHILD_REMOVED:
                                System.out.println("CHILD_REMOVED: " + event.getData().getPath());
                                break;
                            case CHILD_UPDATED:
                                System.out.println("CHILD_UPDATED: " + event.getData().getPath());
                                break;
                            default:
                                break;
                        }
                    }
                },
                pool
        );

//
//        client.setData().forPath("/zk-huey/cnode", "world".getBytes());
//
//        client.create().withMode(CreateMode.EPHEMERAL).forPath("/zk-huey/cnode1", "word1".getBytes());

//        Thread.sleep(10 * 1000);
//        pool.shutdown();
        log.info("tmp");
        Thread.sleep(1000000);
        client.close();
    }


    public static void createServiceNode(CuratorFramework client, CreateBuilder createBuilder, String serviceName) throws Exception {
        createBuilder.withMode(CreateMode.PERSISTENT);
//        createBuilder.creatingParentsIfNeeded().forPath("/"+"provider"+"/"+serviceName, null);
        String consumersPath = "/" + serviceName + "/" + "consumers";
        String providersPath = "/" + serviceName + "/" + "providers";

        //check if exist
        if (client.checkExists().forPath(consumersPath) == null) {
            createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "consumers", null);
        }

        if (client.checkExists().forPath(providersPath) == null) {
            createBuilder.creatingParentsIfNeeded().forPath("/" + serviceName + "/" + "providers", null);
        }
    }
}

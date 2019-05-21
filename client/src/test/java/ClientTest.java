import com.anyang.CountService;
import com.anyang.exception.RpcTimeOutException;
import com.anyang.manage.ZubboApplication;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.*;

public class ClientTest {


    //client 压力测试
    @Test
    public void clientBeanchTest() throws Exception {

        ZubboApplication application = new ZubboApplication("112.74.62.29:2181", "localhost:3002");
        long startTine = System.currentTimeMillis();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 20, 100, TimeUnit.MILLISECONDS, new LinkedBlockingDeque<>());

        int repeatCount = 100;
        CountDownLatch latch = new CountDownLatch(repeatCount);
        for (int i = 0; i < repeatCount; i++) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    CountService countService = null;
                    try {
                        countService = application.subscribe(CountService.class);
                        int result = countService.count();
                        System.out.println(result);
                        latch.countDown();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        latch.await();
        System.out.println("total time : " + (System.currentTimeMillis() - startTine));

        Thread.sleep(10000);
    }

    @Test
    public void test2() {
        String[] paths = "/com.anyang.CountService/consumers/localhost:3002".split("/");
        System.out.println(Arrays.toString(paths));

        InetSocketAddress socketAddress = new InetSocketAddress("localhost", 3000);
        System.out.println(socketAddress.getAddress());
        System.out.println(socketAddress.getHostName());
        System.out.println(socketAddress.getPort());
    }
}

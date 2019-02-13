import com.anyang.CountService;
import com.anyang.manage.ZubboApplication;
import com.anyang.registry.ConnectionManager;
import org.junit.Test;

import java.util.Arrays;

public class ClientTest {


    @Test
    public void test() throws Exception {
        ZubboApplication application = new ZubboApplication("112.74.62.29:2181", "localhost:3002");
        application.startWatch();

        long startTine = System.currentTimeMillis();

        for (int i = 0; i < 200; i++) {
            CountService countService = application.subscribe(CountService.class);
            int result = countService.count();
            System.out.println(result);
        }

        System.out.println("total time : " + (System.currentTimeMillis() - startTine));

        Thread.sleep(10000);
    }

    @Test
    public void test2() {
        String[] paths = "/com.anyang.CountService/consumers/localhost:3002".split("/");
        System.out.println(Arrays.toString(paths));
    }
}

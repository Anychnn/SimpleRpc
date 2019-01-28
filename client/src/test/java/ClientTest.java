import com.anyang.CountService;
import com.anyang.manage.ZubboApplication;
import org.junit.Test;

public class ClientTest {


    @Test
    public void test() {
        ZubboApplication application = new ZubboApplication("112.74.62.29:2181");
        CountService countService = application.subscribe(CountService.class);
        countService.count();
    }
}

import com.anyang.CountService;
import com.anyang.manage.ZubboApplication;

public class Main {
    public static void main(String[] args) {
        ZubboApplication application = new ZubboApplication("112.74.62.29:2181");
        CountService countProxy = application.subscribe(CountService.class);
        int count = countProxy.count();
        System.out.println(count);
    }
}

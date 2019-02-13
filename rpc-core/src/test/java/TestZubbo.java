import com.anyang.manage.ZubboApplication;
import com.anyang.protocal.RpcRequest;
import com.anyang.registry.ConnectionManager;
import com.anyang.util.SerializationUtil;
import org.junit.Test;

public class TestZubbo {
    @Test
    public void test() throws Exception {
        String zookeeper = "112.74.62.29:2181";
        String serverAddress = "localhost:4040";
        ZubboApplication application = new ZubboApplication(zookeeper, serverAddress);
        ConnectionManager.getInstance().initServer();
    }

    @Test
    public void testSerialize() throws InstantiationException, IllegalAccessException {
        RpcRequest request = new RpcRequest();
        request.setMethodName("count");
        request.setParameters(new Object[]{1, 2, 3});
        byte[] data = SerializationUtil.serialize(request);
        System.out.println(data.length);
        System.out.println(SerializationUtil.deSerialize(data,RpcRequest.class));
    }
}

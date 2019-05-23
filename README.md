# SimpleRpc

![项目结构](https://github.com/Anychnn/SimpleRpc/blob/master/img/SimpleRpc%E9%A1%B9%E7%9B%AE%E7%BB%93%E6%9E%84.jpg)

### project description : 因为公司用的dubbo比较多,花了几天时间写了一个RPC框架,单纯的小项目吧

- RPC框架
- 注册中心,Zookeper
- 序列化,protolbuf
- 通信,netty,几乎是通信的标配了
- 反射,默认使用jdk反射 jdk1.8情况下 反射和cglib字节码技术 性能差别不大
- spring 的支持
- netty解决了大量CLOSE_WAIT的socket问题
- 设置心跳时间,断线开始尝试重连

todo
- 负载均衡策略,隐式的根据延迟
- rpc调用延迟
- 调用链采集
- 使用延迟作为启发式（算法的因子）来均衡跨集群主机的负载,（隐式地）派发给具有最低负载、最低延迟的主机
- 一个单一的基于Netty 的服务便能够处理将近1 百万的入站TCP 套接字连接
- SSL
- ByteBuf

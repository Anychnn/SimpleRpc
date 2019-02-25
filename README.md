# SimpleRpc

![项目结构](https://github.com/Anychnn/SimpleRpc/blob/master/img/SimpleRpc%E9%A1%B9%E7%9B%AE%E7%BB%93%E6%9E%84.jpg)

### project description : 因为公司用的dubbo比较多,花了几天时间写了一个RPC框架,单纯的小项目吧

- RPC框架
- 注册中心,Zookeper
- 序列化,protolbuf
- 通信,netty,几乎是通信的标配了
- 反射,默认使用jdk反射 jdk1.8情况下 反射和cglib字节码技术 性能差别不大
- spring 的支持
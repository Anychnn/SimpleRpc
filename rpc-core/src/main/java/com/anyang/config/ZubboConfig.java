package com.anyang.config;

import com.anyang.invoke.InvokerEnum;
import lombok.Data;

@Data
public class ZubboConfig {
    public static final String root = "zubbo";
    public static String serverAddress;
    public static String zookeeperAddress;
    public static InvokerEnum invokerEnum = InvokerEnum.JDKInvoker; //默认使用jdk反射 jdk1.8情况下 反射和cglib字节码技术 性能差别不大
}

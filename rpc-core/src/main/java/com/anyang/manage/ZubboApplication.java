package com.anyang.manage;

public class ZubboApplication {
    private String address;

    /**
     * subscribe service from zookeeper
     * @param clazz the type of service
     * @return
     */
    public Object subscribe(Class clazz){
        return new Object();
    }

    public void register(Object service){

    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

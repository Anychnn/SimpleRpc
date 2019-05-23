package com.anyang.config;

public enum OutTypeEnum {
    RRC_RESPONSE(1, "RPC返回"), HEART_BEAT(2, "心跳");

    private int type;
    private String desc;

    OutTypeEnum(int type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }
}

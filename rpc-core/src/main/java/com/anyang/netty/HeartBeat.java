package com.anyang.netty;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
public class HeartBeat implements Serializable {
    private String id;

    public HeartBeat() {
        this.id = UUID.randomUUID().toString();
    }
}

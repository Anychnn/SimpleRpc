package com.anyang.netty;

import lombok.Data;

import java.io.Serializable;

@Data
public class HeartBeat implements Serializable {
    private long id;
}

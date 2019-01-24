package com.anyang;

import com.anyang.CountService;
import com.anyang.annotation.RpcService;

import java.util.Random;

@RpcService(CountService.class)
public class CountServiceImpl implements CountService {
    @Override
    public int count() {
        return new Random().nextInt();
    }
}

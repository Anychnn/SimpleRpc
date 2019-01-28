package com.anyang.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Created by Anyang on 2019/1/21.
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RpcService {
}

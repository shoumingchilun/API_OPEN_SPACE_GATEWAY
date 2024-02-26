package com.chilun.apiopenspace.gateway;

/**
 * @author 齿轮
 * @date 2024-02-26-16:07
 */
public interface RedisKeyPrefix {
    String ACCESS_PREFIX = "InterfaceAccess_";
    String PREVENT_REPLAY = "preventReplay";
}

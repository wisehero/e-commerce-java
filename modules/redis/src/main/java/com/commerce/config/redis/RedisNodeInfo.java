package com.commerce.config.redis;

public record RedisNodeInfo(
        String host,
        int port
) { }

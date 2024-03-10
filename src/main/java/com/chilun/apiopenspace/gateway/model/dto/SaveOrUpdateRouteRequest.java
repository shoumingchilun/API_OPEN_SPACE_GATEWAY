package com.chilun.apiopenspace.gateway.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author 齿轮
 * @date 2024-02-13-19:55
 */
@Data
public class SaveOrUpdateRouteRequest {
    @NotNull
    String id;
    @NotNull
    String uri;

    Integer replenishRate;
    Integer burstCapacity;
    Integer requestedTokens;
}

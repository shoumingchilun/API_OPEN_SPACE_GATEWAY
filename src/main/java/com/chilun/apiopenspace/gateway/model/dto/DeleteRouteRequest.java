package com.chilun.apiopenspace.gateway.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author 齿轮
 * @date 2024-02-13-20:01
 */
@Data
public class DeleteRouteRequest {
    @NotNull
    String id;
}

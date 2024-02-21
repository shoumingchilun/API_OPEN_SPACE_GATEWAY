package com.chilun.apiopenspace.gateway.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author 齿轮
 * @date 2024-02-13-20:00
 */
@Data
public class InitRouteRequest {
    @NotNull
    List<SaveOrUpdateRouteRequest> list;
}

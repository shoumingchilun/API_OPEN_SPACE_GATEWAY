package com.chilun.apiopenspace.gateway.entity.pojo;

import lombok.Data;

import java.util.List;

/**
 *
 * Route实体类，用于远程调用接受参数
 * @author 齿轮
 * @date 2024-02-13-19:53
 */
@Data
public class RoutePOJO {
    String id;
    String URI;
    List<String> Predicates;
    List<String> Filters;
}

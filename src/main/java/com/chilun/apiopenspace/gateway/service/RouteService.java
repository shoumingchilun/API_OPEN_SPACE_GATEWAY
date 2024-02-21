package com.chilun.apiopenspace.gateway.service;

import com.chilun.apiopenspace.gateway.model.dto.DeleteRouteRequest;
import com.chilun.apiopenspace.gateway.model.dto.InitRouteRequest;
import com.chilun.apiopenspace.gateway.model.dto.SaveOrUpdateRouteRequest;
import com.chilun.apiopenspace.gateway.model.pojo.RoutePOJO;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author 齿轮
 * @date 2024-02-08-22:39
 */
public interface RouteService {
    void init(InitRouteRequest request);
    void saveOrUpdate(SaveOrUpdateRouteRequest request);
    void delete(DeleteRouteRequest request);
    void refresh();
    Mono<List<RoutePOJO>> getAll();
}

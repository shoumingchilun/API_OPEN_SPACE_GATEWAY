package com.chilun.apiopenspace.gateway.service.impl;

import com.chilun.apiopenspace.gateway.model.dto.DeleteRouteRequest;
import com.chilun.apiopenspace.gateway.model.dto.InitRouteRequest;
import com.chilun.apiopenspace.gateway.model.dto.SaveOrUpdateRouteRequest;
import com.chilun.apiopenspace.gateway.model.pojo.RoutePOJO;
import com.chilun.apiopenspace.gateway.service.RouteService;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 齿轮
 * @date 2024-02-08-22:43
 */
@Service
public class RouteServiceAdepter implements RouteService, ApplicationEventPublisherAware {

    @Resource
    private RouteDefinitionWriter routeDefinitionWriter;
    @Resource
    private RouteDefinitionLocator locator;

    @Value("${myGateway.RoutePrefix}")
    private String routePrefix;

    @Override
    public void init(InitRouteRequest request) {
        request.getList().forEach(this::saveOrUpdate);
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @Override
    public void saveOrUpdate(SaveOrUpdateRouteRequest request) {
        //定义路由
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(request.getId());
        URI uri = URI.create(request.getUri());
        routeDefinition.setUri(uri);
        String Path = "/" + routePrefix + "/" + request.getId();
        routeDefinition.setPredicates(Collections.singletonList(new PredicateDefinition("Path=" + Path)));

        //定义过滤器
        //1RPS过滤器
        FilterDefinition rateFilterDefinition = null;
        if (ObjectUtils.allNotNull(request.getReplenishRate(), request.getBurstCapacity(), request.getRequestedTokens())) {
            rateFilterDefinition = new FilterDefinition();
            rateFilterDefinition.setName("RequestRateLimiter");
            Map<String, String> argsMap = new HashMap<>();
            argsMap.put("redis-rate-limiter.replenishRate", request.getReplenishRate().toString());
            argsMap.put("redis-rate-limiter.burstCapacity", request.getBurstCapacity().toString());
            argsMap.put("redis-rate-limiter.requestedTokens", request.getRequestedTokens().toString());
            argsMap.put("key-resolver", "#{@accessKeyResolver}");
            rateFilterDefinition.setArgs(argsMap);
        }
        //2路径转换过滤器
        //将请求转发到指定URI（包含Path），同时去除请求到达网关时原始的path
        FilterDefinition pathTransferFilterDefinition = new FilterDefinition("SetPath=/" + (uri.getPath().length() <= 1 ? "" : uri.getPath().substring(1)));

        //添加过滤器
        ArrayList<FilterDefinition> filterDefinitions = new ArrayList<>();
        filterDefinitions.add(pathTransferFilterDefinition);
        if (rateFilterDefinition != null) {
            filterDefinitions.add(rateFilterDefinition);
        }
        routeDefinition.setFilters(filterDefinitions);

        //保存路由
        routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
    }

    @Override
    public void delete(DeleteRouteRequest request) {
        routeDefinitionWriter.delete(Mono.just(request.getId())).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @Override
    public void refresh() {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @Override
    public Mono<List<RoutePOJO>> getAll() {
        return locator.getRouteDefinitions()
                .map(routeDefinition -> {
                    RoutePOJO routePOJO = new RoutePOJO();
                    routePOJO.setId(routeDefinition.getId());
                    routePOJO.setURI(routeDefinition.getUri().toString());
                    routePOJO.setPredicates(routeDefinition.getPredicates().stream()
                            .map(predicate -> predicate.getArgs().values().toString())
                            .collect(Collectors.toList()));
                    routePOJO.setFilters(routeDefinition.getFilters().stream()
                            .map(FilterDefinition::getName)
                            .collect(Collectors.toList()));
                    return routePOJO;
                })
                .collectList();
    }

    private ApplicationEventPublisher publisher;

    @Override
    public void setApplicationEventPublisher(@NotNull ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}

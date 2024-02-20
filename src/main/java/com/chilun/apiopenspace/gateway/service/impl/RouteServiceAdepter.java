package com.chilun.apiopenspace.gateway.service.impl;

import com.chilun.apiopenspace.gateway.entity.dto.DeleteRouteRequest;
import com.chilun.apiopenspace.gateway.entity.dto.InitRouteRequest;
import com.chilun.apiopenspace.gateway.entity.dto.SaveOrUpdateRouteRequest;
import com.chilun.apiopenspace.gateway.entity.pojo.RoutePOJO;
import com.chilun.apiopenspace.gateway.service.RouteService;
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

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

//    @PostConstruct
//    public void test(){
//        SaveOrUpdateRouteRequest saveOrUpdateRouteRequest = new SaveOrUpdateRouteRequest();
//        saveOrUpdateRouteRequest.setUri("https://www.baidu.com");
//        saveOrUpdateRouteRequest.setId("1");
//        saveOrUpdate(saveOrUpdateRouteRequest);
//    }

    @Override
    public void init(InitRouteRequest request) {
        request.getList().forEach(this::saveOrUpdate);
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @Override
    public void saveOrUpdate(SaveOrUpdateRouteRequest request) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(request.getId());
        URI uri = URI.create(request.getUri());
        routeDefinition.setUri(uri);
        String Path = "/" + routePrefix + "/" + request.getId();
        routeDefinition.setPredicates(Collections.singletonList(new PredicateDefinition("Path=" + Path)));
        routeDefinition.setFilters(Arrays.asList(
                //将请求转发到指定URI（包含Path），同时去除请求到达网关时原始的path
                new FilterDefinition("SetPath=/" + (uri.getPath().length() <= 1 ? "" : uri.getPath().substring(1))),
                //去除请求头中的敏感信息（网关验证信息）
                new FilterDefinition("RemoveRequestHeader=ChilunAPISpace-sendTimestamp"),
                new FilterDefinition("RemoveRequestHeader=ChilunAPISpace-expireTimestamp"),
                new FilterDefinition("RemoveRequestHeader=ChilunAPISpace-accesskey"),
                new FilterDefinition("RemoveRequestHeader=ChilunAPISpace-salt"),
                new FilterDefinition("RemoveRequestHeader=ChilunAPISpace-sign")));
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
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}

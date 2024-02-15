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
        routeDefinition.setUri(URI.create(request.getUri()));
        String Path = "/" + routePrefix + "/" + request.getId();
        routeDefinition.setPredicates(Collections.singletonList(new PredicateDefinition("Path=" + Path)));
        routeDefinition.setFilters(Collections.singletonList(new FilterDefinition("SetPath=/")));
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

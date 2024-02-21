package com.chilun.apiopenspace.gateway.controller;

import com.chilun.apiopenspace.gateway.model.dto.DeleteRouteRequest;
import com.chilun.apiopenspace.gateway.model.dto.InitRouteRequest;
import com.chilun.apiopenspace.gateway.model.dto.SaveOrUpdateRouteRequest;
import com.chilun.apiopenspace.gateway.model.pojo.RoutePOJO;
import com.chilun.apiopenspace.gateway.service.RouteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @author 齿轮
 * @date 2024-02-13-22:13
 */
@Slf4j
@RestController
@RequestMapping("/manage")
public class RouteController {
    @Resource
    RouteService service;

    @PostMapping("/update")
    public boolean add(@RequestBody @Valid SaveOrUpdateRouteRequest request) {
        try {
            log.debug("保存或更新路由：id={}, uri={}", request.getId(), request.getUri());
            service.saveOrUpdate(request);
        } catch (Exception e) {
            log.error("保存或更新路由时出现错误：", e);
            return false;
        }
        return true;
    }

    @PostMapping("/init")
    public boolean init(@RequestBody @Valid InitRouteRequest request) {
        try {
            log.debug("进行路由初始化：{}", request);
            service.init(request);
        } catch (Exception e) {
            log.error("初始化路由时出现错误：", e);
            return false;
        }
        return true;
    }

    @PostMapping("/delete")
    public boolean delete(@RequestBody @Valid DeleteRouteRequest request) {
        try {
            log.debug("删除路由：id={}", request.getId());
            service.delete(request);
        } catch (Exception e) {
            log.error("删除指定路由" + request.getId() + "时出现错误：", e);
            return false;
        }
        return true;
    }

    @GetMapping("/getAll")
    public Mono<List<RoutePOJO>> getAll() {
        return service.getAll();
    }

    @GetMapping("/refresh")
    public boolean refresh() {
        try {
            log.debug("更新路由");
            service.refresh();
        } catch (Exception e) {
            log.error("更新路由时出现错误：", e);
            return false;
        }
        return true;
    }
}

package com.chilun.apiopenspace.gateway.controller;

import com.chilun.apiopenspace.gateway.entity.dto.DeleteRouteRequest;
import com.chilun.apiopenspace.gateway.entity.dto.InitRouteRequest;
import com.chilun.apiopenspace.gateway.entity.dto.SaveOrUpdateRouteRequest;
import com.chilun.apiopenspace.gateway.entity.pojo.RoutePOJO;
import com.chilun.apiopenspace.gateway.service.RouteService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author 齿轮
 * @date 2024-02-13-22:13
 */
@RestController
@RequestMapping("/manage")
public class RouteController {
    @Resource
    RouteService service;

    @PostMapping("/update")
    public boolean add(@RequestBody SaveOrUpdateRouteRequest request) {
        try {
            service.saveOrUpdate(request);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @PostMapping("/init")
    public boolean init(@RequestBody InitRouteRequest request) {
        try {
            service.init(request);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @PostMapping("/delete")
    public boolean delete(@RequestBody DeleteRouteRequest request) {
        try {
            service.delete(request);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @GetMapping("/getAll")
    public Mono<List<RoutePOJO>> getAll() {
        return service.getAll();
    }

    @GetMapping("/refresh")
    public boolean refresh(){
        try {
            service.refresh();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}

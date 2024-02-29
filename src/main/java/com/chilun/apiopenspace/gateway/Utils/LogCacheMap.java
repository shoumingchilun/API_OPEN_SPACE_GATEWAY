package com.chilun.apiopenspace.gateway.Utils;


import java.util.HashMap;

/**
 * @author 齿轮
 * @date 2024-02-29-18:52
 */

public class LogCacheMap {
    public static HashMap<String, String> LogMap = new HashMap<>();

    public static void saveRequest(String uuid, String request) {
        LogMap.put(uuid + "request", request);
    }

    public static String getRequest(String uuid) {
        return LogMap.remove(uuid + "request");
    }

    public static void saveResponse(String uuid, String response) {
        LogMap.put(uuid + "response", response);
    }

    public static String getResponse(String uuid) {
        return LogMap.remove(uuid + "response");
    }
}

package com.chenxiaofei.disruptorflow.support.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

//TODO:目前假设用户id存储到Headers中，透传后端//
public class UserContext {

    /**
     * 从请求头中获取userId
     * @return
     */
    public static String getUserId() {
        ServletRequestAttributes attr =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attr == null) {
            return null;
        }

        HttpServletRequest request = attr.getRequest();

        return request.getHeader("user_id");
    }
}

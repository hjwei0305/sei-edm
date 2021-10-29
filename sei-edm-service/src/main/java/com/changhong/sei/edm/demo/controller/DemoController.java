package com.changhong.sei.edm.demo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@RefreshScope
@Controller
@Api(value = "示例演示", tags = "示例演示")
public class DemoController {
    //网关上下文地址
    @Value("${sei.gateway.context-path:/api-gateway}")
    private String gatewayPath;

    /**
     * 演示页
     */
    @ApiOperation("演示")
    @GetMapping(value = "/demo")
    public String uploadPage(Model model, HttpServletRequest request) {
        String contextPath = request.getContextPath();
        if (StringUtils.isNotBlank(contextPath) && !StringUtils.startsWith(contextPath, "/")) {
            contextPath = "/" + contextPath;
        }
        model.addAttribute("baseUrl", gatewayPath.concat(contextPath));
        return "demo.html";
    }
}

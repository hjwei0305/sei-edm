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
    //引入配置
    @Value("${sei.edm.base-url:none}")
    private String baseUrl;

    /**
     * 演示页
     */
    @ApiOperation("演示")
    @GetMapping(value = "/demo")
    public String uploadPage(Model model, HttpServletRequest request) {
        StringBuffer url = request.getRequestURL();
        String tempContextUrl = url.delete(url.length() - request.getRequestURI().length(), url.length()).append(request.getContextPath()).toString();
        if (StringUtils.equals("none", baseUrl)) {
            baseUrl = tempContextUrl;
        }
        model.addAttribute("baseUrl", baseUrl);
        return "demo.html";
    }
}

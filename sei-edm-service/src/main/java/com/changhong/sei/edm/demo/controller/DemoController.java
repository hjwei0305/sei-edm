package com.changhong.sei.edm.demo.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Api(value = "示例演示", tags = "示例演示")
public class DemoController {

    /**
     * 演示页
     */
    @ApiOperation("演示")
    @GetMapping(value = "/demo")
    public String uploadPage() {
        return "demo.html";
    }
}

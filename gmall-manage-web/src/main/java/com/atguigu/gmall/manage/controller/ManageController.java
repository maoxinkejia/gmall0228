package com.atguigu.gmall.manage.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ManageController {


    //去往首页
    @RequestMapping("index")
    public String index() {
        return "index";
    }


}

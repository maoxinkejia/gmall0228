package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SpuManageController {

    @Reference
    private ManageService manageService;

    //去往spu属性管理页面
    @RequestMapping("/spuListPage")
    public String spuListPage() {
        return "spuListPage";
    }

    @ResponseBody
    @RequestMapping("/spuList")
    public List<SpuInfo> spuList(String catalog3Id) {
        return manageService.spuList(catalog3Id);
    }

    @ResponseBody
    @RequestMapping("/baseSaleAttrList")
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return manageService.getBaseSaleAttrList();
    }

    @ResponseBody
    @RequestMapping(value = "/saveSpuInfo", method = RequestMethod.POST)
    public void saveSpuInfo(SpuInfo spuInfo) {
        manageService.saveSpuInfo(spuInfo);
    }
}

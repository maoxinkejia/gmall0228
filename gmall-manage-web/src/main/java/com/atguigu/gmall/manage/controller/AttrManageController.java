package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class AttrManageController {

    @Reference
    private ManageService manageService;

    //去往分级属性查询页面
    @RequestMapping("attrListPage")
    public String attrListPage() {
        return "attrListPage";
    }

    @ResponseBody
    @RequestMapping(value = "/getCatalog1", method = RequestMethod.POST)
    public List<BaseCatalog1> getCatalog1() {
        return manageService.getCatalog1();
    }

    @ResponseBody
    @RequestMapping(value = "/getCatalog2", method = RequestMethod.POST)
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        return manageService.getCatalog2(catalog1Id);
    }

    @ResponseBody
    @RequestMapping(value = "/getCatalog3", method = RequestMethod.POST)
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        return manageService.getCatalog3(catalog2Id);
    }

    @ResponseBody
    @RequestMapping("/attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id) {
        return manageService.attrInfoList(catalog3Id);
    }

    @RequestMapping(value = "saveAttrInfo", method = RequestMethod.POST)
    @ResponseBody
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        manageService.saveAttrInfo(baseAttrInfo);
    }

}

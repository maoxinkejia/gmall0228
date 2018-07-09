package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SpuImage;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @ResponseBody
    @RequestMapping("/skuAttrInfoList")
    public List<BaseAttrInfo> getSkuAttrInfoList(String catalog3Id) {
        return manageService.getSkuAttrInfoList(catalog3Id);
    }

    @ResponseBody
    @RequestMapping("/spuSaleAttrList")
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return manageService.getSpuSaleAttrList(spuId);
    }

    @ResponseBody
    @RequestMapping("/spuImageList")
    public List<SpuImage> getSpuImageList(String spuId) {
        return manageService.getSpuImageList(spuId);
    }

    @RequestMapping(value = "/saveSku", method = RequestMethod.POST)
    @ResponseBody
    public String saveSku(SkuInfo skuInfo) {
        manageService.saveSku(skuInfo);
        return "success";
    }

}

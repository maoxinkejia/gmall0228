package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

    @RequestMapping("/{skuId}.html")
    //自定义的注解类，true表示访问该控制器时需要走这个注解（需要通过过滤器）（需要登录才能访问该控制器）
    @LoginRequire(autoRedirect = true)
    public String skuInfoPage(@PathVariable("skuId") String skuId, Model model) {

        //根据skuId查询对应的商品信息
        SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
        //将查询到的商品信息放到域中
        model.addAttribute("skuInfo", skuInfo);

        //查询显示销售属性，属性值
        List<SpuSaleAttr> spuSaleAttrList = manageService.selectSpuSaleAttrListCheckBySku(skuInfo);
        //将查询到的销售属性集合放到域中
        model.addAttribute("spuSaleAttrList", spuSaleAttrList);

        //调用方法将sku销售属性值集合查询到，并且拼装成一个字符串
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());
        //声明一个准备拼接用的字符串
        String valueKeys = "";
        //定义一个需要封装字符串数据的map
        Map<String, String> map = new HashMap<>();
        //循环遍历集合拿到具体值
        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            //根据下标拿到集合里面具体的值
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
            //判断什么时候需要加"|"
            if (valueKeys.length() > 0) {
                //当字符串前面已经有值的时候，需要添加
                valueKeys += "|";
            }
            //当字符串为空时说明是第一次拼串，直接进行拼串即可
            valueKeys += skuSaleAttrValue.getSaleAttrValueId();
            //判断什么时候结束拼串
            //当最后一次遍历完，等于这个集合长度的时候结束拼串，或者是
            //当前的sku销售属性值的skuId和集合中下一索引的sku销售属性值的skuId不一致时，说明这不是一个商品
            if ((i + 1) == skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i + 1).getSkuId())) {
                //往map里面存放数据
                map.put(valueKeys, skuSaleAttrValue.getSkuId());
                //每当存放完数据之后就清空当前的字符串，重新拼接
                valueKeys = "";
            }
        }
        //将map转化成json字符串
        String skuValueJson = JSON.toJSONString(map);
        System.out.println(skuValueJson);
        //把字符串放到页面中
        model.addAttribute("skuValueJson", skuValueJson);

        //调用服务，使这个商品每被访问一次就自增分数一次
        listService.incrHotScore(skuId);
        return "item";
    }
}

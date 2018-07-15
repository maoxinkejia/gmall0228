package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@Controller
public class AttrManageController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

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

    @RequestMapping("/getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueList(String attrId) {
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        return baseAttrInfo.getAttrValueList();
    }

    //根据skuInfo的id查询skuInfo对象
    @RequestMapping("/onSale")
    @ResponseBody
    public void onSale(String skuId) {
        //通过商品id查询商品对象
        //由于es中需要保存的不仅是skuInfo对象，还有里面的属性值集合，这个使用这个方法是查不到的
        //所以需要在实现类里面手动查询，将查询到的销售属性值封装进skuInfo对象中
        SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
        //创建一个保存到es中的商品信息对象
        SkuLsInfo skuLsInfo = new SkuLsInfo();

        try {
            //调用工具类，进行转换
            BeanUtils.copyProperties(skuLsInfo, skuInfo);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        //将赋值好的skuLSInfo对象存放到es中
        listService.saveSkuLsInfo(skuLsInfo);
    }
}

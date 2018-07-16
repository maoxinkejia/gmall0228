package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseAttrValue;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;

    @RequestMapping(value = "list.html", method = RequestMethod.GET)
    public String getSkuLsResult(SkuLsParams skuLsParams, Model model) {
        //根据网页传来的参数到es中查询获取结果
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        System.out.println(JSON.toJSONString(skuLsResult));

        //将获取到的数据存放到域中，给前台使用
        model.addAttribute("skuLsInfoList", skuLsResult.getSkuLsInfoList());

        //根据从es中查询到的结果类中获取平台属性值id集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //根据平台属性值id集合查询平台属性对象集合
        List<BaseAttrInfo> attrList = manageService.getAttrInfo(attrValueIdList);
        //将查询到的结果放到页面中
        model.addAttribute("attrList", attrList);

        //根据查询结果参数对象制作一个url地址，当选择平台属性时，会把之前选择的参数（后面拼接的URL地址）
        //与新选择的平台属性拼接成一个完整的url地址，同来收集完整参数，用来筛选新的商品
        String makeUrl = makeUrlParam(skuLsParams);

        //创建一个集合，用来添加被选中属性值的对象
        List<BaseAttrValue> baseAttrValueList = new ArrayList<BaseAttrValue>();

        //因为拼接的url地址是用来访问的路径，但是在前台页面地址也需要拼接属性值id（valueId）
        //所以需要去重，遍历属性集合
        //因为要在遍历中对集合进行删除操作，所以不能使用foreach循环，需要使用迭代器循环遍历
        for (Iterator<BaseAttrInfo> iterator = attrList.iterator(); iterator.hasNext(); ) {
            //通过迭代，拿到每一个属性信息对象
            BaseAttrInfo baseAttrInfo = iterator.next();

            //拿到里面的valueId集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            //循环遍历iter
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //判断非空
                if (baseAttrValue.getId() != null && baseAttrValue.getId().length() > 0) {
                    //判断非空
                    if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
                        //遍历从页面中拿到的结果集合，拿到每个valueId进行比较，去重
                        for (String valueId : skuLsParams.getValueId()) {
                            //进行判断，如过重复则去重
                            if (valueId.equals(baseAttrValue.getId())) {
                                //使用迭代器进行删除
                                iterator.remove();

                                //制作面包屑位置的属性和属性值的集合
                                //创建一个被选中的属性值的对象
                                BaseAttrValue baseAttrValueSelected = new BaseAttrValue();
                                //给被选中的属性值附一个新的名字
                                baseAttrValueSelected.setValueName(baseAttrInfo.getAttrName() + ":" + baseAttrValue.getValueName());
                                //重新调用制作url的方法，将被选中的valueId传进去，进行去重操作
                                String urlParam = makeUrlParam(skuLsParams, valueId);
                                //将新的url地址放到被选中的属性值对象中
                                baseAttrValueSelected.setUrlParam(urlParam);
                                //创建一个集合，用来添加被选中的属性值的对象
                                baseAttrValueList.add(baseAttrValueSelected);
                            }
                        }
                    }
                }
            }
        }


        //将被选中的属性值的集合放到页面中
        model.addAttribute("baseAttrValuesList", baseAttrValueList);
        //将全文检索里面输入的关键字设置到页面中
        model.addAttribute("keyword", skuLsParams.getKeyword());

        //将拼接好的url地址放到页面中
        model.addAttribute("urlParam", makeUrl);

        return "list";
    }

    /**
     * 用来拼接一个url地址的方法
     *
     * @param skuLsParams 由页面收集到的参数，存放了三级id，平台属性，属性值数据
     * @param paramIds    用于添加面包屑时，进行判断，如果属性值id是一样的就去掉，不拼接
     */
    private String makeUrlParam(SkuLsParams skuLsParams, String... paramIds) {
        //创建一个字符串准备拼接
        String makeUrl = "";
        //判断是否有keyword
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            //如果有keyword则直接拼接
            makeUrl += "keyword=" + skuLsParams.getKeyword();
        }

        //判断是否有三级分类id
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            //有三级分类id还需要判断makeUrl是否有值，如果有需要拼接一个&符号
            if (makeUrl.length() > 0) {
                makeUrl += "&";
            }
            //拼接三级分类id
            makeUrl += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }

        //判断是否有属性值id的集合
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {

            //遍历这个数组
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                //拿到具体的每一个属性值id
                String valueId = skuLsParams.getValueId()[i];

                //将传进来的可变参数的值与valueId进行比较，如果一样则不拼接
                if (paramIds != null && paramIds.length > 0) {
                    //获取每一个可变参数的id值
                    String paramId = paramIds[0];
                    //进行比较
                    if (paramId.equals(valueId)) {
                        //如果一样则不进行拼接（后面的代码不走，结束本次循环，开始下一次循环）
                        continue;
                    }
                }

                if (makeUrl.length() > 0) {
                    makeUrl += "&";
                }
                //拼接属性值id
                makeUrl += "valueId=" + valueId;
            }
        }
        return makeUrl;
    }

}

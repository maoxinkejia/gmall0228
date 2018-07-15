package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {

    //根据spuId查询sku销售属性值集合，用于拼接json字符串
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}

package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    //根据spuId查询spu销售属性集合
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    //根据skuId和spuId查询spu销售属性集合
    List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(String id, String spuId);
}

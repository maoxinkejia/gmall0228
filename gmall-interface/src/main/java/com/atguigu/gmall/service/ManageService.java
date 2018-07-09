package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {

    //获取所有一级分类集合
    List<BaseCatalog1> getCatalog1();

    //获取所有二级分类集合
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    //获取所有三级分类集合
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    //根据三级id获取属性信息集合（单表）
    List<BaseAttrInfo> attrInfoList(String catalog3Id);

    //保存属性和属性值信息
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    //根据属性id查询属性属性对象信息
    BaseAttrInfo getAttrInfo(String attrId);

    //根据三级id查询spu信息集合
    List<SpuInfo> spuList(String catalog3Id);

    //查询所有销售属性集合
    List<BaseSaleAttr> getBaseSaleAttrList();

    //大保存，保存spu编辑列表里面的所有数据
    void saveSpuInfo(SpuInfo spuInfo);

    //根据三级id查询所有属性信息集合（多表）
    List<BaseAttrInfo> getSkuAttrInfoList(String catalog3Id);

    //根据spuId查询spu销售属性集合
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    //根据spuId查询spu图片集合
    List<SpuImage> getSpuImageList(String spuId);

    //保存skuInfo
    void saveSku(SkuInfo skuInfo);
}

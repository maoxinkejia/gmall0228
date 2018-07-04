package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;

import java.util.List;

public interface ManageService {

    //获取所有一级分类集合
    List<BaseCatalog1> getCatalog1();

    //获取所有二级分类集合
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    //获取所有三级分类集合
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    //获取属性信息集合
    List<BaseAttrInfo> attrInfoList(String catalog3Id);

    //保存属性和属性值信息
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);
}

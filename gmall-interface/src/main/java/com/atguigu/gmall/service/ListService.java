package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    //在es中保存skuInfo
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    //根据参数查询，获得查询结果
    public SkuLsResult search(SkuLsParams skuLsParams);
}

package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {

    //在es中保存skuInfo
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    //根据参数查询，获得查询结果
    public SkuLsResult search(SkuLsParams skuLsParams);

    //自增分数，商品每被查看一次，就调用一次这个方法，刷评分
    void incrHotScore(String skuId);
}

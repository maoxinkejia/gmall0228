package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface OrderService {

    //保存订单
    String saveOrder(OrderInfo orderInfo);

    //根据用户id生成流水订单号，用来校验表单是否重复提交
    String getTradeNo(String userId);

    //防止重复登录，删除redis中缓存的key
    void delTradeNo(String userId);

    //根据用户id和页面获取到的订单流水号，到redis中查询是否存在当前key
    boolean checkTradeCode(String tradeNo, String userId);

    //根据商品id和数量验库存
    boolean checkStock(String skuId, Integer skuNum);

    //根据订单id查询订单信息
    OrderInfo getOrderInfo(String orderId);

    //根据订单id更新订单状态
    void updateOrderStatus(String orderId, ProcessStatus PAID);

    //发送订单状态
    void sendOrderStatus(String orderId);

    //根据订单id和库存的map进行拆单
    List<OrderInfo> orderSplit(String orderId, String wareSkuMap) throws InvocationTargetException, IllegalAccessException;
}

package com.atguigu.gmall.service;

import com.alipay.api.AlipayApiException;
import com.atguigu.gmall.bean.PaymentInfo;

import java.util.List;

public interface PaymentService {

    // 保存信息
    void savePaymentInfo(PaymentInfo paymentInfo);

    //支付对象中有商品订单流水号，根据唯一的流水号查询支付信息对象
    PaymentInfo getpaymentInfo(PaymentInfo paymentInfo);

    //支付成功后，更改支付状态
    void updatePaymentInfo(PaymentInfo paymentInfoUpd, String outTradeNo);

    //更新支付信息，给消息队列发送消息
    void sendPaymentResult(PaymentInfo paymentInfo, String result);

    //检查支付结果
    boolean checkPayment(PaymentInfo paymentInfoQuery) throws AlipayApiException;

    // 设置延迟队列！
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount);

    // 关闭交易记录信息
    void closePayment(String id);
}

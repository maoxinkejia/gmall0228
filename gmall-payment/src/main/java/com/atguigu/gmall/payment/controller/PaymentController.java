package com.atguigu.gmall.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.bean.enums.PaymentStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PaymentController {

    @Reference
    private OrderService orderService;
    @Reference
    private PaymentService paymentService;
    @Autowired
    private AlipayClient alipayClient;


    @RequestMapping("index")
    public String index(HttpServletRequest request, Model model) {
        // 取得订单id
        String orderId = request.getParameter("orderId");
        // 保存订单orderId到页面
        model.addAttribute("orderId", orderId);
        // 根据orderId 进行查询
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        //orderInfo.sumTotalAmount();
        model.addAttribute("totalAmount", orderInfo.getTotalAmount());
        return "index";
    }

    @RequestMapping(value = "alipay/submit", method = RequestMethod.POST)
    @ResponseBody
    public String submitPayment(HttpServletRequest request, HttpServletResponse response) {
        //获取请求参数
        String orderId = request.getParameter("orderId");
        //根据订单id查询订单对象，支付对象的所有信息应该都是根据订单对象得来的
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject(orderInfo.getTradeBody());
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setCreateTime(new Date());

        // 保存
        paymentService.savePaymentInfo(paymentInfo);

        // 生成二维码，根据alipay的API文档调用接口
        //创建API对应的request
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        //设置同步回调函数和异步回调函数
        //在公共参数中设置回跳和通知地址
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url);

        // 声明一个Map
        Map<String, Object> bizContnetMap = new HashMap<>();
        bizContnetMap.put("out_trade_no", paymentInfo.getOutTradeNo());
        bizContnetMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        bizContnetMap.put("subject", paymentInfo.getSubject());
        bizContnetMap.put("total_amount", paymentInfo.getTotalAmount());
        // 将map变成json
        String Json = JSON.toJSONString(bizContnetMap);
        alipayRequest.setBizContent(Json);

        String form = "";

        try {
            form = alipayClient.pageExecute(alipayRequest).getBody(); //调用SDK生成表单
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType("text/html;charset=UTF-8");

        return form;
    }

    //同步回调：商家跳转到购物结算
    @RequestMapping(value = "/alipay/callback/return", method = RequestMethod.GET)
    public String callbackReturn() {
        return "redirect:" + AlipayConfig.return_order_url;
    }

    /**
     * 支付宝用来异步回调的控制器
     *
     * @param paramMap 一个由alipay拼接成的一个超长的参数
     */
    @RequestMapping(value = "/alipay/callback/notify", method = RequestMethod.POST)
    @ResponseBody
    public String paymentNotify(@RequestParam(value = "paramMap") Map<String, String> paramMap) throws AlipayApiException {
        // 取得sign 公钥信息
        //sign=ZVcFYePO0DY0MysHEcRSQ0QsDm2StHupyuMpF7qS8ukKL4lUHRydnkG%2Bf%2BCGFwN1v9USmlIH0v6NauPY4aHQxhk1ED9WBHtWMR%2FlJryR1s7CfIKOll5hxz5BNxXlPeC%2F38OENpCtD2HxXbGpw%2FaN4%2BbouhuL9bskXabM27UNP5ruxXXzRNfy9VlNx1aDT5huBaNI%2BIYKmrtiqXNsdXKFhRsKWshxgJOOiz01Wikr2hgLAn1SQNUL9hKOxWjc1IB32E%2FckC1uBtNofAF0zes9Z81oW8X1usGKfUFU0gQ6TCYZ7bUaMg4qi63b5YAhcWdmwPrP%2BknIQd3GjQxBbra40A%3D%3D
        boolean signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, "utf-8", AlipayConfig.sign_type);
        if (signVerified) {
            //根据回调参数，拿到支付宝的交易状态，判断当前交易状态属于哪一个
            // 当付款完成 TRADE_SUCCESS，TRADE_FINISHED！
            String trade_status = paramMap.get("trade_status");
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)) {
                // 如果当前订单已经付款！则该次支付，失败！paymengInfo 对象去查询一下当前的状态！
                // paymentInfo.paymentStatus
                String out_trade_no = paramMap.get("out_trade_no");
                PaymentInfo paymentInfo = new PaymentInfo();
                // 根据out_trade_no 找到paymentInfo 对象 -- 能够拿到payment_status
                paymentInfo.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfoHas = paymentService.getpaymentInfo(paymentInfo);

                if (paymentInfoHas.getPaymentStatus() == PaymentStatus.ClOSED || paymentInfoHas.getPaymentStatus() == PaymentStatus.PAID) {
                    return "fail";
                } else {
                    PaymentInfo paymentInfoUpd = new PaymentInfo();
                    // 修改订单-支付状态，
                    paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                    // 修改时间
                    paymentInfoUpd.setCreateTime(new Date());
                    paymentInfoUpd.setOutTradeNo(out_trade_no);
                    // 更新数据状态
                    paymentService.updatePaymentInfo(paymentInfoUpd, out_trade_no);
                    // 发送消息给订单。修改订单状态。 url:payment.gmall.com/sendPaymentResult?orderId=53&result=success 60.
                    paymentService.sendPaymentResult(paymentInfoUpd, "success");
                    return "success";
                }
            } else {
                return "fail";
            }
        } else {
            return "fail";
        }
    }

    // 测试支付宝返回结果payment.gmall.com/sendPaymentResult?orderId=53&result=success
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public void sendPaymentResult(PaymentInfo paymentInfo, @RequestParam(value = "result") String result) {
        paymentService.sendPaymentResult(paymentInfo, result);
    }

    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(HttpServletRequest request) throws AlipayApiException {
        // 先获取订单编号
        String orderId = request.getParameter("orderId");
        // 准备创建PaymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        // 根据传递过来的orderId查询PaymentInfo ,该paymentInfo 中肯定会out_trade_no! select * from paymentInfo where orderId = 53 ; out_trade_no
        PaymentInfo info = paymentService.getpaymentInfo(paymentInfo);
        // 调用service服务
        boolean flag = paymentService.checkPayment(info);
        return "result=" + flag;
    }

}

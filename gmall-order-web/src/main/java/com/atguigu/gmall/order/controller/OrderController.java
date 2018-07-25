package com.atguigu.gmall.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.enums.OrderStatus;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.service.UserAddressService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class OrderController {

    // 调用service 层 服务 @Autowired 不要了！
    @Reference
    private UserAddressService userAddressService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @RequestMapping("trade")
    @LoginRequire
    public String tradeInit(HttpServletRequest request, Model model) {
        // 获取的userId
        String userId = (String) request.getAttribute("userId");
        // 得到购物车列表 ,从redis中勾选的key中取得数据
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        // 收货人的地址
        List<UserAddress> userAddressList = userAddressService.getUserAddressList(userId);
        model.addAttribute("addressList", userAddressList);

        // 订单详情的数据是从cartInfo 对象中的来的！
        List<OrderDetail> orderDetailList = new ArrayList<OrderDetail>();
        for (CartInfo cartInfo : cartInfoList) {
            // 创建对象
            OrderDetail orderDetail = new OrderDetail();
            //一一赋值
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            orderDetail.setOrderPrice(cartInfo.getCartPrice());
            orderDetail.setImgUrl(cartInfo.getImgUrl());

            //添加到集合中
            orderDetailList.add(orderDetail);

        }

        model.addAttribute("orderDetailList", orderDetailList);
        // 数据展示 , OrderInfo ,OrderDetail.// 保存信息，给前台显示！
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        //调用内部方法计算所有被选中商品的总价
        orderInfo.sumTotalAmount();
        model.addAttribute("totalAmount", orderInfo.getTotalAmount());

        // 在进入提交订单页面时就应该生成这个这个流水号，保存流水号，给前台
        String tradeNo = orderService.getTradeNo(userId);
        model.addAttribute("tradeCode", tradeNo);

        // 数据库插入。点击提交订单的时候，插入数据库！
        return "trade";
    }

    //提交订单，将购物车对象转换成对单对象
    @RequestMapping(value = "submitOrder", method = RequestMethod.POST)
    @LoginRequire
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo, Model model) {
        //拿到用户id
        String userId = (String) request.getAttribute("userId");

        //首先获取页面中的订单流水号
        String tradeNo = request.getParameter("tradeNo");

        //避免重复提交
        //根据流水号和用户id去redis中查询，看是否存在这个值
        boolean flag = orderService.checkTradeCode(tradeNo, userId);
        if (!flag) {
            //如果没有查询到则跳转到错误页面，给出错误提示消息
            model.addAttribute("errMsg", "提交订单失败，请联系管理员！");
            return "tradeFail";
        }

        //验库存
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //拿到具体的每一个订单详情，根据商品id和商品数量作为参数进行检查
        for (OrderDetail orderDetail : orderDetailList) {
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result) {
                //没有库存时，给出错误提示信息，并跳转错误页面
                model.addAttribute("errMsg", "库存不足，请重新下单！");
                return "tradeFail";
            }
        }

        // orderInfo 表中有一些固定的字段值需要设置，不是通过表单提交上来的
        //订单状态，未支付（枚举类常量）
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //流程状态，未支付
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);
        //保存用户id
        orderInfo.setUserId(userId);
        // 调用计算总价的方法，设置总价
        orderInfo.sumTotalAmount();
        orderInfo.setTotalAmount(orderInfo.getTotalAmount());

        // 调用service 中的方法，保存收集好的订单对象，同时，将订单编号返回
        String orderId = orderService.saveOrder(orderInfo);

        // 验证完毕后，删除redis 中的tradeNo，订单再次提交时就会失败
        orderService.delTradeNo(userId);

        //订单数据收集完后重定向到支付的页面，带上订单编号
        return "redirect://payment.gmall.com/index?orderId=" + orderId;
    }


}

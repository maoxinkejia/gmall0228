package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
public class CartController {

    @Autowired
    private CartCookieHandler cartCookieHandler;
    @Reference
    private CartService cartService;
    @Reference
    private ManageService manageService;

    //添加购物车
    @RequestMapping(value = "addToCart", method = RequestMethod.POST)
    //不需要登录，但是需要走拦截器
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response, Model model) {
        //从页面获取参数
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("skuNum");
        String userId = (String) request.getAttribute("userId");

        //判断用户是否登录，如果登录了就走数据库，如果没有登录就走cookie
        if (userId != null) {
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));
        } else {
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        }

        //添加完购物车后要将添加的商品信息对象放到前台页面显示出来（添加购物车成功页面）
        SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
        model.addAttribute("skuInfo", skuInfo);
        model.addAttribute("skuNum", skuNum);

        return "success";
    }

    //查询购物车列表
    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public String cartList(HttpServletRequest request, HttpServletResponse response, Model model) {
        // 判断是否登录
        String userId = (String) request.getAttribute("userId");
        // 取得cookie中cartInfo集合数据
        List<CartInfo> cartListFromCookie = cartCookieHandler.getCartList(request);
        //定义一个空的购物车集合用来合并购物车
        List<CartInfo> cartList = null;
        if (userId != null) {
            //已经登录，且cookie中的购物车对象有值时
            if (cartListFromCookie != null && cartListFromCookie.size() > 0) {
                // 合并购物车，cookie-->db。 根据skuId 相同的就合并，合并完之后，返回一个集合
                cartList = cartService.mergeToCartList(cartListFromCookie, userId);
                // cookie删除掉。
                cartCookieHandler.deleteCartCookie(request, response);
            } else {
                //如果cookie里面没有数据就直接将redis中查询的结果返回
                cartList = cartService.getCartList(userId);
            }
            // 将集合保存给前台使用
            model.addAttribute("cartList", cartList);
        } else {
            // 没有登录，cookie中取得
            List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);
            model.addAttribute("cartList", cookieHandlerCartList);
        }
        return "cartList";
    }


    //改变购物车物品被勾选状态
    @RequestMapping(value = "checkCart", method = RequestMethod.POST)
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request, HttpServletResponse response) {
        // 取得userId判断是否登录
        String userId = (String) request.getAttribute("userId");
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");
        if (userId != null) {
            // 已登录 ,从redis中将数据取出来并修改被选中状态
            cartService.checkCart(userId, skuId, isChecked);
        } else {
            // 未登录，在cookie中修改购物车物品被选中状态
            cartCookieHandler.checkCart(request, response, skuId, isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire(autoRedirect = true)
    public String toTrade(HttpServletRequest request, HttpServletResponse response) {
        // 取得userId
        String userId = (String) request.getAttribute("userId");
        // 结算的时候：cookie+db
        //先将cookie中的购物车集合拿到
        List<CartInfo> cookieHandlerCartList = cartCookieHandler.getCartList(request);

        // 循环遍历cookie中的值，跟db进行合并
        if (cookieHandlerCartList != null && cookieHandlerCartList.size() > 0) {
            // 准备合并
            cartService.mergeToCartList(cookieHandlerCartList, userId);
            // 将cookie中的数据删除！
            cartCookieHandler.deleteCartCookie(request, response);
        }
        return "redirect://order.gmall.com/trade";
    }
}

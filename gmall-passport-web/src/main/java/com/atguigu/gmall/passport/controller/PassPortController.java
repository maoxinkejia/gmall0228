package com.atguigu.gmall.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.passport.util.JwtUtil;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassPortController {
    //从配置文件中拿到对应key的值，key：（token.key），生成token用的唯一的key
    @Value("${token.key}")
    private String signKey;

    @Reference
    private UserInfoService userInfoService;

    @RequestMapping("/index")
    public String index(HttpServletRequest request) {
        String originUrl = request.getParameter("originUrl");
        request.setAttribute("originUrl", originUrl);

        return "index";
    }

    @ResponseBody
    @RequestMapping(value = "login", method = RequestMethod.POST)
    public String login(HttpServletRequest request, UserInfo userInfo) {
        // 自动获取 通过nginx 的方向代理得到！location /{ proxy_pass  http://item.gmal.com }
        //根据请求头，自动获取ip地址
        String ip = request.getHeader("X-forwarded-for");

        if (userInfo != null) {
            UserInfo login = userInfoService.login(userInfo);
            //根据查询到的用户信息进行判断，执行下一步的操作
            if (login != null) {
                //如果用户存在，登录成功，生成token
                Map map = new HashMap<>();
                map.put("userId", login.getId());
                map.put("nickName", login.getNickName());
                //使用jwt工具类，生成token，需要的参数：唯一的自定义的key，map中存放的是用户信息对象数据
                //ip地址是自己添加的，防止查找规律的盐
                String toekn = JwtUtil.encode(signKey, map, ip);
                System.out.println("token=" + toekn);
                return toekn;
            }
        }
        return "fail";
    }

    // 验证token
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request){
        // 从请求域中取得token，ip地址，用来解密
        String token = request.getParameter("token");
        String currentIp = request.getParameter("currentIp");

        // 准备解密，调用jwt工具类的解密方法，返回的是一个对象map
        Map<String, Object> map = JwtUtil.decode(token, signKey, currentIp);

        if (map!=null){
            // 根据存数据时使用的key，取得用户userId
            String userId = (String) map.get("userId");
            // 根据userId 判断redis中是否有登录用户,查找用户
            UserInfo userInfo = userInfoService.verify(userId);
            if (userInfo!=null){
                return "success";
            }
        }
        return "fail";
    }
}

package com.atguigu.gmall.usermanage.controller;

import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserInfoController {

    @Autowired
    UserInfoService userInfoService;

    @RequestMapping("findAll")
    public List<UserInfo> findAll(){
        return  userInfoService.getList();
    }

    @RequestMapping("findsAll")
    public List<UserInfo> findsAll(){
        return  userInfoService.getUserInfoList();
    }

    @RequestMapping(value = "insUser",method = RequestMethod.POST)
    public void insert(UserInfo userInfo){
        userInfo.setNickName("adminaaa");
        userInfo.setLoginName("bbbbbb");
        userInfoService.addUserInfo(userInfo);
    }

    @RequestMapping(value = "upd",method = RequestMethod.GET)
    public void upd(UserInfo userInfo){
        userInfo.setId("2");
        userInfo.setLoginName("aaaaa");
        userInfoService.updUserInfo(userInfo);
    }
    @RequestMapping(value = "del",method = RequestMethod.GET)
    public void del(UserInfo userInfo){
        userInfo.setId("1");
        userInfoService.delUserInfo(userInfo);
    }

}

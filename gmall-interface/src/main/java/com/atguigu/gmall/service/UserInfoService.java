package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;

import java.util.List;

public interface UserInfoService {
    public List<UserInfo> getList();
    public List<UserInfo> getUserInfoList();
    public void  addUserInfo(UserInfo userInfo);
    public  void  updUserInfo(UserInfo userInfo);
    public  void delUserInfo(UserInfo userInfo);
    public List<UserInfo> getUserInfo(UserInfo userInfo);

    public List<UserAddress> getUserAddressList(String userId);

}

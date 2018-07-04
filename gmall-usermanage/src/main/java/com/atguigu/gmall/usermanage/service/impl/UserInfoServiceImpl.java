package com.atguigu.gmall.usermanage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    UserAddressMapper userAddressMapper;

    @Override
    public List<UserInfo> getList() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> getUserInfoList() {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andLike("loginName","%a%");
        List<UserInfo> userInfos = userInfoMapper.selectByExample(example);
        return userInfos;
    }

    @Override
    public void addUserInfo(UserInfo userInfo) {
        userInfoMapper.insertSelective(userInfo);
    }

    @Override
    public void updUserInfo(UserInfo userInfo) {
//        userInfoMapper.updateByPrimaryKeySelective(userInfo);
        Example example = new Example(UserInfo.class);
        example.createCriteria().andLike("loginName","%a%").andEqualTo("id","2");
//        userInfoMapper.updateByPrimaryKeySelective(userInfo);
        userInfoMapper.updateByExampleSelective(userInfo,example);

    }


    @Override
    public void delUserInfo(UserInfo userInfo) {
        userInfoMapper.deleteByPrimaryKey(userInfo);
    }

    @Override
    public List<UserInfo> getUserInfo(UserInfo userInfo) {

        Example example = new Example(UserInfo.class);
//   example.createCriteria().
        return userInfoMapper.selectByExample(example);
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {

        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        List<UserAddress> userAddresses = userAddressMapper.select(userAddress);
        return userAddresses;
    }


}

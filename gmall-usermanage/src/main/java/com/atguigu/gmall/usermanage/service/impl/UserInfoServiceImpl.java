package com.atguigu.gmall.usermanage.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.UserAddress;
import com.atguigu.gmall.bean.UserInfo;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.UserInfoService;
import com.atguigu.gmall.usermanage.mapper.UserAddressMapper;
import com.atguigu.gmall.usermanage.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class UserInfoServiceImpl implements UserInfoService {

    @Autowired
    UserInfoMapper userInfoMapper;

    @Autowired
    UserAddressMapper userAddressMapper;

    @Autowired
    RedisUtil redisUtil;

    //三个常量，前缀，后缀，超时时间
    public String userKey_prefix = "user:";
    public String userinfoKey_suffix = ":info";
    public int userKey_timeOut = 60 * 60;

    @Override
    public List<UserInfo> getList() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserInfo> getUserInfoList() {
        Example example = new Example(UserInfo.class);
        example.createCriteria().andLike("loginName", "%a%");
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
        example.createCriteria().andLike("loginName", "%a%").andEqualTo("id", "2");
//        userInfoMapper.updateByPrimaryKeySelective(userInfo);
        userInfoMapper.updateByExampleSelective(userInfo, example);

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

    @Override
    public UserInfo login(UserInfo userInfo) {
        //调用方法，根据用户信息查询用户，只查询一个用户
        UserInfo info = userInfoMapper.selectOne(userInfo);
        if (info != null) {
            // 将用户信息放入redis中，设置超时时间，key设置好前缀和后缀
            Jedis jedis = redisUtil.getJedisPool();
            jedis.setex(userKey_prefix + info.getId() + userinfoKey_suffix, userKey_timeOut, JSON.toJSONString(info));
            //关闭redis连接池
            jedis.close();
            return info;
        }
        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        // 从redis中取得数据
        // 定义key
        String key = userKey_prefix + userId + userinfoKey_suffix;
        //拿到redis连接池对象
        Jedis jedis = redisUtil.getJedisPool();
        // 判断key是否存在
        if (jedis.exists(key)) {
            //当redis中有这个key时，延长这个key对应的时效
            jedis.expire(key, userKey_timeOut);
            //根据key获取对应的值
            String userJson = jedis.get(key);

            if (userJson != null && !"".equals(userJson)) {
                // 将userJson转换成对象
                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
                return userInfo;
            }
        }
        return null;
    }
}
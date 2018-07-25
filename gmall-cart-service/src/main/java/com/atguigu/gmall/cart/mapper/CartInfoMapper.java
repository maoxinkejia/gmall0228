package com.atguigu.gmall.cart.mapper;

import com.atguigu.gmall.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {

    // 根据userId 查找价格以及购物车的信息
    List<CartInfo> selectCartListWithCarPrice(String userId);
}

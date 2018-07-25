package com.atguigu.gmall.cart.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.cart.constant.CartConst;
import com.atguigu.gmall.cart.mapper.CartInfoMapper;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Autowired
    private RedisUtil redisUtil;
    // 需要调用getSkuInfo(userId)

    @Reference
    private ManageService manageService;


    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        // 根据用户id和商品id查看购物车CartInfo中是否有该商品
        CartInfo cartInfoQuery = new CartInfo();
        cartInfoQuery.setSkuId(skuId);
        cartInfoQuery.setUserId(userId);
        //查询一条数据拿到返回结果
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfoQuery);

        //判断返回的购物车对象
        if (cartInfoExist != null) {
            //如果不为空，说明该用户对应的购物车中有这个商品，只需要增加商品的数量即可
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);
            //根据更改后的对象对数据库进行更新
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);
            // 更改也需要变动缓存
        } else {
            // 如果购物车的对象为空，说明该用户还没有创建购物车
            // 根据skuId 查找商品信息
            SkuInfo skuInfo = manageService.getSkuInfoById(skuId);
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);
            cartInfo.setSkuName(skuInfo.getSkuName());
            // 实时价格
            cartInfo.setSkuPrice(skuInfo.getPrice());
            // 添加购物车时候的价格
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            // 将创建好的购物车对象插入数据库
            cartInfoMapper.insertSelective(cartInfo);
            //无论是更新还是新添加购物车都需要对缓存进行更新，所以这里将2个更新缓存的操作合并为一个
            // 将插入的购物车对象给已经存在的购物车对象，最后统一操作这一个对象就可以了
            cartInfoExist = cartInfo;

        }
        // 想办法将购物车数据放到redis中
        // hset(key,field,value) :key = （user:userId:cart）
        //拼接购物车key
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        // 获取redis对象
        Jedis jedis = redisUtil.getJedisPool();
        // 保存的数据
        String cartJson = JSON.toJSONString(cartInfoExist);
        jedis.hset(userCartKey, skuId, cartJson);


        // 细节的地方！user:userId+:info
        //添加完之后说明当前用户是活跃用户，拿到该用户的key
        String userInfoKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFOKEY_SUFFIX;
        //获取该用户剩余的过期时间
        Long ttl = jedis.ttl(userInfoKey);
        //给用户购物车也加上一个过期时间，跟该用户的过期时间一致
        jedis.expire(userCartKey, ttl.intValue());
        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        // 看缓存，数据库！   缓存key
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        // 创建redis对象 hset(key,field,value); hget(key,field) hget(userCartKey , skuId);
        Jedis jedis = redisUtil.getJedisPool();
        // redis=hash --- java=list;
        List<String> cartJsons = jedis.hvals(userCartKey);

        // 准备一个新的集合：
        if (cartJsons != null && !"".equals(cartJsons)) {
            List<CartInfo> cartInfoList = new ArrayList<>();
            // 循环
            for (String cartJson : cartJsons) {
                // 将对象转换成cartInfo对象
                CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
                cartInfoList.add(cartInfo);
            }
            // hash 有顺序么？ 根据id进行排序 time 外部比较器
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    return o1.getId().compareTo(o2.getId());
                }
            });
            return cartInfoList;
        } else {
            // 走数据库，验价过程！ sku_Info 中的price ，car_Info cartprice ,将数据库中的数据放入缓存
            List<CartInfo> cartInfoList = loadCartCache(userId);
            return cartInfoList;

        }

    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListFromCookie, String userId) {
        // 先到数据库中根据userId查询购物车对象集合
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCarPrice(userId);
        //遍历从cookie中拿到的购物车对象集合
        for (CartInfo cartInfoCK : cartListFromCookie) {
            // 有相同的，没有相同[insert]
            boolean isMatch = false;
            for (CartInfo infoDB : cartInfoListDB) {
                // 如果skuId 相同，则说明是同一个商品，则数据要增加
                if (cartInfoCK.getSkuId().equals(infoDB.getSkuId())) {
                    //DB的数量+ck的数量
                    infoDB.setSkuNum(cartInfoCK.getSkuNum() + infoDB.getSkuNum());
                    //更新数据库中的购物车对象
                    cartInfoMapper.updateByPrimaryKeySelective(infoDB);
                    //更改标志为true（当skuId不同时需要插入新的数据和db查询的购物车集合为空时都需要插入操作，合并为一步）
                    isMatch = true;
                }
            }
            // 合并后的插入信息
            if (!isMatch) {
                // userId 赋值
                cartInfoCK.setUserId(userId);
                //根据cookie中的购物车对象插入数据
                cartInfoMapper.insertSelective(cartInfoCK);
            }
        }
        // 根据用户id再重新到数据库中查询购物车数据，并放到缓存中，将查询到的购物车数据集合返回.
        List<CartInfo> cartInfoList = loadCartCache(userId);

        // 需要跟cookie中的数据就行匹配：{isChecked} ,根据skuId 。并且 isChecked = “1”;从新更新数据库！
        for (CartInfo cartInfo : cartInfoList) {
            for (CartInfo info : cartListFromCookie) {
                // 找skuId
                if (cartInfo.getSkuId().equals(info.getSkuId())){
                    // 并且isChecked 为 1
                    if ("1".equals(info.getIsChecked())){
                        // 将数据从新更新，redis
                        cartInfo.setIsChecked("1");
                        checkCart(userId,cartInfo.getSkuId(),info.getIsChecked());
                    }
                }
            }
        }
        return cartInfoList;
    }

    @Override
    public void checkCart(String userId, String skuId, String isChecked) {
        // 定义key，取出redis 数据
        String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedisPool();
        //根据key和field拿到唯一的value
        String cartInfoJson = jedis.hget(userCartKey, skuId);
        // 将字符串转换成对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        // 修改物品状态
        cartInfo.setIsChecked(isChecked);
        // 将修改后的数据存入redis
        jedis.hset(userCartKey, skuId, JSON.toJSONString(cartInfo));
        // 将所有选中的商品保存到一个新的key中
        String cartIsCheckKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        // 将数据放入redis,当 isChecked 为 1 时创建，0删除
        if ("1".equals(isChecked)) {
            jedis.hset(cartIsCheckKey, skuId, JSON.toJSONString(cartInfo));
        } else {
            jedis.hdel(cartIsCheckKey, skuId);
        }
    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {

        // 定义key(拿到被选中商品)，创建redis对象
        String userCheckedKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;
        Jedis jedis = redisUtil.getJedisPool();
        //拿到所有被选中商品的集合
        List<String> cartCheckedList = jedis.hvals(userCheckedKey);

        // 创建一个集合
        List<CartInfo> cartInfoList = new ArrayList<>();
        // 遍历，将字符串集合转换成对象，并添加到集合中
        for (String cartJson : cartCheckedList) {
            CartInfo cartInfo = JSON.parseObject(cartJson, CartInfo.class);
            cartInfoList.add(cartInfo);
        }
        return cartInfoList;
    }

    //根据用户id查询购物车集合，并存放到缓存中
    private List<CartInfo> loadCartCache(String userId) {
        // 在mapper中写个方法，
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCarPrice(userId);
        //  判断集合
        if (cartInfoList != null && cartInfoList.size() > 0) {
            // 准备放入redis
            Jedis jedis = redisUtil.getJedisPool();
            // 对数据进行转换 hset(key,field,value);
            String userCartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
            // field，value === 正好对应上我们的map.put(field,value) jedis.hmset(userCartKey,map);
            Map<String, String> map = new HashMap<>(cartInfoList.size());
            for (CartInfo cartInfo : cartInfoList) {
                // 将cartInfo 转换成对象
                String cartJson = JSON.toJSONString(cartInfo);
                map.put(cartInfo.getSkuId(), cartJson);
            }
            // 往redis 中添加数据
            jedis.hmset(userCartKey, map);
            jedis.close();
        }
        return cartInfoList;
    }

}

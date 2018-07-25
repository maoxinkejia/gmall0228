package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OrderDetail;
import com.atguigu.gmall.bean.OrderInfo;
import com.atguigu.gmall.bean.enums.ProcessStatus;
import com.atguigu.gmall.config.ActiveMQUtil;
import com.atguigu.gmall.config.HttpClientUtil;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.order.mapper.OrderDetailMapper;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.service.OrderService;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private ActiveMQUtil activeMQUtil;

    @Override
    public String saveOrder(OrderInfo orderInfo) {
        // 插入创建时间
        orderInfo.setCreateTime(new Date());
        // 使用日历,工具类。
        Calendar calendar = Calendar.getInstance();
        // 过期时间：当前日期的后一天（使用枚举，当前时间，一天）
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());

        // 设置out_trade_no : 第三方支付使用
        //生成一个商品订单号
        String out_trade_no = "ATGUIGU" + System.currentTimeMillis() + new Random().nextInt(1000);
        orderInfo.setOutTradeNo(out_trade_no);
        // 将准备好得订单对象保存，插入到DB中
        orderInfoMapper.insertSelective(orderInfo);

        // 因为订单对象中还有一个订单详细表，一个订单对应记录了多个订单详情，所以订单详情也要插入
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        //遍历保存订单详情，一个订单详情集合对应只有一个订单，所以orderId是固定的，唯一的
        for (OrderDetail orderDetail : orderDetailList) {
            orderDetail.setOrderId(orderInfo.getId());
            orderDetailMapper.insertSelective(orderDetail);
        }

        // 返回订单编号
        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        //根据用户id生成一个存放到redis中的key，专门用来校验表单是否重复提交
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // redis
        Jedis jedis = redisUtil.getJedisPool();
        //生成一个随机数，存放到redis中，并设置过期时间，在时间内单次提交就可以
        //超出时间或重复提交时会报错，需要用户重新下单
        String tradeNo = UUID.randomUUID().toString();
        jedis.setex(tradeNoKey, 10 * 60, tradeNo);

        //将生成的流水号返回
        return tradeNo;
    }

    @Override
    public void delTradeNo(String userId) {
        //redis: key:
        String tradeNoKey = "user:" + userId + ":tradeCode";
        // redis
        Jedis jedis = redisUtil.getJedisPool();

        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkTradeCode(String tradeNo, String userId) {
        Jedis jedis = redisUtil.getJedisPool();
        //根据用户id生成key
        String tradeNoKey = "user:" + userId + ":tradeCode";
        //去redis中查询
        String tradeCode = jedis.get(tradeNoKey);
        //如果查询到了，返回true，没查到则返回false
        if (tradeCode != null && !"".equals(tradeCode)) {
            if (tradeCode.equals(tradeNo)) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        //   调用 库存的接口 http://www.gware.com/hasStock，如果还有库存返回1，没有返回0
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        if ("1".equals(result)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        // 根据主键查询orderInfo
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        //注意：订单对象中还有订单详情集合，不是数据库的字段，如果只查询数据库的话这个集合是没有数据的，需要单独查询然后再放进订单对象中
        OrderDetail orderDetail = new OrderDetail();
        //根据订单编号查询
        orderDetail.setOrderId(orderId);
        //一个订单编号可能对应多个订单详情，所以需要查询出来的是一个集合
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        //将查询出来的订单详情集合放进订单对象中
        orderInfo.setOrderDetailList(orderDetailList);
        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus PAID) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(PAID);
        // 更新OrderStatus == PAID
        orderInfo.setOrderStatus(PAID.getOrderStatus());
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        // 创建连接
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();

            //需要的参数是一个Json字符串，根据orderId得到这个Json
            String orderJson = initWareOrder(orderId);

            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            // MessageConsumer consumer = session.createConsumer(order_result_queue);
            MessageProducer producer = session.createProducer(order_result_queue);

            // 消息内容是Json字符串 ：orderInfo ，orderDetails 。整体可以将其看作一个map,然后将map转换成Json字符串！
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            activeMQTextMessage.setText(orderJson);
            producer.send(activeMQTextMessage);
            session.commit();
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            e.printStackTrace();
        }


    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) throws InvocationTargetException, IllegalAccessException {
        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        // 第一个获取原始订单 53 === 1,2
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);
        // wareSkuMap 得到[{"wareId":"1","skuIds":["20","18"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        // 准备将仓库数据中wareId，skuIds进行循环匹配，进行拆单。
        for (Map map : maps) {
            String wareId = (String) map.get("wareId");
            List<String> skuIds = (List<String>) map.get("skuIds");
            // 设置子订单
            OrderInfo subOrderInfo = new OrderInfo();
            // 属性拷贝 id 主键自增 属性拷贝一定放在设置id为null的前面！
            BeanUtils.copyProperties(subOrderInfo, orderInfoOrigin);
            subOrderInfo.setId(null);
            subOrderInfo.setParentOrderId(orderId);
            // 子订单的details
            List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
            // 创建新的子订单详细信息
            List<OrderDetail> subOrderDetailList = new ArrayList<>();
            for (OrderDetail orderDetail : orderDetailList) {
                for (String skuId : skuIds) {
                    if (skuId.equals(orderDetail.getSkuId())) {
                        orderDetail.setId(null);
                        subOrderDetailList.add(orderDetail);
                    }
                }
            }
            // 新的子订单给新的orderInfo
            subOrderInfo.setOrderDetailList(subOrderDetailList);
            // 计算一下总钱数
            subOrderInfo.getTotalAmount();
            // 保存到数据库
            saveOrder(subOrderInfo);
            // 返回子订单集合！
            subOrderInfoList.add(subOrderInfo);
        }
        // 更改订单状态
        updateOrderStatus(orderId, ProcessStatus.SPLIT);

        return subOrderInfoList;
    }

    private String initWareOrder(String orderId) {
        // 根据主键查询，上面写过这个方法
        OrderInfo orderInfo = getOrderInfo(orderId);
        //拿到订单对象，将订单对象完善，并封装成一个map
        Map map = initWareOrder(orderInfo);
        String string = JSON.toJSONString(map);
        return string;
    }

    private Map initWareOrder(OrderInfo orderInfo) {
        Map map = new HashMap<>();
        map.put("orderId", orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel", orderInfo.getConsigneeTel());
        map.put("orderComment", orderInfo.getOrderComment());
        map.put("orderBody", orderInfo.getTradeBody());
        map.put("deliveryAddress", orderInfo.getDeliveryAddress());
//      付款方式
        map.put("paymentWay", "2");
//       仓库的Id
        map.put("wareId", orderInfo.getWareId());

//       details == 集合OrderDetails
        ArrayList<Object> arrayList = new ArrayList<>();
        // 取得到OrderDetail列表
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map mapDetail = new HashMap();
            mapDetail.put("skuId", orderDetail.getSkuId());
            mapDetail.put("skuNum", orderDetail.getSkuNum());
            mapDetail.put("skuName", orderDetail.getSkuName());
            // 将orderDtail 放入list集合中
            arrayList.add(mapDetail);
        }
        map.put("details", arrayList);
        return map;
    }

}

package com.atguigu.gmall.payment.mq;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {

    public static void main(String[] args) throws JMSException {
        // 根据ip创建工厂
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://192.168.11.136:61616");
        // 根据工厂创建连接
        Connection connection = activeMQConnectionFactory.createConnection();
        // 使用链接，启动！！！
        connection.start();
        // 使用链接创建一个消息，false：不开启事务，AUTO_ACKNOWLEDGE：自动签收
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        // 创建一个队列
        Queue queue = session.createQueue("Atguigu");
        // 根据队列，创建提供者
        MessageProducer producer = session.createProducer(queue);
        // 消息持久化
        //   producer.setDeliveryMode(DeliveryMode.PERSISTENT);
        // 创建一个消息对象
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("这么困呢？晚上干啥去了！");
        // 准备发送消息
        producer.send(activeMQTextMessage);
        // 关闭的时候，如果有事务开启，则必须先提交事务  session.commit();
        producer.close();
        session.close();
        connection.close();
    }
}

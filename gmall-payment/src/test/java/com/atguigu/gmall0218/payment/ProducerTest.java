package com.atguigu.gmall0218.payment;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQTextMessage;

import javax.jms.*;

public class ProducerTest {
    public static void main(String[] args) throws JMSException {
        /*
            1.  创建连接工厂
            2.  创建连接
            3.  打开连接
            4.  创建 Session
            5.  创建队列 Session 创建 Queue
            6.  创建消息提供者
            7.  创建消息对象
            8.  发送消息
            9.  关闭
         */

        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://81.69.33.96:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();

        //第一个参数：是否开启事务
        //第二个参数：表示开启/关闭事务的相应配置参数

        //Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED); //必须提交
        Queue atguigu = session.createQueue("atguigu-true");

        MessageProducer producer = session.createProducer(atguigu);

        //文本
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("Hello World!");
        producer.send(activeMQTextMessage);

        //提交
        session.commit();

        producer.close();
        session.close();
        connection.close();

    }
}

package com.atguigu.gmall0218.payment;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;

public class ConsumerTest {

    public static void main(String[] args) throws JMSException {
        /*
            1.  创建连接工厂
            2.  创建连接
            3.  打开连接
            4.  创建 Session
            5.  创建队列 Session 创建 Queue
            6.  创建消息消费者
            7.  消费消息
         */

        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(ActiveMQConnection.DEFAULT_USER, ActiveMQConnection.DEFAULT_PASSWORD, "tcp://81.69.33.96:61616");
        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();

        // 第一个参数：是否开启事务
        // 第二个参数：表示开启/关闭事务的相应配置参数

        //Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED); //必须提交
        Queue atguigu = session.createQueue("atguigu-true");

        MessageConsumer consumer = session.createConsumer(atguigu);

        // 消息监听器
        consumer.setMessageListener(new MessageListener() {
            // Message javax.jms；
            @Override
            public void onMessage(Message message) {
                // 如何获取消息
                if(message instanceof TextMessage){
                    try {
                        String text = ((TextMessage) message).getText();
                        System.out.println("获取的消息："+ text);
                    } catch (JMSException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }
}

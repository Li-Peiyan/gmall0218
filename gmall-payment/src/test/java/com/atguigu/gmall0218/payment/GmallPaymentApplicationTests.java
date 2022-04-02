package com.atguigu.gmall0218.payment;

import com.atguigu.gmall0218.config.ActiveMQConfig;
import com.atguigu.gmall0218.config.ActiveMQUtil;;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.jms.*;

@SpringBootTest
class GmallPaymentApplicationTests {

    private ActiveMQConfig activeMQConfig = new ActiveMQConfig();

    @Test
    public void testM() throws JMSException {

        ActiveMQUtil activeMQUtil = activeMQConfig.getActiveMQUtil();
        Connection connection = activeMQUtil.getConnection();
//        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory("tcp://81.69.33.96:61616");
//        Connection connection = activeMQConnectionFactory.createConnection();
        connection.start();

        //第一个参数：是否开启事务
        //第二个参数：表示开启/关闭事务的相应配置参数

        //Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Session session = connection.createSession(true, Session.SESSION_TRANSACTED); //必须提交
        Queue atguigu = session.createQueue("atguigu-test");

        MessageProducer producer = session.createProducer(atguigu);

        //文本
        ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
        activeMQTextMessage.setText("Hello World test!");
        producer.send(activeMQTextMessage);

        //提交
        session.commit();

        producer.close();
        session.close();
        connection.close();



    }

}

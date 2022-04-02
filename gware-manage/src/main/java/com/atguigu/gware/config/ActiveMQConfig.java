package com.atguigu.gware.config;




import javax.jms.JMSException;


/**
 * @param
 * @return
 */

public class ActiveMQConfig {

    //@Value("${spring.activemq.broker-url:novalue}")
    String brokerURL = "tcp://81.69.33.96:61616" ;


    public    ActiveMQUtil   getActiveMQUtil() throws JMSException {
        if(brokerURL.equals("novalue")){
            return null;
        }
        ActiveMQUtil activeMQUtil=new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return activeMQUtil;
    }




}

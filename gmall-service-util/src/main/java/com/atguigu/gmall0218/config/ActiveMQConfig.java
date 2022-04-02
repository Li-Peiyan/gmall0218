package com.atguigu.gmall0218.config;

public class ActiveMQConfig {

    ActiveMQConst activeMQConst = new ActiveMQConst();
//    @Value("${spring.activemq.broker-url:disabled}")
    private String brokerURL = activeMQConst.getBrokerURL();


    // 在 Spring 容器中添加一个 ActiveMQUtil 的实例
    public ActiveMQUtil getActiveMQUtil(){
        if ("disabled".equals(brokerURL)){
            return null;
        }
        ActiveMQUtil activeMQUtil = new ActiveMQUtil();
        activeMQUtil.init(brokerURL);
        return  activeMQUtil;
    }

//    // 专门用来配置消息监听器工厂！
//    public DefaultJmsListenerContainerFactory jmsQueueListenerContainerFactory(ActiveMQConnectionFactory activeMQConnectionFactory) {
//
//        if("disabled".equals(listenerEnable)){
//            return null;
//        }
//        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//        factory.setConnectionFactory(activeMQConnectionFactory);
//        // 设置事务
//        factory.setSessionTransacted(false);
//        // 自动签收
//        factory.setSessionAcknowledgeMode(Session.AUTO_ACKNOWLEDGE);
//        // 设置并发数
//        factory.setConcurrency("5");
//        // 重连间隔时间
//        factory.setRecoveryInterval(5000L);
//
//        return factory;
//    }
//    // 接收消息的工厂
//    public ActiveMQConnectionFactory activeMQConnectionFactory ( ){
//        ActiveMQConnectionFactory activeMQConnectionFactory =
//                new ActiveMQConnectionFactory(brokerURL);
//        return activeMQConnectionFactory;
//    }
}

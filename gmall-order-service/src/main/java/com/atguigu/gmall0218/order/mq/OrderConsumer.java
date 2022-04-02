package com.atguigu.gmall0218.order.mq;

import com.atguigu.gmall0218.bean.enums.ProcessStatus;
import com.atguigu.gmall0218.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderConsumer {

    @Autowired
    private OrderService orderService;

    // 获取消息队列中的数据
    /**
     * destination 监听消息队列
     * containerFactory 要使用的消息监听器工厂
     */
    @JmsListener(destination = "PAYMENT_RESULT_QUEUE", containerFactory = "jmsQueueListener")
    public  void  consumerPaymentResult(MapMessage mapMessage) throws JMSException {
        // 通过 mapMessage 获取支付结果消息
        String orderId = mapMessage.getString("orderId");
        String result = mapMessage.getString("result");

        System.out.println("result = " + result);
        System.out.println("orderId = " + orderId);
        if ("success".equals(result)){
            // 更新订单消息
            orderService.updateOrderStatus(orderId, ProcessStatus.PAID);

            // 发送消息给仓库
            orderService.sendOrderStatus(orderId);
            // 更新订单消息
            orderService.updateOrderStatus(orderId, ProcessStatus.NOTIFIED_WARE);
        }else {
            orderService.updateOrderStatus(orderId,ProcessStatus.UNPAID);
        }
    }

    @JmsListener(destination = "SKU_DEDUCT_QUEUE", containerFactory = "jmsQueueListener")
    public  void  consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        // 通过 mapMessage 获取仓库减库存结果消息
        String orderId = mapMessage.getString("orderId");
        String status = mapMessage.getString("status");

        System.out.println("orderId = " + orderId);
        System.out.println("status = " + status);
        if("DEDUCTED".equals(status)){
            orderService.updateOrderStatus(  orderId , ProcessStatus.WAITING_DELEVER);
        }else{
            orderService.updateOrderStatus(  orderId , ProcessStatus.STOCK_EXCEPTION);
        }

    }
}

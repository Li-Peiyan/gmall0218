package com.atguigu.gmall0218.payment.mq;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.PaymentInfo;
import com.atguigu.gmall0218.bean.enums.ProcessStatus;
import com.atguigu.gmall0218.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class PaymentConsumer {

    @Reference
    private PaymentService paymentService;


    // 消费检查是否支付成功的消息队列
    @JmsListener(destination = "PAYMENT_RESULT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public  void  consumeSkuDeduct(MapMessage mapMessage) throws JMSException {
        // 通过 mapMessage 获取支付结果消息
        String outTradeNo = mapMessage.getString("outTradeNo");
        int delaySec = mapMessage.getInt("delaySec");
        int checkCount = mapMessage.getInt("checkCount");

        System.out.println("outTradeNo = " + outTradeNo);System.out.println("delaySec = " + delaySec);System.out.println("checkCount = "+ checkCount);

        // 创建一个paymentInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOutTradeNo(outTradeNo);
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);

        // 判断是否支付成功
        // 需要有参数 out_trade_no 和 orderId
        boolean result = paymentService.checkPayment(paymentInfoQuery);
        System.out.println("检查结果："+ result);

        // 判断是否消费成功
        // 失败
        if(!result && checkCount > 0){
            System.out.println("检查次数："+ checkCount);
            // 调用发送消息的方法
            paymentService.sendDelayPaymentResult(outTradeNo, delaySec, checkCount - 1);

        }

    }

}

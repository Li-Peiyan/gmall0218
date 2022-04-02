package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    /**
     * 保存交易记录
     * @param paymentInfo
     */
    void savePaymentInfo(PaymentInfo paymentInfo);

    /**
     * 查询交易信息
     * @param paymentInfo
     */
    PaymentInfo getPaymentInfo(PaymentInfo paymentInfo);

    /**
     * 根据 第三方交易编号 更新交易信息
     * @param out_trade_no
     * @param paymentInfo
     */
    void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfo);

    /**
     * 支付宝退款接口
     * @param orderId
     * @return
     */
    boolean refund(String orderId);

    /**
     * 微信生成支付二维码
     * @param orderId
     * @param s
     * @return
     */
    Map createNative(String orderId, String s);

    /**
     * 发送消息给订单
     * @param paymentInfo
     * @param result
     */
    void sendPaymentResult(PaymentInfo paymentInfo,String result);

    /**
     * 根据 out_trade_no 去查询交易记录
     * @param paymentInfoQuery
     * @return
     */
    boolean checkPayment(PaymentInfo paymentInfoQuery);

    /**
     *
     * 发送消息的延迟队列, 反复调用
     * @param outTradeNo 第三方交易编号
     * @param delaySec 每隔多长时间查询一次
     * @param checkCount 查询次数
     */
    void sendDelayPaymentResult(String outTradeNo,int delaySec ,int checkCount);

    /**
     * 根据订单 id 关闭交易记录
     * @param orderId
     */
    void closePayment(String orderId);
}

package com.atguigu.gmall0218.payment.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.atguigu.gmall0218.bean.OrderInfo;
import com.atguigu.gmall0218.bean.PaymentInfo;
import com.atguigu.gmall0218.bean.enums.PaymentStatus;
import com.atguigu.gmall0218.config.ActiveMQConfig;
import com.atguigu.gmall0218.config.ActiveMQUtil;
import com.atguigu.gmall0218.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall0218.service.OrderService;
import com.atguigu.gmall0218.service.PaymentService;
import com.atguigu.gmall0218.util.HttpClient;
import com.github.wxpay.sdk.WXPayUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentInfoMapper paymentInfoMapper;

    @Autowired
    private AlipayClient alipayClient;

    @Reference
    private OrderService orderService;

    // 手动注入
    private ActiveMQConfig activeMQConfig = new ActiveMQConfig();

    // 服务号Id
    @Value("${appid}")
    private String appid;
    // 商户号Id
    @Value("${partner}")
    private String partner;
    // 密钥
    @Value("${partnerkey}")
    private String partnerkey;


    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public PaymentInfo getPaymentInfo(PaymentInfo paymentInfo) {
        return paymentInfoMapper.selectOne(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(String out_trade_no, PaymentInfo paymentInfo) {
        // 更新
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("outTradeNo", out_trade_no);
        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }

    @Override
    public boolean refund(String orderId) {
        //通过 orderId 获取交易信息
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        // AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();

        HashMap<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", orderInfo.getOutTradeNo());
        bizContent.put("refund_amount", 0.01); // orderInfo.getTotalAmount()
        bizContent.put("refund_reason", "买多了！");

        //// 返回参数选项，按需传入
        //JSONArray queryOptions = new JSONArray();
        //queryOptions.add("refund_detail_item_list");
        //bizContent.put("query_options", queryOptions);

        request.setBizContent(JSON.toJSONString(bizContent));

        AlipayTradeRefundResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            // 更新状态！
            System.out.println("调用成功");
            return true;
        } else {
            System.out.println("调用失败");
            return false;
        }
    }

    @Override
    public Map createNative(String orderId, String money) {
        /*
            1.  制作参数使用 map
            2.  将 map 装换成 xml
            3.  获取直接结果
         */
        HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("appid", appid);//公众号
            hashMap.put("mch_id", partner);//商户号
            hashMap.put("nonce_str", WXPayUtil.generateNonceStr());//随机字符串
            hashMap.put("body", "送手机。");//商品描述
            hashMap.put("out_trade_no", orderId);//商户订单号
            hashMap.put("total_fee", money);//总金额（分）
            hashMap.put("spbill_create_ip", "127.0.0.1");//IP
            hashMap.put("notify_url", "http://order.gmall.com/trade");//回调地址(随便写)
            hashMap.put("trade_type", "NATIVE");//交易类型
        try {
            // 生成 xml， 以 post 请求发送给支付接口
            String xmlParam = WXPayUtil.generateSignedXml(hashMap, partnerkey);
            // 导入工具类
            HttpClient httpClient = new HttpClient("https://api.mch.weixin.qq.com/pay/unifiedorder");
            // 设置 https 请求
            httpClient.setHttps(true);
            // 将 xmlParam 发送到接口上
            httpClient.setXmlParam(xmlParam);
            // 以 post 请求
            httpClient.post();


            // 获取结果: 将结果集放入 map 中!
            String result = httpClient.getContent();
            Map<String, String> xmlToMap = WXPayUtil.xmlToMap(result);

            HashMap<String, String> resultMap = new HashMap<>();
            resultMap.put("code_url", resultMap.get("code_url"));//支付地址
            resultMap.put("total_fee", money);//总金额
            resultMap.put("out_trade_no", orderId);//订单号


            return resultMap;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void sendPaymentResult(PaymentInfo paymentInfo, String result) {
        // 创建消息工厂

        ActiveMQUtil activeMQUtil = activeMQConfig.getActiveMQUtil();

        Connection connection = activeMQUtil.getConnection();

        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue payment_result_queue = session.createQueue("PAYMENT_RESULT_QUEUE");
            // 创建消息提者
            MessageProducer producer = session.createProducer(payment_result_queue);
            // 创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("orderId", paymentInfo.getOrderId());
            activeMQMapMessage.setString("result", result);

            producer.send(activeMQMapMessage);

            // 提交
            session.commit();
            //关闭
            closeAll(connection, session, producer);

        } catch (JMSException e) {
            e.printStackTrace();
        }



    }

    // 查询支付交易是否成功！需要根据 orderId 查询！
    // http://payment.gmall.com/queryPaymentResult?orderId=93
    @Override
    public boolean checkPayment(PaymentInfo paymentInfoQuery) {
        // 判断订单状态
        if(paymentInfoQuery.getPaymentStatus() == PaymentStatus.PAID || paymentInfoQuery.getPaymentStatus() == PaymentStatus.ClOSED){
            return true;
        }

        //AlipayClient alipayClient = new DefaultAlipayClient("https://openapi.alipay.com/gateway.do","app_id","your private_key","json","GBK","alipay_public_key","RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", paymentInfoQuery.getOutTradeNo());

        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(response.isSuccess()){
            // 表示有交易记录
            if("TRADE_SUCCESS".equals(response.getTradeStatus()) || "TRADE_FINISHED".equals(response.getTradeStatus()) ){
                // 表示支付成功
                System.out.println("支付成功");
                // 更新状态
                PaymentInfo paymentInfoUpd = new PaymentInfo();
                paymentInfoUpd.setPaymentStatus(PaymentStatus.PAID);
                // 根据 out_trade_no 更新状态
                updatePaymentInfo(paymentInfoQuery.getOutTradeNo(), paymentInfoUpd);
                // 通知订单支付完成
                paymentInfoQuery.setPaymentStatus(PaymentStatus.PAID);
                sendPaymentResult(paymentInfoQuery,"success");
                return true;
            }else{
                System.out.println("支付失败");
                return false;
            }
        } else {
            System.out.println("支付失败");
            return false;
        }
    }

    @Override
    public void sendDelayPaymentResult(String outTradeNo, int delaySec, int checkCount) {
        // 创建工厂
        ActiveMQUtil activeMQUtil = activeMQConfig.getActiveMQUtil();
        Connection connection = activeMQUtil.getConnection();
        try {
            connection.start();
            // 创建 session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue payment_result_check_queue = session.createQueue("PAYMENT_RESULT_CHECK_QUEUE");
            // 创建 发送者
            MessageProducer producer = session.createProducer(payment_result_check_queue);
            // 创建消息对象
            ActiveMQMapMessage activeMQMapMessage = new ActiveMQMapMessage();
            activeMQMapMessage.setString("outTradeNo", outTradeNo);
            activeMQMapMessage.setInt("delaySec", delaySec);
            activeMQMapMessage.setInt("checkCount", checkCount);

            // 设置延迟队列的开启                                                        // delaySec 默认毫秒
            activeMQMapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY,delaySec*1000);

            // 发送
            producer.send(activeMQMapMessage);

            // 提交
            session.commit();

            // 关闭
            closeAll(connection, session, producer);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void closePayment(String orderId) {
        // 根据 orderId 获得 out_trade_no
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentStatus(PaymentStatus.ClOSED);
        // 更新
        Example example = new Example(PaymentInfo.class);
        example.createCriteria().andEqualTo("orderId", orderId);

        paymentInfoMapper.updateByExampleSelective(paymentInfo, example);
    }

    /**
     * 关闭消息队列
     * @param connection
     * @param session
     * @param producer
     * @throws JMSException
     */
    public void closeAll(Connection connection, Session session, MessageProducer producer) throws JMSException {
        producer.close();
        connection.close();
        session.close();
    }

}

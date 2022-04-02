package com.atguigu.gmall0218.payment.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;

import com.atguigu.gmall0218.bean.OrderInfo;
import com.atguigu.gmall0218.bean.PaymentInfo;
import com.atguigu.gmall0218.bean.enums.OrderStatus;
import com.atguigu.gmall0218.bean.enums.PaymentStatus;
import com.atguigu.gmall0218.payment.config.AlipayConfig;
import com.atguigu.gmall0218.service.OrderService;
import com.atguigu.gmall0218.service.PaymentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;


@Controller
public class PaymentController {
    @Reference
    private OrderService orderService;

    @Reference
    private PaymentService paymentService;

    @Autowired
    private AlipayClient alipayClient;

    @RequestMapping("index")
    public String index(HttpServletRequest request, String orderId){
        //选中支付渠道
        //获取总金额
        OrderInfo orderInfo = orderService.getOrderInfo(orderId);

        //保存到作用域
        //保存订单 Id
        request.setAttribute("orderId", orderId);
        //保存总金额
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        // 保存支付信息 属性赋值
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo.setOutTradeNo(orderInfo.getOutTradeNo());
        paymentInfo.setTotalAmount(orderInfo.getTotalAmount());
        paymentInfo.setSubject("进大厂，发Mac！");
        paymentInfo.setPaymentStatus(PaymentStatus.UNPAID);
        paymentInfo.setCreateTime(new Date());
        //保存信息
        paymentService.savePaymentInfo(paymentInfo);

        return "index";
    }

    @RequestMapping("alipay/submit")
    @ResponseBody
    private String alipay(HttpServletRequest request, HttpServletResponse response) {
        /*
            1.  保存支付记录 将数据放入数据库
                去重，对账!  幂等性=保障每笔交易只能交易一次（第三方交易编号 outTradeNo）！
                paymentInfo
            2.  生成二维码
         */

        //获取 orderId
        String orderId = request.getParameter("orderId");
        //获取订单信息
        PaymentInfo paymentInfoo = new PaymentInfo();
        paymentInfoo.setOrderId(orderId);
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoo);

        if(paymentInfo.getPaymentStatus() != PaymentStatus.UNPAID ) {
            System.out.println("订单不是可支付状态！");
            return "";
        }

        //生成二维码
        // 支付宝参数
            //AlipayClient alipayClient =  new DefaultAlipayClient( "https://openapi.alipay.com/gateway.do" , APP_ID, APP_PRIVATE_KEY, FORMAT, CHARSET, ALIPAY_PUBLIC_KEY, SIGN_TYPE);  //获得初始化的AlipayClient
        // alipay.trade.page.pay
        AlipayTradePagePayRequest alipayRequest =  new  AlipayTradePagePayRequest(); //创建API对应的request
        // 设置同步回调， url
        alipayRequest.setReturnUrl(AlipayConfig.return_payment_url);
        // 设置异步回调， url
        alipayRequest.setNotifyUrl(AlipayConfig.notify_payment_url); //在公共参数中设置回跳和通知地址
        // 生成二维码的参数
        // 声明一个 map 集合来存储参数
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("out_trade_no", paymentInfo.getOutTradeNo());
        hashMap.put("product_code", "FAST_INSTANT_TRADE_PAY");
        hashMap.put("total_amount", "0.01");//paymentInfo.getTotalAmount()
        hashMap.put("subject", paymentInfo.getSubject());

        String s = JSON.toJSONString(hashMap);
        //将封装好的参数传递给支付宝！
        alipayRequest.setBizContent(s);

        String form= "" ;
        try  {
            form = alipayClient.pageExecute(alipayRequest).getBody();  //调用SDK生成表单
        }  catch  (AlipayApiException e) {
            e.printStackTrace();
        }
        response.setContentType( "text/html;charset=UTF-8");
//        response.getWriter().write(form); //直接将完整的表单html输出到页面
//        response.getWriter().flush();
//        response.getWriter().close();

        // 调用延迟队列
        paymentService.sendDelayPaymentResult(paymentInfo.getOutTradeNo(),15,3);

        return form;
    }

    @RequestMapping("alipay/callback/return")
    //同步回调
    public String callbackReturn(){
        //付款完成之后，订单，购物车应该删除

        return "redirect:"+AlipayConfig.return_order_url;
    }

    @RequestMapping("alipay/callback/notify")
    public String callbackNotify(@RequestParam Map<String,String> paramMap, HttpServletRequest request) {

        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false;//调用SDK验证签名
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            // 对业务的二次校验
            // 只有交易通知状态为 TRADE_SUCCESS 或 TRADE_FINISHED 时，支付宝才会认定为买家付款成功
            // 支付成功之后，需要做什么？

            // 需要得到 trade_status 交易状态
            String trade_status = paramMap.get("trade_status");
            // 通过 out_trade_no 订单号 获取订单支付状态
            String out_trade_no = paramMap.get("out_trade_no");
            // 验证总金额
            String total_amount = paramMap.get("total_amount");

            if("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status) ){

                // 当前订单支付状态 如果是已付款，或者是关闭
                PaymentInfo paymentInfoQuery = new PaymentInfo();
                paymentInfoQuery.setOutTradeNo(out_trade_no);
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
                if(paymentInfo.getPaymentStatus() == PaymentStatus.PAID || paymentInfo.getPaymentStatus() == PaymentStatus.ClOSED){
                    return "failure";
                }

                // 验证总金额
                if( !paymentInfo.getTotalAmount().toString().equals(total_amount)){
                    return "failure";
                }

                //更新交易记录状态
                PaymentInfo paymentInfoUPD = new PaymentInfo();
                paymentInfoUPD.setPaymentStatus(PaymentStatus.PAID);
                paymentInfoUPD.setCallbackTime(new Date());
                paymentService.updatePaymentInfo(out_trade_no, paymentInfoUPD);

                paymentInfo = paymentService.getPaymentInfo(paymentInfoQuery);
                // 发送消息给订单
                paymentService.sendPaymentResult(paymentInfo, "success");

                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }

        return "failure";
    }

    @RequestMapping("refund")
    @ResponseBody
    // 根据 orderId 退款
    public String refund(String orderId){
        boolean flag = paymentService.refund(orderId);
        System.out.println("flag:"+flag);
        return flag+"";
    }

    @RequestMapping("wx/submit")
    @ResponseBody
    public Map wxSubmit(String orderId){

        //  // 做一个判断：支付日志中的订单支付状态 如果是已支付，则不生成二维码直接重定向到消息提示页面！
        // 调用服务层数据
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        paymentInfo = paymentService.getPaymentInfo(paymentInfo);
        if(paymentInfo.getPaymentStatus() == PaymentStatus.UNPAID ){
            // orderId 是订单编号 ，"1"表示金额 1分
            Map map = paymentService.createNative(orderId, "1");
            System.out.println(map.get("code_url"));
            return map;
        }

        System.out.println("订单不是可支付状态！");
        return new HashMap();

    }


    // 测试 消息队列
    // http://payment.gmall.com/sendPaymentResult?orderId=95&result=success
    @RequestMapping("sendPaymentResult")
    @ResponseBody
    public String sendPaymentResult(PaymentInfo paymentInfo,@RequestParam("result") String result){
        // 发送消息给订单
        paymentService.sendPaymentResult(paymentInfo, "success");

        return "OK";
    }

    // 查询订单支付状态
    // http://payment.gmall.com/queryPaymentResult?orderId=93
    @RequestMapping("queryPaymentResult")
    @ResponseBody
    public String queryPaymentResult(String orderId){
        // 通过 orderId 查询 orderInfo
        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setOrderId(orderId);
        // 查询， 该对象中必须要有 out_trade_no 和 orderId
        PaymentInfo paymentInfoQuery = paymentService.getPaymentInfo(paymentInfo);

        boolean flag = paymentService.checkPayment(paymentInfoQuery);
        return ""+flag;
    }


}

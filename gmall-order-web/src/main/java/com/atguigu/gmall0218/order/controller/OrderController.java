package com.atguigu.gmall0218.order.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.*;
import com.atguigu.gmall0218.bean.enums.OrderStatus;
import com.atguigu.gmall0218.bean.enums.PaymentWay;
import com.atguigu.gmall0218.config.LoginRequire;
import com.atguigu.gmall0218.service.CartService;
import com.atguigu.gmall0218.service.ManageService;
import com.atguigu.gmall0218.service.OrderService;
import com.atguigu.gmall0218.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;


import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@CrossOrigin
public class OrderController {

    @Reference
    private UserService userService;

    @Reference
    private CartService cartService;

    @Reference
    private OrderService orderService;

    @Reference
    private ManageService manageService;

    @RequestMapping("trade")
    @LoginRequire
    public String trade(HttpServletRequest request){
        String userId = (String) request.getAttribute("userId");
        //获取用户信息
        List<UserAddress> userAddressList = userService.getUserAddressList(userId);

        //获取勾选的购物清单
        List<CartInfo> cartInfoList = cartService.getCartCheckedList(userId);
        //将集合赋值给 OrderDetail
        List<OrderDetail> orderDetailList = new ArrayList<>();
        if(cartInfoList != null && cartInfoList.size() > 0) {
            for (CartInfo cartInfo : cartInfoList) {
                OrderDetail orderDetail = new OrderDetail();
                orderDetail.setSkuId(cartInfo.getSkuId());
                orderDetail.setSkuName(cartInfo.getSkuName());
                orderDetail.setImgUrl(cartInfo.getImgUrl());
                orderDetail.setSkuNum(cartInfo.getSkuNum());
                orderDetail.setOrderPrice(cartInfo.getCartPrice());
                orderDetailList.add(orderDetail);
            }
        }
        //总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        //放入作用域
        //用户信息
        request.setAttribute("userAddressList", userAddressList);
        //商品详情
        request.setAttribute("orderDetailList", orderDetailList);
        //总金额
        request.setAttribute("totalAmount", orderInfo.getTotalAmount());

        //生成流水号
        String tradeNo = orderService.getTradeNo(userId);
        request.setAttribute("tradeNo", tradeNo);

        //返回一个用户信息
        return "trade";
    }

    @RequestMapping("submitOrder")
    @LoginRequire
    public String submitOrder(HttpServletRequest request, OrderInfo orderInfo){
        String userId = (String) request.getAttribute("userId");
        //orderInfo 中还缺少一个 userId
        orderInfo.setUserId(userId);

        //判断是否重复提交,第一次提交会删除缓存中的流水号，重复提交时已查询不到流水号
        //获取流水号
        String tradeNo = request.getParameter("tradeNo");
        //调用比较方法
        boolean flag = orderService.checkTradeCode(userId, tradeNo);
        //能查到流水号说明还未提交
        if(!flag){
            request.setAttribute("errMsg","订单已提交，不能重复提交订单！");
            return "tradeFail";
        }

        //获取详细订单
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //验证库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());

            //只要存在一个订单不能满足则中止订单
            if(!result){
                request.setAttribute("errMsg",orderDetail.getSkuName() + " 商品库存不足！");
                return "tradeFail";
            }


            //获取实时价格
            SkuInfo skuInfo = manageService.getSkuInfo(orderDetail.getSkuId());
            BigDecimal price = skuInfo.getPrice();
//            BigDecimal subtract = price.subtract(orderDetail.getOrderPrice());
//            if(subtract.equals(0)){}
            int res = price.compareTo(orderDetail.getOrderPrice());
            if(res != 0){
                request.setAttribute("errMsg",orderDetail.getSkuName() + " 价格不匹配！");
                //更新购物车缓存
                cartService.loadCartCache(userId);
                return "tradeFail";
            }


        }

        //保存订单
        String orderId = orderService.saveOrder(orderInfo);

        //删除流水号
        orderService.delTradeCode(userId);

        //支付订单
        return "redirect://payment.gmall.com/index?orderId="+orderId;
    }


    @RequestMapping("orderSplit")
    @ResponseBody
    public String orderSplit(HttpServletRequest request){
        // 获得参数
        String orderId = request.getParameter("orderId");
        String wareSkuMap = request.getParameter("wareSkuMap");
        // 返回子订单集合
        List<OrderInfo> orderInfoList = orderService.orderSplit(orderId, wareSkuMap);

        // 创建一个集合来存储 Map
        ArrayList<Map> mapArrayList = new ArrayList<>();

        // 循环遍历
        for (OrderInfo orderInfo : orderInfoList) {
            // orderInfo 转换成 map
            Map map = orderService.initWareOrder(orderInfo);
            mapArrayList.add(map);
        }

        return JSON.toJSONString(mapArrayList);
    }

}
















package com.atguigu.gmall0218.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0218.bean.OrderDetail;
import com.atguigu.gmall0218.bean.OrderInfo;
import com.atguigu.gmall0218.bean.enums.OrderStatus;
import com.atguigu.gmall0218.bean.enums.ProcessStatus;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0218.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0218.service.OrderService;
import com.atguigu.gmall0218.util.HttpClientUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    @Transactional
    public String saveOrder(OrderInfo orderInfo) {
        //初始化参数
        //计算总金额
        orderInfo.sumTotalAmount();
        //订单状态
        orderInfo.setOrderStatus(OrderStatus.UNPAID);
        //第三方交易编号
        String outTradeNo="ATGUIGU"+System.currentTimeMillis()+""+new Random().nextInt(1000);
        orderInfo.setOutTradeNo(outTradeNo);
        //创建时间
        orderInfo.setCreateTime(new Date());
        //过期时间
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 1);
        orderInfo.setExpireTime(calendar.getTime());

        //进程状态
        orderInfo.setProcessStatus(ProcessStatus.UNPAID);

        //只保存了一份订单
        orderInfoMapper.insertSelective(orderInfo);

        //订单明细
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            //设置 orderId
            orderDetail.setOrderId(orderInfo.getId());
            //插入表中
            orderDetailMapper.insertSelective(orderDetail);
        }



        return orderInfo.getId();
    }

    @Override
    public String getTradeNo(String userId) {
        Jedis jedis = redisUtil.getJedis();
        //Key
        String tradeNoKey="user:"+userId+":tradeCode";
        //流水号
        String tradeNo = UUID.randomUUID().toString();

        jedis.set(tradeNoKey, tradeNo);

        jedis.close();

        return tradeNo;
    }

    @Override
    public boolean checkTradeCode(String userId, String tradeCodeNo) {
        Jedis jedis = redisUtil.getJedis();
        //Key
        String tradeNoKey="user:"+userId+":tradeCode";
        String tradeNo = jedis.get(tradeNoKey);

        jedis.close();

        return tradeCodeNo.equals(tradeNo);
    }

    @Override
    public void delTradeCode(String userId) {
        Jedis jedis = redisUtil.getJedis();
        //Key
        String tradeNoKey="user:"+userId+":tradeCode";
        //删除
        jedis.del(tradeNoKey);
        jedis.close();
    }

    @Override
    public boolean checkStock(String skuId, Integer skuNum) {
        //调用 gware-manage 库存系统 http://www.gware.com/hasStock?skuId=10221&num=2
        String result = HttpClientUtil.doGet("http://www.gware.com/hasStock?skuId=" + skuId + "&num=" + skuNum);
        //0：无库存   1：有库存
        return "1".equals(result);
    }
}

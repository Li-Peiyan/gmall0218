package com.atguigu.gmall0218.order.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.atguigu.gmall0218.bean.OrderDetail;
import com.atguigu.gmall0218.bean.OrderInfo;
import com.atguigu.gmall0218.bean.enums.OrderStatus;
import com.atguigu.gmall0218.bean.enums.PaymentWay;
import com.atguigu.gmall0218.bean.enums.ProcessStatus;
import com.atguigu.gmall0218.config.ActiveMQConfig;
import com.atguigu.gmall0218.config.ActiveMQUtil;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.order.mapper.OrderDetailMapper;
import com.atguigu.gmall0218.order.mapper.OrderInfoMapper;
import com.atguigu.gmall0218.service.OrderService;
import com.atguigu.gmall0218.service.PaymentService;
import com.atguigu.gmall0218.util.HttpClientUtil;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import javax.jms.Queue;
import java.util.*;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private OrderInfoMapper orderInfoMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Reference
    private PaymentService paymentService;

    // 手动注入
    private ActiveMQConfig activeMQConfig = new ActiveMQConfig();

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
        //支付方式
        orderInfo.setPaymentWay(PaymentWay.ONLINE);
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

    @Override
    public OrderInfo getOrderInfo(String orderId) {
        // 查询订单
        OrderInfo orderInfo = orderInfoMapper.selectByPrimaryKey(orderId);
        // 放入 OrderDetailList
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrderId(orderId);
        List<OrderDetail> orderDetailList = orderDetailMapper.select(orderDetail);
        orderInfo.setOrderDetailList(orderDetailList);

        return orderInfo;
    }

    @Override
    public void updateOrderStatus(String orderId, ProcessStatus processStatus) {
        // 设置 id ，订单状态
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setProcessStatus(processStatus);
        orderInfo.setOrderStatus(processStatus.getOrderStatus());
        // 更新
        orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
    }

    @Override
    public void sendOrderStatus(String orderId) {
        // 创建消息工厂
        ActiveMQUtil activeMQUtil = activeMQConfig.getActiveMQUtil();

        Connection connection = activeMQUtil.getConnection();

        // ordrtInfo 组成的 JSON 字符串
        String orderInfoJson = initWareOrder(orderId);

        try {
            connection.start();
            // 创建session
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 创建队列
            Queue order_result_queue = session.createQueue("ORDER_RESULT_QUEUE");
            // 创建消息提者
            MessageProducer producer = session.createProducer(order_result_queue);
            // 创建消息对象
            ActiveMQTextMessage activeMQTextMessage = new ActiveMQTextMessage();
            // ordrtInfo 组成的 JSON 字符串
            activeMQTextMessage.setText(orderInfoJson);

            producer.send(activeMQTextMessage);

            // 提交
            session.commit();
            //关闭
            closeAll(connection, session, producer);

        } catch (JMSException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<OrderInfo> getExpiredOrderList() {
        // 查询过期订单
        Example example = new Example(OrderInfo.class);
        example.createCriteria().andEqualTo("processStatus",ProcessStatus.UNPAID).andLessThan("expireTime",new Date());

        List<OrderInfo> orderInfoList = orderInfoMapper.selectByExample(example);
        return orderInfoList;
    }

    @Override
    @Async
    public void execExpiredOrder(OrderInfo orderInfo) {
        // 将订单状态改为关闭
        // 订单信息
        updateOrderStatus(orderInfo.getId(),ProcessStatus.CLOSED);
        // 付款信息
        paymentService.closePayment(orderInfo.getId());

    }

    /**
     * 根据 orderId 将orderInfo 变为 JSON 字符串
     * @param orderId
     * @return
     */
    private String initWareOrder(String orderId) {
        // 根据 orderId 查询 orderInfo
        OrderInfo orderInfo = getOrderInfo(orderId);
        // 将 orderInfo 中有用的信息保存到 map
        Map map = initWareOrder(orderInfo);
        // 将 map 装换成 JSON
        return JSON.toJSONString(map);
    }

    // 设置初始化仓库信息方法
    @Override
    public Map  initWareOrder (OrderInfo orderInfo){
        Map<String,Object> map = new HashMap<>();
        // 赋值
        map.put("orderId",orderInfo.getId());
        map.put("consignee", orderInfo.getConsignee());
        map.put("consigneeTel",orderInfo.getConsigneeTel());
        map.put("orderComment",orderInfo.getOrderComment());
        map.put("orderBody", "测试");
        map.put("deliveryAddress",orderInfo.getDeliveryAddress());
        map.put("paymentWay","2");

        map.put("wareId",orderInfo.getWareId());//仓库 id

        // 组合json
        List detailList = new ArrayList();
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            Map<String,Object> detailMap = new HashMap<>();
            detailMap.put("skuId",orderDetail.getSkuId());
            detailMap.put("skuName",orderDetail.getSkuName());
            detailMap.put("skuNum",orderDetail.getSkuNum());
            detailList.add(detailMap);
        }
        map.put("details",detailList);

        return map;
    }

    @Override
    public List<OrderInfo> orderSplit(String orderId, String wareSkuMap) {
        /*
            1.  获取原始订单
            2.  将 wareSkuMap 转换成我们能操作的对象
            3.  创建新的子订单
            4.  给子订单赋值 并保存到数据库
            5.  将子订单添加到集合中
            6.  更新订单状态
         */

        List<OrderInfo> subOrderInfoList = new ArrayList<>();
        OrderInfo orderInfoOrigin = getOrderInfo(orderId);

        // [{"wareId":"1","skuIds":["2","10"]},{"wareId":"2","skuIds":["3"]}]
        List<Map> maps = JSON.parseArray(wareSkuMap, Map.class);
        if(maps != null){
            for (Map map : maps) {
                // 获取仓库 id
                String wareId = (String) map.get("wareId");
                // 获取商品 id
                List<String> skuIds = (List<String>) map.get("skuIds");

                OrderInfo subOrderInfo = new OrderInfo();
                // 属性拷贝
                BeanUtils.copyProperties(orderInfoOrigin, subOrderInfo);
                // id必须设为 空
                subOrderInfo.setId(null);
                subOrderInfo.setWareId(wareId);
                subOrderInfo.setParentOrderId(orderId);

                // 声明一个新的子订单集合
                ArrayList<OrderDetail> subDetailArrayList = new ArrayList<>();
                // 价格：获取到原始订单的明细
                List<OrderDetail> orderDetailList = orderInfoOrigin.getOrderDetailList();
                for (OrderDetail orderDetail : orderDetailList) {
                    for (String skuId : skuIds) {
                        if(skuId.equals(orderDetail.getSkuId())){
                            orderDetail.setId(null);
                            subDetailArrayList.add(orderDetail);
                        }
                    }
                }
                // 将新的子订单集合放到子订单中
                subOrderInfo.setOrderDetailList(subDetailArrayList);

                // 计算价格
                subOrderInfo.sumTotalAmount();

                // 保存到数据库
                saveOrder(subOrderInfo);

                // 将子订单添加到集合
                subOrderInfoList.add(subOrderInfo);
            }
            updateOrderStatus(orderId,ProcessStatus.SPLIT);

            return subOrderInfoList;
        }


        return null;
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

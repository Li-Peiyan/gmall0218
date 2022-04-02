package com.atguigu.gmall0218.order.task;

import com.atguigu.gmall0218.bean.OrderInfo;
import com.atguigu.gmall0218.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@EnableScheduling
@Component
public class OrderTask {

//    // cron 表示任务启动的规则
//    // 5 每分钟的第五秒
//    @Scheduled(cron = "5 * * * * ?")
//    public void  work(){
//        System.out.println(Thread.currentThread().getName()+ "================001=============== " );
//    }
//
//    // 0/5 没隔五秒执行一次
//    @Scheduled(cron = "0/5 * * * * ?")
//    public void  work1(){
//        System.out.println(Thread.currentThread().getName()+ "================002=============== ");
//    }

    @Autowired
    private OrderService orderService;

    // 轮询处理过期订单
    @Scheduled(cron = "* 0/1 * * * ?")
    public  void checkOrder(){
        System.out.println("开始处理过期订单");
        long starttime = System.currentTimeMillis(); // 开始时间

        List<OrderInfo> expiredOrderList = orderService.getExpiredOrderList();
        for (OrderInfo orderInfo : expiredOrderList) {
            // 处理未完成订单
            orderService.execExpiredOrder(orderInfo);
        }

        long costtime = System.currentTimeMillis() - starttime; // 结束时间 - 开始时间

        System.out.println("一共处理"+expiredOrderList.size()+"个订单 共消耗"+costtime+"毫秒");
    }


}

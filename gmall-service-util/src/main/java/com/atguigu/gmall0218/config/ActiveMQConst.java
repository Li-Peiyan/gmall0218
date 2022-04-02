package com.atguigu.gmall0218.config;

import lombok.Data;

@Data
public class ActiveMQConst {
    private String brokerURL =  "tcp://81.69.33.96:61616";
    private String listenerEnable = "true";
}

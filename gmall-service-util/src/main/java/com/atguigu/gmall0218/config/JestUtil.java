package com.atguigu.gmall0218.config;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

public class JestUtil {
    public static JestClient getJestClient(){
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
                .Builder("http://81.69.33.96:9200")
                .multiThreaded(true)
                .build());
        return  factory.getObject();
    }

}

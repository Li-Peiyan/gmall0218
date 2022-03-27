package com.atguigu.gmall0218.bean;

import lombok.Data;

import java.io.Serializable;

@Data
public class SkuLsParams implements Serializable {
    // keyword = skuName
    String  keyword;

    String catalog3Id;

    String[] valueId;

    int pageNo=1;

    int pageSize=20;

}

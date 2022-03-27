package com.atguigu.gmall0218.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsResult implements Serializable {
    //展示商品
    List<SkuLsInfo> skuLsInfoList;

    long total;

    long totalPages;

    //显示 平台属性 和 平台属性值 名称，
    List<String> attrValueIdList;
}

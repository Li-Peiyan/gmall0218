package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.SkuLsInfo;
import com.atguigu.gmall0218.bean.SkuLsParams;
import com.atguigu.gmall0218.bean.SkuLsResult;

public interface ListService {
    /**
     * 保存数据到es中
     * @param skuLsInfo
     */
    void saveSkuLsInfo(SkuLsInfo skuLsInfo);

    /**
     * 检索数据
     * @param skuLsParams
     * @return
     */
    SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 记录每个商品被访问的次数
     * @param skuId
     */
    void incrHotScore(String skuId);
}

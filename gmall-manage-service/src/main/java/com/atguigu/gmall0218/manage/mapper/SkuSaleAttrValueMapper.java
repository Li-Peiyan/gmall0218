package com.atguigu.gmall0218.manage.mapper;

import com.atguigu.gmall0218.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    //根据spuId去查询销售属性值集合
    List<SkuSaleAttrValue> selectSkuSaleAttrValueListBySpu(String spuId);
}

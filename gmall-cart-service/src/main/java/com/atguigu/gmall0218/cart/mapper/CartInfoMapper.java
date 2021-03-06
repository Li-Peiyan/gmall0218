package com.atguigu.gmall0218.cart.mapper;

import com.atguigu.gmall0218.bean.CartInfo;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface CartInfoMapper extends Mapper<CartInfo> {
    /**
     * 根据用户 Id 查询实时价格 到 cartInfo 中
     * @param userId
     * @return
     */
    List<CartInfo> selectCartListWithCurPrice(String userId);
}

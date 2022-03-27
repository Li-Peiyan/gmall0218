package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.CartInfo;

import java.util.List;

public interface CartService {
    /**
     * 登陆状态添加购物车
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public  void  addToCart(String skuId, String userId, Integer skuNum);

    /**
     * 登录状态根据用户Id 查询购物车数据
     * @param userId
     * @return
     */
    List<CartInfo> getCartList(String userId);

    /**
     * 合并购物车
     * @param cartListCK
     * @param userId
     * @return
     */
    List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId);

    /**
     * 修改商品状态，选择 未选择
     * @param skuId
     * @param isChecked
     * @param userId
     */
    void checkCart(String skuId, String isChecked, String userId);

    /**
     * 根据 userId 查询被勾选的购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据 用户 id 查询购物车,匹配实时价格
     * @param userId
     * @return
     */
    public List<CartInfo> loadCartCache(String userId);
}

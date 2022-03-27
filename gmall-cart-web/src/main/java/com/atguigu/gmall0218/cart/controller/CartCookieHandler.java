package com.atguigu.gmall0218.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.CartInfo;
import com.atguigu.gmall0218.bean.SkuInfo;
import com.atguigu.gmall0218.config.CookieUtil;
import com.atguigu.gmall0218.service.ManageService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartCookieHandler {

    // 定义购物车名称
    private String cookieCartName = "CART";

    // 设置cookie 过期时间
    private int COOKIE_CART_MAXAGE=7*24*3600;

    @Reference
    private ManageService manageService;

    /**
     * 未登录状态添加购物车
     * @param request
     * @param response
     * @param skuId
     * @param userId
     * @param skuNum
     */
    public void addToCart(HttpServletRequest request, HttpServletResponse response, String skuId, String userId, int skuNum) {
        /*
            1. 查看购物车中是否有商品
            2. true：数量相加
            3. false： 直接添加
         */
        //从 cookie 中获取购物车数据 //有中文要编码
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);
        //存 cookie 中的 CartInfo
        List<CartInfo> cartInfoList = new ArrayList<>();

        //如果没有，直接添加，通过一个 boolean 判断
        boolean ifExist=false;

        if(StringUtils.isNotEmpty(cookieValue)){
            //该字符串多个 cartInfo 实体类 //string 转 CartInfo
            cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            //判断是否有该商品
            for (CartInfo cartInfo : cartInfoList) {
                //比较商品的 Id
                if(cartInfo.getSkuId().equals(skuId)){
                    //有商品
                    cartInfo.setSkuNum(cartInfo.getSkuNum() + skuNum);
                    //实时价格初始化
                    cartInfo.setSkuPrice(cartInfo.getCartPrice());

                    ifExist = true;
                    break;
                }
            }
        }
        //购物车中没有该商品
        if(!ifExist){
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            //属性赋值
            CartInfo cartInfo = new CartInfo();
            cartInfo.setSkuId(skuId);
            cartInfo.setCartPrice(skuInfo.getPrice());
            cartInfo.setSkuPrice(skuInfo.getPrice());
            cartInfo.setSkuName(skuInfo.getSkuName());
            cartInfo.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo.setUserId(userId);
            cartInfo.setSkuNum(skuNum);

            cartInfoList.add(cartInfo);
        }
        //写入 cookie
        String newCartJson = JSON.toJSONString(cartInfoList);
        CookieUtil.setCookie(request, response,cookieCartName, newCartJson, COOKIE_CART_MAXAGE, true);

    }


    /**
     * 未登录状态查询购物车列表
     * @param request
     * @return
     */
    public List<CartInfo> getCartList(HttpServletRequest request) {
        String cookieValue = CookieUtil.getCookieValue(request, cookieCartName, true);

        if(StringUtils.isNotEmpty(cookieValue)) {
            List<CartInfo> cartInfoList = JSON.parseArray(cookieValue, CartInfo.class);
            return  cartInfoList;
        }
        return null;
    }

    /**
     * 删除cookie 中的数据
     * @param request
     * @param response
     */
    public void deleteCartCookie(HttpServletRequest request, HttpServletResponse response) {
        CookieUtil.deleteCookie(request,response,cookieCartName);
    }

    public void checkCart(HttpServletRequest request, HttpServletResponse response, String skuId, String isChecked) {
        List<CartInfo> cartList = getCartList(request);
        if(cartList != null && cartList.size() > 0){
            for (CartInfo cartInfo : cartList) {
                if(cartInfo.getSkuId().equals(skuId)){
                    cartInfo.setIsChecked(isChecked);
                    break;
                }
            }
        }
        //写回 cookie
        CookieUtil.setCookie(request, response, cookieCartName, JSON.toJSONString(cartList), COOKIE_CART_MAXAGE, true);
    }
}

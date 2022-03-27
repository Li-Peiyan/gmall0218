package com.atguigu.gmall0218.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.CartInfo;
import com.atguigu.gmall0218.bean.SkuInfo;
import com.atguigu.gmall0218.config.LoginRequire;
import com.atguigu.gmall0218.service.CartService;
import com.atguigu.gmall0218.service.ManageService;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CartController {

    @Reference
    private CartService cartService;

    @Autowired
    private CartCookieHandler cartCookieHandler;

    @Reference
    private ManageService manageService;


    //区分用户登录，只需看 userId
    @RequestMapping("addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        //获取商品的数量
        String skuNum = request.getParameter("skuNum");
        String skuId = request.getParameter("skuId");
        //获取登录状态
        String userId = (String) request.getAttribute("userId");
        if(userId != null){
            //登录状态调用添加购物车
            cartService.addToCart(skuId, userId, Integer.parseInt(skuNum));

        }else{
            //未登录状态调用添加购物车
            //放到 cookie 中
            cartCookieHandler.addToCart(request, response, skuId, userId, Integer.parseInt(skuNum));
        }

        //根据 skuId 查询 skuInfo
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("skuNum", skuNum);

        return "success";
    }

    @RequestMapping("cartList")
    @LoginRequire(autoRedirect = false)
    public  String cartList(HttpServletRequest request, HttpServletResponse response){
        List<CartInfo> cartInfoList =new ArrayList<>();

        //获取登录状态
        String userId = (String) request.getAttribute("userId");
        if(userId != null){
            //合并购物车
            // 从cookie中查找购物车
            List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
            if (cartListCK!=null && cartListCK.size()>0){
                // 开始合并
                cartInfoList = cartService.mergeToCartList(cartListCK, userId);
                // 删除cookie中的购物车数据
                cartCookieHandler.deleteCartCookie(request, response);
            }else{
                //登录调用查看购物车
                cartInfoList = cartService.getCartList(userId);
            }
        }else{
            //未登录状态调用查看购物车
            //放到 cookie 中
            cartInfoList = cartCookieHandler.getCartList(request);

        }
        //保存购物车集合
        request.setAttribute("cartInfoList", cartInfoList);

        return "cartList";
    }

    @RequestMapping("checkCart")
    @LoginRequire(autoRedirect = false)
    @ResponseBody
    public void checkCart(HttpServletRequest request, HttpServletResponse response){
        // 获取页面传过来的数据
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        String userId = (String) request.getAttribute("userId");
        if(userId != null){
            cartService.checkCart(skuId, isChecked, userId);
        }else{
            cartCookieHandler.checkCart(request, response,skuId,isChecked);
        }
    }

    @RequestMapping("toTrade")
    @LoginRequire()
    public String toTrade(HttpServletRequest request,HttpServletResponse response){
        //合并勾选的商品 未登录+登录
        List<CartInfo> cartListCK = cartCookieHandler.getCartList(request);
        String userId = (String) request.getAttribute("userId");
        if(cartListCK != null && cartListCK.size() > 0){
            //合并
            cartService.mergeToCartList(cartListCK, userId);
            //删除未登录cookie 中购物车数据
            cartCookieHandler.deleteCartCookie(request, response);
        }


        return "redirect://order.gmall.com/trade";
    }

}

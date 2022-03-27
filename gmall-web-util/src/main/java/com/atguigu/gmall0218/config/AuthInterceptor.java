package com.atguigu.gmall0218.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.util.HttpClientUtil;
import io.jsonwebtoken.impl.Base64UrlCodec;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Handler;

@Component
public class AuthInterceptor extends HandlerInterceptorAdapter {
    //多个拦截器的执行顺序
    //跟配置文件中，配置拦截器的顺序有关

    // 用户进入控制器之前！
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //如何获取到 token
        //用户再登入完成后会返回一个 url
        String token = request.getParameter("newToken");
        //将 token 放入 cookie 中
        //当 token 不为 null 时
        if(token != null) {
            CookieUtil.setCookie(request, response, "token", token, WebConst.COOKIE_MAXAGE, false);
        }

        // 当用户访问非登录之后的页面，登录之后，继续访问其他业务模块时，url 并没有 newtoken，但后台可能将 token 放入了 cookie 中
        if(token == null){
            token = CookieUtil.getCookieValue(request, "token", false);
        }

        //从 cookie 中获取 token， 解密 token ！
        if(token != null){
            //开始解密，获取 nickName
            Map map = getUserMapByToken(token);
            //取出用户名称
            String nickName = (String) map.get("nickName");
            //保存到作用域
            request.setAttribute("nickName", nickName);
        }


        //在拦截器获取方法的注解！
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //获取方法中的注解 LoginRequire
        LoginRequire methodAnnotation = handlerMethod.getMethodAnnotation(LoginRequire.class);
        if(methodAnnotation != null){
            //此时有注解
            //判断用户是否登录？调用 verify
            String salt = request.getHeader("X-forwarded-for");
            //调用 verify() 认证
            String result = HttpClientUtil.doGet(WebConst.VERIFY_ADDRESS + "?token=" + token + "&salt=" + salt);
            if("success".equals(result)){
                //登录，认证成功
                //保存下 userId
                Map map = getUserMapByToken(token);
                //取出用户名称
                String userId = (String) map.get("userId");
                //保存到作用域
                request.setAttribute("userId", userId);
                return true;
            }else{
                //认证失败！并且methodAnnotation.autoRedirect() = true
                if(methodAnnotation.autoRedirect()){
                    //必须登录！跳转到页面
                    //先保留原页面地址，获取 url
                    String requestURL = request.getRequestURL().toString();
                    System.out.println("requestURL:"+requestURL);

                    //将 url 进行转换
                    String encodeURL = URLEncoder.encode(requestURL, "UTF-8");
                    System.out.println("encodeURL:"+encodeURL);
                    response.sendRedirect(WebConst.LOGIN_ADDRESS+"?originUrl="+encodeURL);
                    return false;
                }
            }
        }

        return true;
    }
    //解密 token 获取 map 数据
    private Map getUserMapByToken(String token) {
        //获取 token 中间部分
        String tokenUserInfo = StringUtils.substringBetween(token, ".");

        //将 tokenUserInfo 进行 base64 解码
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();

        //解码之后得到 byte[] 数组
        byte[] decode = base64UrlCodec.decode(tokenUserInfo);

        //byte[] 数组不能直接转成 map，需要先转成 String
        String mapJson = null;
        try {
            mapJson = new String(decode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        //转成 map 并返回
        return JSON.parseObject(mapJson, Map.class);
    }


    // 用户进入控制器之后，视图渲染之前！
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
    }
    //视图渲染之后
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
    }
}

package com.atguigu.gmall0218.passport.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.UserInfo;
import com.atguigu.gmall0218.passport.config.JwtUtil;
import com.atguigu.gmall0218.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PassportController {

    @Reference
    private UserService userService;

    @Value("${token.key}")
    private String key;

    @RequestMapping("index")
    public String index(HttpServletRequest request){
        //获取originUrl
        String originUrl = request.getParameter("originUrl");
        //保存originUrl
        request.setAttribute("originUrl", originUrl);
        return "index";
    }

    @RequestMapping("login")
    @ResponseBody
    public String login(UserInfo userInfo, HttpServletRequest request){
        //服务器 ip 地址
        String salt = request.getHeader("X-forwarded-for");

        //调用登陆方法
        UserInfo info =  userService.login(userInfo);

        if(info != null){
            //生成 Token
            HashMap<String, Object> map = new HashMap<>();
            map.put("userId", info.getId());
            map.put("nickName", info.getNickName());
            String token = JwtUtil.encode(key, map, salt);
            return token;
        }else{
            return "fail";
        }
    }

    /**
     * 登录认证
     * @param request
     * @return
     */
    @RequestMapping("verify")
    @ResponseBody
    public String verify(HttpServletRequest request) {
//        1.  获取服务器的 Ip，Token
//        2.  key+ip， 解密 token 得到用户信息 userId，nickName
//        3.  判断用户是否登录：缓存中找 key=user:userId:info  value=userInfo
//        4.  userInfo != null
//        String salt = request.getHeader("X-forwarded-for");
        String token = request.getParameter("token");
        String salt = request.getParameter("salt");

        //调用 jwt 工具类
        Map<String, Object> map = JwtUtil.decode(token, key, salt);
        if(map != null && map.size() > 0){
            //获取 userId
            String userId = (String) map.get("userId");
            //调用服务层查询用户是否已经登录
            UserInfo userInfo = userService.verify(userId);
            if(userInfo != null){
                return "success";
            }else{
                return "fail";
            }
        }
        return "fail";

    }
}

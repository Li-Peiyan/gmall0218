package com.atguigu.gmall0218.user.controller;

import com.atguigu.gmall0218.bean.UserInfo;
import com.atguigu.gmall0218.service.UserService;
import com.atguigu.gmall0218.user.service.impl.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping("findAll")
    public List<UserInfo> findAll(){
        return  userService.findAll();
    }
}

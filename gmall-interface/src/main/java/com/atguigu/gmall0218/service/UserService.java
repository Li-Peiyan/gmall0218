package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.UserAddress;
import com.atguigu.gmall0218.bean.UserInfo;

import java.util.List;

public interface UserService {
    /**
     * 查询所有数据
     * @return
     */
    List<UserInfo> findAll();

    /**
     * 根据userId查询用户地址
     * @param userId
     * @return
     */
    public List<UserAddress> getUserAddressList(String userId);

    /**
     * 登录方法
     * @param userInfo
     * @return
     */
    UserInfo login(UserInfo userInfo);

    /**
     * 查询用户是否已经登录
     * @param userId
     * @return
     */
    UserInfo verify(String userId);
}

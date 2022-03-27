package com.atguigu.gmall0218.user.service.impl;


import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.UserAddress;
import com.atguigu.gmall0218.bean.UserInfo;
import com.atguigu.gmall0218.config.JestUtil;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.service.UserService;
import com.atguigu.gmall0218.user.mapper.UserAddressMapper;
import com.atguigu.gmall0218.user.mapper.UserInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private RedisUtil redisUtil;

    public String userKey_prefix="user:";
    public String userinfoKey_suffix=":info";
    public int userKey_timeOut=60*60*24;


    @Override
    public List<UserInfo> findAll() {
        return userInfoMapper.selectAll();
    }

    @Override
    public List<UserAddress> getUserAddressList(String userId) {
        UserAddress userAddress = new UserAddress();
        userAddress.setUserId(userId);
        return userAddressMapper.select(userAddress);
    }

    @Override
    public UserInfo login(UserInfo userInfo) {
        //密码需要进行加密
        String passwd = userInfo.getPasswd();
        //对密码进行加密
        String newPwd = DigestUtils.md5DigestAsHex(passwd.getBytes());
        //赋值
        userInfo.setPasswd(newPwd);
        //根据 id 和 加密后的密码查询
        UserInfo info = userInfoMapper.selectOne(userInfo);

        if(info != null){
            Jedis jedis = redisUtil.getJedis();
            String userKey = userKey_prefix + info.getId() + userinfoKey_suffix;
            //存储成 String 类型
            jedis.setex(userKey, userKey_timeOut, JSON.toJSONString(info));

            jedis.close();
            return info;
        }

        return null;
    }

    @Override
    public UserInfo verify(String userId) {
        Jedis jedis = null;
        try {
            //获取 jedis
            jedis = redisUtil.getJedis();
            //定义 key
            String userKey = userKey_prefix + userId + userinfoKey_suffix;

            String userJson = jedis.get(userKey);
            if(userJson != null && userJson.length() > 0){

                UserInfo userInfo = JSON.parseObject(userJson, UserInfo.class);
                return  userInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }

        return null;
    }


}

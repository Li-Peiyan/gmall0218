package com.atguigu.gmall0218.passport;

import com.atguigu.gmall0218.passport.config.JwtUtil;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest
class GmallPassportWebApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void test01(){
        String key = "atguigu";
        String ip="81.69.33.96";
        Map map = new HashMap();
        map.put("userId","1001");
        map.put("nickName","marry");
        String token = JwtUtil.encode(key, map, ip);

        System.out.println("token=" + token);
        //token=eyJhbGciOiJIUzI1NiJ9.eyJuaWNrTmFtZSI6Im1hcnJ5IiwidXNlcklkIjoiMTAwMSJ9.LllQM3n3U2bp-o7U0MMGbmaVCYMYokWfAhOqeRj97Ko

        Map<String, Object> maps = JwtUtil.decode(token, key, "81.69.33.96");

        System.out.println(maps);
        //{nickName=marry, userId=1001}
    }

}

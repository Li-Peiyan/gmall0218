package com.atguigu.gmall0218.bean;

import lombok.Data;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.io.Serializable;

@Repository
@Data
public class UserInfo implements Serializable {
    //通用mapper注解
    @Id//主键
    @GeneratedValue(strategy = GenerationType.IDENTITY)//获取数据库主键自增
    private String id;
    @Column
    private String loginName;
    @Column
    private String nickName;
    @Column
    private String passwd;
    @Column
    private String name;
    @Column
    private String phoneNum;
    @Column
    private String email;
    @Column
    private String headImg;
    @Column
    private String userLevel;

}

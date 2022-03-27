package com.atguigu.gmall0218.manage.mapper;

import com.atguigu.gmall0218.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    /**
     * 根据三级分类Id查询平台属性集合
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getBaseAttrInfoListByCatalog3Id(String catalog3Id);

    /**
     * 根据平台属性值 id 查找 平台属性 平台属性值
     * @param valueIds
     * @return
     */
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIds);
}

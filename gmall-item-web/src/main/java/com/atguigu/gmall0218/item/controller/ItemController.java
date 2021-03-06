package com.atguigu.gmall0218.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.SkuImage;
import com.atguigu.gmall0218.bean.SkuInfo;
import com.atguigu.gmall0218.bean.SkuSaleAttrValue;
import com.atguigu.gmall0218.bean.SpuSaleAttr;
import com.atguigu.gmall0218.config.LoginRequire;
import com.atguigu.gmall0218.service.ListService;
import com.atguigu.gmall0218.service.ManageService;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {
    @Reference
    private ManageService manageService;

    @Reference
    private ListService listService;

    //测试：localhost:8084/33.html
    //控制器
    @RequestMapping("{skuId}.html")
    //@LoginRequire
    public String item(@PathVariable String skuId, HttpServletRequest request){

        //根据skuId 获取数据 SkuInfo + SkuImage
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);

        //查询销售属性，销售属性值// 存储 spu，sku数据
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);

        //获取的销售属性值Id 集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        String key = "";
        HashMap<String, Object> map = new HashMap<>();
        //遍历拼接字符串
        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
            // key>0 时需要拼上 "|"
            if(key.length() > 0){
                key += "|";
            }
            key += skuSaleAttrValue.getSaleAttrValueId();
            //不存在销售属性值 或 当本次 skuId 与下次 skuId 不同时停止拼接
            if(i+1 == skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i + 1).getSkuId()) ){
                //停止拼接放入 map 集合
                map.put(key, skuSaleAttrValue.getSkuId());
                // 并清空 key
                key = "";
            }
        }
        //将 map 转换成 json 字符串
        String valueSkuJson = JSON.toJSONString(map);

        //保存 Json
        request.setAttribute("valueSkuJson", valueSkuJson);

        //保存到作用域
        request.setAttribute("skuInfo", skuInfo);
        request.setAttribute("spuSaleAttrList", spuSaleAttrList);

        //更新热度
        listService.incrHotScore(skuId);

        return "item";
    }
}

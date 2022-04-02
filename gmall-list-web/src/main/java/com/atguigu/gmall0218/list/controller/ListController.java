package com.atguigu.gmall0218.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall0218.bean.*;
import com.atguigu.gmall0218.service.ListService;
import com.atguigu.gmall0218.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("list.html")
    public String listData(SkuLsParams skuLsParams, HttpServletRequest request){
        //测试分页,设置分页大小
        skuLsParams.setPageSize(2);

        SkuLsResult skuLsResult = listService.search(skuLsParams);

        //显示商品数据
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();

        //平台属性值 查找 平台属性
        //平台属性值集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        //平台属性值 Id 查询 平台属性名称 平台属性值名称
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);

        //http://list.gmall.com/list.html?keyword=大米&valueId=13
        //编写一个方法来判断url 后面的参数
        String urlParam =  makeUrlParam(skuLsParams);

        //制作一个面包屑
        ArrayList<BaseAttrValue> baseAttrValueList = new ArrayList<>();

        //使用迭代器，将被选中的平台属性移除
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            //平台属性
            BaseAttrInfo baseAttrInfo =  iterator.next();
            //获取平台属性值集合对象
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            //循环判断
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){

                    for (String valueId : skuLsParams.getValueId()) {
                        if(valueId.equals(baseAttrValue.getId())){
                            //如果相同移除
                            iterator.remove();

                            //面包屑的组成 //baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName()
                            // 把面包屑存在 BaseAttrValue 中
                            BaseAttrValue baseAttrValueed = new BaseAttrValue();
                            //将平台属性值的名称改成面包屑
                            baseAttrValueed.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            //使用 makeUrlParam 方法，返回除掉当前面包屑后的 url
                            baseAttrValueed.setUrlParam( makeUrlParam(skuLsParams, valueId) );
                            //加入面包屑集合
                            baseAttrValueList.add(baseAttrValueed);
                        }
                    }
                }
            }
        }

    //保存到作用域
        //保存分页数据
        request.setAttribute("pageNo", skuLsParams.getPageNo());
        request.setAttribute("totalPages", skuLsResult.getTotalPages());

        //保存url参数
        request.setAttribute("urlParam", urlParam);
        //保存面包屑
        request.setAttribute("baseAttrValueList", baseAttrValueList);
        //保存一个检索关键字
        request.setAttribute("keyword",skuLsParams.getKeyword());
        //保存平台属性
        request.setAttribute("baseAttrInfoList", baseAttrInfoList);
        //保存商品集合
        request.setAttribute("skuLsInfoList", skuLsInfoList);

        return "list";
    }

    /**
     * 判断url 后面的参数
     * @param skuLsParams
     * @param excludeValueIds 点击面包屑时获取的平台属性值 id  //String... 可变长度参数 jdk1.5以后
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams, String... excludeValueIds) {
        String urlParam = "";
        //http://list.gmall.com/list.html?keyword=大米
        //根据 keyword
        if(skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0){
            urlParam += "keyword="+skuLsParams.getKeyword();
        }

        //http://list.gmall.com/list.html?keyword=大米&catalog3Id=61
        //判断三级分类 id
        if(skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0){
            if(urlParam != null && urlParam.length() > 0){
                urlParam += "&";
            }
            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }

        //http://list.gmall.com/list.html?keyword=大米&catalog3Id=61&valueId=13
        //判断平台属性 id
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0){
            for (String valueId : skuLsParams.getValueId()) {
                //判断面包屑
                if(excludeValueIds != null && excludeValueIds.length > 0) {
                    String excludeValueId = excludeValueIds[0];
                    if(valueId.equals(excludeValueId)){
                        continue;
                    }
                }
                //正常拼接
                if(urlParam != null && urlParam.length() > 0){
                    urlParam += "&";
                }
                urlParam += "valueId=" + valueId;
            }
        }

        return urlParam;
    }
}

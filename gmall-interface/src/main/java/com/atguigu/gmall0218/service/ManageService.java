package com.atguigu.gmall0218.service;

import com.atguigu.gmall0218.bean.*;

import java.util.List;

public interface ManageService {
    /**
     * 获取所有的一级分类数据
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 根据一级分类 id 查询二级分类数据
     * select * where catalog1Id = ?
     * @param catalog1Id
     * @return
     */
    List<BaseCatalog2> getCatalog2(String catalog1Id);

    /**
     * 根据二级分类id查询三级分类数据
     * select * where catalog2Id = ?
     * @param catalog2Id
     * @return
     */
    List<BaseCatalog3> getCatalog3(String catalog2Id);

    /**
     * 根据三级分类 id 查询平台属性
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrList(String catalog3Id);

    /**
     * 保存平台属性数据
     * @param baseAttrInfo
     */
    void  saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 根据平台属性Id查询平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(String attrId);
//    /**
//     * 根据平台属性 Id 查询平台属性值集合
//     * @param attrId
//     * @return
//     */
//    List<BaseAttrValue> getAttrValueList(String attrId);

    /**
     * 根据spuInfo 对象属性获取spuInfo 集合
     * @param spuInfo
     * @return
     */
    List<SpuInfo> getSpuList(SpuInfo spuInfo);
//    /**
//     * 根据三级属性Id查询SPU
//     * @param catalog3Id
//     * @return
//     */
//    List<SpuInfo> getSpuList(String catalog3Id);

    /**
     * 获取所有的销售属性数据
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 保存 spuInfo 数据
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据 spuId 获取 spuImage 中的图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(String spuId);

    /**
     * 根据 spuId 获取销售属性集合
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(String spuId);

    /**
     * 根据 skuInfo 保存所有 sku 数据
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);

    /**
     * 根据 skuId 查询 skuInfo
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(String skuId);

    /**
     * 根据 skuId 查询 SkuImage图片集合
     * @param skuId
     * @return
     */
    List<SkuImage> getSkuImageBySkuId(String skuId);

    /**
     * 根据skuId,spuId 查询销售属性集合
     * @param skuInfo
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo);

    /**
     * 根据spuId去查询销售属性值集合
     * @param spuId
     * @return
     */
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

    /**
     * 根据平台属性值 id 集合查询 平台属性名 平台属性值名
     * @param attrValueIdList
     * @return
     */
    List<BaseAttrInfo> getAttrList(List<String> attrValueIdList);
}

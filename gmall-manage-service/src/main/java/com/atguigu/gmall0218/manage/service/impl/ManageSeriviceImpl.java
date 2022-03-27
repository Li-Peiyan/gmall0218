package com.atguigu.gmall0218.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.*;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.manage.constant.ManageConst;
import com.atguigu.gmall0218.manage.mapper.*;
import com.atguigu.gmall0218.service.ManageService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class ManageSeriviceImpl implements ManageService {

    //调用mapper
    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;

    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;

    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;

    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;

    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;

    @Autowired
    private  SpuInfoMapper spuInfoMapper;

    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;

    @Autowired
    private SpuImageMapper spuImageMapper;

    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;

    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;

    @Autowired
    private  SkuInfoMapper skuInfoMapper;

    @Autowired
    private SkuImageMapper skuImageMapper;

    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    private RedisUtil redisUtil;


    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        //单表
//        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
//        baseAttrInfo.setCatalog3Id(catalog3Id);
//        return baseAttrInfoMapper.select(baseAttrInfo);
        //多表关联查询
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Transactional
    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        //主键不为空说明是对当前属性进行修改，反之是增加
        if(baseAttrInfo.getId() != null || baseAttrInfo.getId().length() > 0){
            //修改操作
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        }else{
            //保存数据 baseAttrInfo
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        //清空 BaseAttrValue 数据
        BaseAttrValue baseAttrValue1 = new BaseAttrValue();
        baseAttrValue1.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue1);

        //baseAttrValue
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if(attrValueList != null && attrValueList.size() > 0){
            //循环
            for (BaseAttrValue baseAttrValue : attrValueList) {
                baseAttrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(baseAttrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        //把平台属性值集合放入平台属性中
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(attrId);
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);

        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }
    //    @Override
//    public List<BaseAttrValue> getAttrValueList(String attrId) {
//        BaseAttrValue baseAttrValue = new BaseAttrValue();
//        baseAttrValue.setAttrId(attrId);
//        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
//        return baseAttrValueList;
//        //return baseAttrValueMapper.select(baseAttrValue);
//    }

    @Override
    public List<SpuInfo> getSpuList(SpuInfo spuInfo) {
        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return spuInfoList;
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    @Transactional
    public void saveSpuInfo(SpuInfo spuInfo) {
//        保存：保存到数据涉及的表？
    //        spuInfo
    //        spuImage
    //        spuSaleAttr
    //        spuSaleAttrValue

        //保存 spuInfo
        spuInfoMapper.insertSelective(spuInfo);

        //保存图片
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(spuImageList != null && spuImageList.size() > 0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }

        //保存销售属性
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        if(spuSaleAttrList != null && spuSaleAttrList.size() > 0 ) {
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);

                //保存销售属性值
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                if (spuSaleAttrValueList != null && spuSaleAttrValueList.size() > 0) {
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }

                }

            }
        }
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        List<SpuSaleAttr> spuSaleAttrList = spuSaleAttrMapper.selectSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    @Override
    @Transactional
    public void saveSkuInfo(SkuInfo skuInfo) {
        // sku_info
        //更新或新增
        if (skuInfo.getId()==null || skuInfo.getId().length()==0){
            skuInfoMapper.insertSelective(skuInfo);
        }else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }

        //skuImage:
        //删除
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);
        //插入数据
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if(skuImageList != null && skuImageList.size() > 0){
            for (SkuImage image : skuImageList) {
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(image);
            }
        }

        //skuAttrValue:
        //删除
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);
        //插入数据
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue attrValue : skuAttrValueList) {
            attrValue.setSkuId(skuInfo.getId());
            skuAttrValueMapper.insertSelective(attrValue);
        }

        //skuSaleAttrValue:
        //删除
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);
        //插入数据
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if(skuSaleAttrValueList != null && skuSaleAttrValueList.size() > 0){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                saleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }

    @Override
    public SkuInfo getSkuInfo(String skuId) {
        //分布式锁
        Config config = new Config();
        config.useSingleServer().setAddress("redis://81.69.33.96:8090");

        RedissonClient redissonClient = Redisson.create(config);

        //使用redisson 调用 getLock
        RLock lock = redissonClient.getLock("myLock");

        lock.lock();
        //放入业务逻辑代码
        SkuInfo skuInfo =null;
        Jedis jedis = null;
        // ctrl+alt+t
        try {
            jedis = redisUtil.getJedis();
            String skuKey = ManageConst.SKUKEY_PREFIX+skuId+ManageConst.SKUKEY_SUFFIX;
            // 判断redis 中是否有key
            if (jedis.exists(skuKey)){
                // 取得key 中的value
                String skuJson = jedis.get(skuKey);
                // 将字符串转换为对象
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            }else {
                skuInfo = getSkuInfoDB(skuId);
                // 放redis 并设置过期时间
                jedis.setex(skuKey,ManageConst.SKUKEY_TIMEOUT,JSON.toJSONString(skuInfo));
                return skuInfo;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis!=null){
                jedis.close();
            }
            if(lock != null) {
                lock.unlock();
            }
        }
        return getSkuInfoDB(skuId);

        //return getSkuInfoJedis(skuId);
    }

    private SkuInfo getSkuInfoJedis(String skuId) {
        SkuInfo skuInfo = null;
        Jedis jedis = null;
        try {
            //获取jedis
            jedis = redisUtil.getJedis();
            //定义 Key
            String skuKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
            String skuJson = jedis.get(skuKey);

            //判断缓存中是否有数据
            if(skuJson == null || skuJson.length() == 0 ){
                //加锁
                System.out.println("缓存中没有数据");
                //执行 set 命令
                //定义上锁的 key  sku:skuId:lock， 作为锁
                String skuLockKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKULOCK_SUFFIX;
                String lockKey = jedis.set(skuLockKey, "LOCK", "NX", "PX", ManageConst.SKULOCK_EXPIRE_PX);

                if("OK".equals(lockKey)){
                    //此时加锁成功
                    //从数据库获取
                    skuInfo = getSkuInfoDB(skuId);
                    //放入 redis，并设置过期时间
                    jedis.setex(skuKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));
                    jedis.del(skuLockKey);
                    return skuInfo;
                }else{
                    //等待
                    Thread.sleep(1000);
                    return getSkuInfo(skuId);
                }
            }else{
                //将字符串转换成对象
                skuInfo = JSON.parseObject(skuJson, SkuInfo.class);
                return skuInfo;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(jedis != null){
                jedis.close();
            }
        }
        return getSkuInfoDB(skuId);
    }

    public SkuInfo getSkuInfoDB(String skuId){
        //直接将 skuImageList 放入 skuInfo 中
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        skuInfo.setSkuImageList(getSkuImageBySkuId(skuId));
        //放入平台属性值集合
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuId);
        skuInfo.setSkuAttrValueList( skuAttrValueMapper.select(skuAttrValue) );

        return skuInfo;
    }

    @Override
    public List<SkuImage> getSkuImageBySkuId(String skuId) {
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuId);
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);
        return skuImageList;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        //根据spuId去查询销售属性值集合
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
        return skuSaleAttrValueList;
    }

    @Override
    public List<BaseAttrInfo> getAttrList(List<String> attrValueIdList) {
        //SELECT * FROM base_attr_info bai INNER JOIN base_attr_value bav ON bai.id=bav.attr_id WHERE bav.id IN (81,82,83)
        // 81,82,83 可以看作一个字符串
        // 将集合编成字符串
        String valueIds = StringUtils.join(attrValueIdList.toArray(), ",");
        System.out.println(valueIds);
        List<BaseAttrInfo> baseAttrInfoList = baseAttrInfoMapper.selectAttrInfoListByIds(valueIds);
        return baseAttrInfoList;

    }

}

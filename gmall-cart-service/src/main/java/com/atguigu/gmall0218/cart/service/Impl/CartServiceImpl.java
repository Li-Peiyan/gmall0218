package com.atguigu.gmall0218.cart.service.Impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall0218.bean.CartInfo;
import com.atguigu.gmall0218.bean.SkuInfo;
import com.atguigu.gmall0218.cart.constant.CartConst;
import com.atguigu.gmall0218.cart.mapper.CartInfoMapper;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.service.CartService;
import com.atguigu.gmall0218.service.ManageService;
import javafx.scene.input.InputMethodTextRun;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    private CartInfoMapper cartInfoMapper;

    @Reference
    private ManageService manageService;

    @Autowired
    private RedisUtil redisUtil;

    //登录时添加购物车
    @Override
    public void addToCart(String skuId, String userId, Integer skuNum) {
        /*
            1. 先查询购物车是否有相同商品，如果有则数量相加。
            2. 如果没有，直接添加到数据库。
            3. 更新缓存。
        * */

        //先查询, 通过 skuId，userId
        CartInfo cartInfo = new CartInfo();
        cartInfo.setSkuId(skuId);
        cartInfo.setUserId(userId);
        CartInfo cartInfoExist = cartInfoMapper.selectOne(cartInfo);
        //有相同商品
        if(cartInfoExist != null){

            //数量相加
            cartInfoExist.setSkuNum(cartInfoExist.getSkuNum() + skuNum);

            //给 skuPrice 初始化操作
            cartInfoExist.setSkuPrice(cartInfoExist.getCartPrice());

            //更新数据
            cartInfoMapper.updateByPrimaryKeySelective(cartInfoExist);

        }else{
            //没有相同商品
            SkuInfo skuInfo = manageService.getSkuInfo(skuId);
            //属性赋值
            CartInfo cartInfo1 = new CartInfo();
            cartInfo1.setSkuId(skuId);
            cartInfo1.setCartPrice(skuInfo.getPrice());
            cartInfo1.setSkuPrice(skuInfo.getPrice());
            cartInfo1.setSkuName(skuInfo.getSkuName());
            cartInfo1.setImgUrl(skuInfo.getSkuDefaultImg());
            cartInfo1.setUserId(userId);
            cartInfo1.setSkuNum(skuNum);
            //添加到数据库
            cartInfoMapper.insertSelective(cartInfo1);

            cartInfoExist = cartInfo1;
        }

        //同步缓存
        //获取 jedis
        Jedis jedis = redisUtil.getJedis();
        //定义购物车的 key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //同步缓存  //采用 hash
        jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfoExist));
        // 更新购物车过期时间, 与用户过期时间一致
        //用户的key
        String userKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USERINFOKEY_SUFFIX;
        //获取用户的过期时间
        Long ttl = jedis.ttl(userKey);
        //设置购物车过期时间
        jedis.expire(cartKey,ttl.intValue());

        jedis.close();
    }

    @Override
    public List<CartInfo> getCartList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        //定义购物车的 key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //从缓存中获取数据
        List<String> stringList = jedis.hvals(cartKey);

        if(stringList != null && stringList.size() > 0){
            for (String cartInfoStr : stringList) {
                cartInfoList.add(JSON.parseObject(cartInfoStr,CartInfo.class));
            }
            // 查看时应该排序。按照时间（更新时间）
            //模拟按照 id 排序
            cartInfoList.sort(new Comparator<CartInfo>() {
                @Override
                public int compare(CartInfo o1, CartInfo o2) {
                    //定义比较规则
                    //compareTo
                    return o1.getId().compareTo(o2.getId());
                }
            });
        }else{
            //从数据库中获取数据
            cartInfoList = loadCartCache(userId);
            return  cartInfoList;
        }
        return cartInfoList;
    }

    @Override
    public List<CartInfo> loadCartCache(String userId) {
        //需要查询实时价格，从 cartInfor , skuInfo 两张表中查询
        List<CartInfo> cartInfoList = cartInfoMapper.selectCartListWithCurPrice(userId);
        if (cartInfoList==null && cartInfoList.size()==0){
            return null;
        }
        //从数据库中查到了数据，放入缓存
        Jedis jedis = redisUtil.getJedis();
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        for (CartInfo cartInfo : cartInfoList) {
            jedis.hset(cartKey,cartInfo.getSkuId(), JSON.toJSONString(cartInfo));
        }

        jedis.close();

        return cartInfoList;
    }

    @Override
    public List<CartInfo> mergeToCartList(List<CartInfo> cartListCK, String userId) {
        //获取数据库购物车数据
        List<CartInfo> cartInfoListDB = cartInfoMapper.selectCartListWithCurPrice(userId);
        //开始合并
        for (CartInfo cartInfoCK : cartListCK) {
            //判断是否有相同数据
            boolean bool = false;
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if(cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())){
                    cartInfoDB.setSkuNum(cartInfoDB.getSkuNum() + cartInfoCK.getSkuNum());
                    //修改数据库
                    cartInfoMapper.updateByPrimaryKeySelective(cartInfoDB);
                    bool = true;
                }
            }
            if(!bool){
                cartInfoCK.setUserId(userId);
                cartInfoMapper.insertSelective(cartInfoCK);
            }

        }
        // 从新在数据库中查询并返回数据
        List<CartInfo> cartInfoList = loadCartCache(userId);

        for (CartInfo cartInfoCK : cartListCK) {
            for (CartInfo cartInfoDB : cartInfoListDB) {
                if(cartInfoDB.getSkuId().equals(cartInfoCK.getSkuId())) {
                    //判断是否被选择 以 cookie 为基准
                    if ("1".equals(cartInfoCK.getIsChecked())) { // || "1".equals(cartInfoDB.getIsChecked())
                        cartInfoDB.setIsChecked("1");
                        checkCart(cartInfoDB.getSkuId(), "1", userId);
                    }
                }
            }
        }


        return cartInfoList;
    }

    @Override
    public void checkCart(String skuId, String isChecked, String userId) {
        /*
            1. 获取Jedis 客户端
            2. 获取购物车集合
            3. 直接修改 skuId 商品勾选状态 isChecked
            4. 写回购物车

            5. 新建一个购物车存储勾选的商品
        * */

        Jedis jedis = redisUtil.getJedis();
        //定义购物车的 key
        String cartKey = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CART_KEY_SUFFIX;
        //从缓存中获取数据
        String cartInfoJson = jedis.hget(cartKey, skuId);
        //转换成 cartInfo 对象
        CartInfo cartInfo = JSON.parseObject(cartInfoJson, CartInfo.class);
        cartInfo.setIsChecked(isChecked);
        //写回购物车
        jedis.hset(cartKey, skuId, JSON.toJSONString(cartInfo));

        //新建一个已选择的购物车的 key
        String cartKeyChecked = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        if("1".equals(isChecked)){
            jedis.hset(cartKeyChecked, skuId, JSON.toJSONString(cartInfo));
        }else {
            jedis.hdel(cartKeyChecked,skuId);
        }

        jedis.close();

    }

    @Override
    public List<CartInfo> getCartCheckedList(String userId) {
        List<CartInfo> cartInfoList = new ArrayList<>();

        Jedis jedis = redisUtil.getJedis();
        //已选择的购物车的 key
        String cartKeyChecked = CartConst.USER_KEY_PREFIX + userId + CartConst.USER_CHECKED_KEY_SUFFIX;

        List<String> stringList = jedis.hvals(cartKeyChecked);

        if(stringList != null && stringList.size() > 0){
            for (String cartJson : stringList) {
                cartInfoList.add( JSON.parseObject(cartJson, CartInfo.class) );
            }
        }


        jedis.close();
        return cartInfoList;
    }
}

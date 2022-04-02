package com.atguigu.gmall0218.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall0218.bean.SkuLsInfo;
import com.atguigu.gmall0218.bean.SkuLsParams;
import com.atguigu.gmall0218.bean.SkuLsResult;
import com.atguigu.gmall0218.config.JestUtil;
import com.atguigu.gmall0218.config.RedisUtil;
import com.atguigu.gmall0218.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.*;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {
    //手动注入
    private JestClient jestClient = JestUtil.getJestClient();

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public void incrHotScore(String skuId) {
        //获取Jedis
        Jedis jedis = redisUtil.getJedis();
        //定义 key
        String hotKey ="hotScore";
        //保存数据
        Double count = jedis.zincrby(hotKey, 1, "skuId:" + skuId);
        //按照一定的规则来更新 es
        if(count % 10 == 0){
            //每10次更新一次
            // es 更新热度
            updateHotScore(skuId,  Math.round(count));
        }

    }
    // 更新热度
    private void updateHotScore(String skuId, long hotScore) {
        String upd = "{\n" +
                "   \"doc\":{\n" +
                "     \"hotScore\":"+hotScore+"\n" +
                "   }\n" +
                "}";
        Update build = new Update.Builder(upd).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        try {
            jestClient.execute(build);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    /*
    1.  定义动作
    2.  执行动作
     */
    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        if(jestClient != null){
            // 保存数据
            Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
            try {
                DocumentResult documentResult = jestClient.execute(index);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /*
    1. 定义 dls 语句
    2. 定义动作
    3. 执行动作
    4. 获取结果集
     */
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //1. 动态 dsl 语句
        String query = makeQueryStringForSearch(skuLsParams);
        //2. 动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
        //3. 执行   4. 返回
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //处理返回值
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);

        return  skuLsResult;
    }

    //生产动态 dsl 语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //定义查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        //创建 bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断 keyword
        if(skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0){
            //创建 match
            MatchQueryBuilder skuName = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            //创建 must
            boolQueryBuilder.must(skuName);

            //设置高亮
            //获取高亮对象
            HighlightBuilder highlighter = searchSourceBuilder.highlighter();
            //设置高亮规则
            highlighter.field("skuName");
            highlighter.preTags("<span style='color:red'>");
            highlighter.postTags("</span>");
            //放入 search
            searchSourceBuilder.highlight(highlighter);
        }

        //判断平台属性值 Id
        if(skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            for (String valueId : skuLsParams.getValueId()) {
                //创建 term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                //创建 filter, 并添加 term
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //判断 三级分类 Id
        if(skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0){
            //创建 term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            //创建 filter, 并添加 term
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //query - bool
        searchSourceBuilder.query(boolQueryBuilder);

        //设置分页
        // from 从第几条开始
        int from = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize() ;
        searchSourceBuilder.from(from);
        // size 每页条数
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //聚合
        //创建一个对象 aggs - trems
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        //aggs 放入查询器
        searchSourceBuilder.aggregation(groupby_attr);

        String query = searchSourceBuilder.toString();

        System.out.println("query:="+query);
        return query;

    }

    /**
     * 返回值处理 成整理好的数据集
     * @param skuLsParams 用户输入
     * @param searchResult 通过 dsl 语句查询出来的结果
     * @return
     */
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        //声明对象
        SkuLsResult skuLsResult = new SkuLsResult();

//      List<SkuLsInfo> skuLsInfoList;
        List<SkuLsInfo> skuLsInfoArrayList = new ArrayList<>();
        //给集合赋值
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        //循环遍历
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;
            //获取 skuName 的高亮
            if(hit.highlight != null && hit.highlight.size() > 0){
                Map<String, List<String>> highlight = hit.highlight;
                List<String> list = highlight.get("skuName");
                //高亮的 skuName
                String skuNameHI = list.get(0);
                skuLsInfo.setSkuName(skuNameHI);
            }
            skuLsInfoArrayList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoArrayList);

//      long total;
        //获取总条数
        skuLsResult.setTotal(searchResult.getTotal());

//      long totalPages;
        //计算总页数
        long totalPages = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);

//      List<String> attrValueIdList
        //获取平台属性值 Id
        ArrayList<String> stringArrayList = new ArrayList<>();

        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        for (TermsAggregation.Entry bucket : buckets) {
            String valueId = bucket.getKey();
            stringArrayList.add(valueId);
        }
        skuLsResult.setAttrValueIdList(stringArrayList);

        return skuLsResult;
    }
}

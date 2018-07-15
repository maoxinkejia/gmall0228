package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    //常量：es中的哪个库
    public static final String ES_INDEX = "gmall";

    //常量：es中的哪个表（类）
    public static final String ES_TYPE = "SkuInfo";

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {
        //准备放入数据
        //把哪个对象的数据放进去，放到哪个库，哪个表，id是多少，建立这个库
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            //根据这个库执行，存放数据
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //调用本类方法，将参数对象中的数据，封装成dsl语言的一个字符串
        String query = makeQueryStringForSearch(skuLsParams);
        //准备执行这个字符串
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();

        //创建查询结果
        SearchResult searchResult = null;
        try {
            //执行查询，将结果返回
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //将查询结果转换成自定义类型
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);

        //将结果返回
        return skuLsResult;
    }

    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
        //从结果对象中获取数据
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        //创建一个集合来存放es对应的实体类
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        //判断结果集合
        if (hits != null && hits.size() > 0) {
            //遍历结果集合
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                //根据查询结果获取es对象
                SkuLsInfo skuLsInfo = hit.source;
                //判断，准备获取高亮
                    //通过查询结果里面的参数获取高亮信息
                    if (hit.highlight != null && hit.highlight.size() > 0) {
                        //根据高亮的名字，拿到具体的高亮集合
                        List<String> list = hit.highlight.get("skuName");
                        //根据下标索引拿到具体的高亮名称，并封装到es对象中
                        String highlight = list.get(0);
                        skuLsInfo.setSkuName(highlight);
                }

                //每循环一次就往存放es对应实体类的集合里面添加一次
                skuLsInfoList.add(skuLsInfo);
            }
        }

        //将准备好的数据分别封装到自定义结果的实体类当中
        //将es实体类的集合封装
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        //封装总条数
        skuLsResult.setTotal(searchResult.getTotal());

        //封装总页数    正常是使用一个三元表达式来判断是否是整页数，需要通过取模来判断是否加1
        //另一种算法是（总数+size-1）/ size
        long totalPages = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);

        //拿到聚合属性    根据查询结果拿到聚合对象
        MetricAggregation aggregations = searchResult.getAggregations();
        //聚合对象根据分组名称查询，拿到结果
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        //根据分组名拿到的结果获取buckets
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        //创建一个新的用来封装属性值id的集合
        List<String> valueIdList = new ArrayList<>();
        if (buckets != null && buckets.size() > 0) {
            //遍历集合
            for (TermsAggregation.Entry bucket : buckets) {
                //获取所有的key，拿到所有的值
                String valueId = bucket.getKey();
                //将拿到的值添加进list集合中
                valueIdList.add(valueId);
            }
        }
        
        //将这个封装属性值id的集合放进自定义类对应的集合中
        skuLsResult.setAttrValueIdList(valueIdList);

        return skuLsResult;
    }

    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //首先创建一个查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建bool对象
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断方法中的参数中的keyword是否有值，如果有值就用来匹配，而且高亮显示
        if (skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length() > 0) {
            //有值，创建match并添加到bool中
            //必须匹配哪个字段（skuName），值是什么（skuLsParams.getKeyword()）
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            //将封装好的match，放到must里面
            boolQueryBuilder.must(matchQueryBuilder);

            //准备高亮显示，创建高亮对象
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            //给哪个属性高亮
            highlightBuilder.field("skuName");
            //高亮的前缀后缀（样式）
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");

            //将高亮对象放到查询器中
            searchSourceBuilder.highlight(highlightBuilder);
        }

        //判断是否有过滤信息（catalog3Id）
        if (skuLsParams.getCatalog3Id() != null && skuLsParams.getCatalog3Id().length() > 0) {
            //有三级3id且不为空，创建查询过滤，过滤的字段是catalog3Id，赋值skuLsParams.getCatalog3Id()
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            //将过滤器放到bool对象中
            boolQueryBuilder.filter(termQueryBuilder);
        }

        //判断数组id进行操作
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                //遍历数组拿到每个value
                String valueId = skuLsParams.getValueId()[i];
                //创建过滤器对象，给哪个字段过滤skuAttrValueList.valueId，赋值valueId
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                //将过滤器对象放到bool对象中
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }

        //分页  根据页码和分页size计算从哪儿开始查   pageSize*（pageNo-1）
        int from = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize();
        //将计算的结果封装到查询器对象中
        searchSourceBuilder.from(from);
        //将size封装金查询器对象中
        searchSourceBuilder.size(skuLsParams.getPageSize());

        //排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);

        //聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        //整合，将bool对象放到查询器对象中
        searchSourceBuilder.query(boolQueryBuilder);
        //将查询器对象转换成字符串并返回
        String query = searchSourceBuilder.toString();
        System.out.println("query = " + query);
        return query;
    }
}

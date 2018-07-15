package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    //根据三级分类id查询属性信息集合
    List<BaseAttrInfo> getAttrInfoListByCatalog3Id(String catalog3Id);

    //根据属性值id集合查询属性信息对象集合
    List<BaseAttrInfo> selectByAttrValueIdList(@Param("list") List<String> attrValueIdList);
}

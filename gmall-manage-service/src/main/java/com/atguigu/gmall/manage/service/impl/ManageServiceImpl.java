package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.manage.constant.ManageConst;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

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
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
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
    public List<BaseAttrInfo> attrInfoList(String catalog3Id) {
        BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        return baseAttrInfoMapper.select(baseAttrInfo);
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {

        if (baseAttrInfo.getId() != null && baseAttrInfo.getId().length() > 0) {

            baseAttrInfoMapper.updateByPrimaryKey(baseAttrInfo);
        } else {
            if (baseAttrInfo.getId().length() == 0) {
                baseAttrInfo.setId(null);
            }
            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);

        if (baseAttrInfo.getAttrValueList() != null && baseAttrInfo.getAttrValueList().size() > 0) {
            for (BaseAttrValue attrValue : baseAttrInfo.getAttrValueList()) {
                if (attrValue.getId().length() == 0) {
                    attrValue.setId(null);
                }
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }
    }

    @Override
    public BaseAttrInfo getAttrInfo(String attrId) {
        //根据主键查询属性对象
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);

        BaseAttrValue baseAttrValue = new BaseAttrValue();
        //给属性值对象设置属性id
        baseAttrValue.setAttrId(baseAttrInfo.getId());

        //根据属性id查询属性值集合
        List<BaseAttrValue> attrValueList = baseAttrValueMapper.select(baseAttrValue);

        //将查询到的属性值集合赋值给属性对象中的属性值集合
        baseAttrInfo.setAttrValueList(attrValueList);

        //将封装好的属性对象返回
        return baseAttrInfo;


    }

    @Override
    public List<SpuInfo> spuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        //保存和更新写在一个方法里面，根据id是否存在
        //判断id
        if (spuInfo.getId() != null && spuInfo.getId().length() > 0) {
            //有id，说明这是一个更新操作
            spuInfoMapper.updateByPrimaryKey(spuInfo);
        } else {
            //为了避免出现id值出现一个空字符串""的现象，再判断一次id
            if (spuInfo.getId() != null && spuInfo.getId().length() == 0) {
                //能进来说明id被封装成了一个空的字符串，为了使用表格主键自增的属性，需要将id设置为null
                spuInfo.setId(null);
            }
            spuInfoMapper.insertSelective(spuInfo);
        }

        //先删除，再插入
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage);

        //从spuInfo中拿到spuImage
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        for (SpuImage image : spuImageList) {
            if (image.getId() != null && image.getId().length() == 0) {
                image.setId(null);
            }
            //因为在收集表单数据时，没有给spuImage表中的spuId赋值，所以需要在这里手动赋值
            image.setSpuId(spuInfo.getId());
            spuImageMapper.insertSelective(image);
        }

        //插入spuSaleAttr，spuSaleAttrValue
        //先删除，再插入
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        SpuSaleAttrValue spuSaleAttrValue = new SpuSaleAttrValue();
        spuSaleAttrValue.setSpuId(spuInfo.getId());
        spuSaleAttrValueMapper.delete(spuSaleAttrValue);

        //获取spu销售属性集合
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        for (SpuSaleAttr saleAttr : spuSaleAttrList) {
            //判断
            if (saleAttr.getId() != null && saleAttr.getId().length() == 0) {
                saleAttr.setId(null);
            }
            //补全数据，将spuId存进去
            saleAttr.setSpuId(spuInfo.getId());
            spuSaleAttrMapper.insertSelective(saleAttr);

            //根据spuSaleAttr对象内存的
            List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
            for (SpuSaleAttrValue saleAttrValue : spuSaleAttrValueList) {
                if (saleAttrValue.getId() != null && saleAttrValue.getId().length() == 0) {
                    saleAttrValue.setId(null);
                }
                //设置spuId
                saleAttrValue.setSpuId(spuInfo.getId());
                spuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }

    @Override
    public List<BaseAttrInfo> getSkuAttrInfoList(String catalog3Id) {
        return baseAttrInfoMapper.getAttrInfoListByCatalog3Id(catalog3Id);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.getSpuSaleAttrList(spuId);
    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public void saveSku(SkuInfo skuInfo) {
        //保存和修改一起做
        //先判断skuInfo对象是否为空
        if (skuInfo.getId() != null && skuInfo.getId().length() > 0) {
            skuInfoMapper.updateByPrimaryKey(skuInfo);
        } else {
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        }

        //先删除再添加
        //sku属性
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);
        //拿到集合
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue attrValue : skuAttrValueList) {
            //判断
            if (attrValue.getId() != null && attrValue.getId().length() == 0) {
                attrValue.setId(null);
            }
            attrValue.setSkuId(skuInfo.getId());
            skuAttrValueMapper.insertSelective(attrValue);
        }

        //sku销售属性
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
            if (saleAttrValue.getId() != null && saleAttrValue.getId().length() == 0) {
                saleAttrValue.setId(null);
            }
            saleAttrValue.setSkuId(skuInfo.getId());
            skuSaleAttrValueMapper.insertSelective(saleAttrValue);
        }

        //sku图片
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);

        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage image : skuImageList) {
            if (image.getId() != null && image.getId().length() == 0) {
                image.setId(null);
            }
            image.setSkuId(skuInfo.getId());
            skuImageMapper.insertSelective(image);
        }

    }

    @Override
    public SkuInfo getSkuInfoById(String skuId) {
        //首先从redis中查询数据
        //拿到jedis对象
        Jedis jedis = redisUtil.getJedisPool();
        SkuInfo skuInfo = null;
        //拼串，动态生成skuId对应的key
        String skuInfoKey = ManageConst.SKUKEY_PREFIX + skuId + ManageConst.SKUKEY_SUFFIX;
        //判断key是否存在
        if (jedis.exists(skuInfoKey)) {
            //存在则直接拿值
            String skuInfoJson = jedis.get(skuInfoKey);
            //判断值是否为空
            if (skuInfoJson != null && !"".equals(skuInfoJson)) {
                skuInfo = JSON.parseObject(skuInfoJson, SkuInfo.class);
            }
            return skuInfo;
        } else {
            //如果不存在key则从数据库查询
            skuInfo = getSkuInfoDB(skuId);
            //将查询结果存放到redis中一份
            jedis.setex(skuInfoKey, ManageConst.SKUKEY_TIMEOUT, JSON.toJSONString(skuInfo));

            return skuInfo;
        }
    }

    private SkuInfo getSkuInfoDB(String skuId) {
        //根据主键查询skuInfo对象
        SkuInfo skuInfo = skuInfoMapper.selectByPrimaryKey(skuId);
        //根据skuId查询对应的图片信息
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        List<SkuImage> skuImageList = skuImageMapper.select(skuImage);

        //将查询到的图片信息封装进skuInfo对象中
        skuInfo.setSkuImageList(skuImageList);

        //手动查询平台属性值集合放到商品对象中
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuInfo.setSkuAttrValueList(skuAttrValueMapper.select(skuAttrValue));

        //手动查询销售属性值集合放到商品对象中
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuInfo.setSkuSaleAttrValueList(skuSaleAttrValueMapper.select(skuSaleAttrValue));

        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> selectSpuSaleAttrListCheckBySku(SkuInfo skuInfo) {
        return spuSaleAttrMapper.selectSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
    }

    @Override
    public List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId) {
        return skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(spuId);
    }

    @Override
    public List<BaseAttrInfo> getAttrInfo(List<String> attrValueIdList) {
        return baseAttrInfoMapper.selectByAttrValueIdList(attrValueIdList);
    }

}

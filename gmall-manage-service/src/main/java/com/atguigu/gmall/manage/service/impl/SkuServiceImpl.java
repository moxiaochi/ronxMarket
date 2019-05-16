package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SkuService;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.util.List;

@Service
public class SkuServiceImpl implements SkuService {


    @Autowired
    SkuInfoMapper skuInfoMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;

    @Autowired
    SkuSaleValueMapper skuSaleValueMapper;
    @Autowired
    SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Autowired
    SkuImageMapper skuImageMapper;

    @Autowired
    RedisUtil redisUtil;

    @Override
    public List<SkuInfo> skuInfoListBySpu(String spuId) {
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSpuId(spuId);
        List<SkuInfo> select = skuInfoMapper.select(skuInfo);
        return select;
    }

    @Override
    public void saveSku(SkuInfo skuInfo) {

        // 保存sku信息
        skuInfoMapper.insertSelective(skuInfo);
        String skuId = skuInfo.getId();

        // 保存平台属性关联信息
        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        for (SkuAttrValue skuAttrValue : skuAttrValueList) {
            skuAttrValue.setSkuId(skuId);
            skuAttrValueMapper.insert(skuAttrValue);
        }

        // 保存销售属性关联信息
        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();

        for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
            skuSaleAttrValue.setSkuId(skuId);
            skuSaleAttrValueMapper.insert(skuSaleAttrValue);
        }

        // 保存sku图片信息
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        for (SkuImage skuImage : skuImageList) {
            skuImage.setSkuId(skuId);
            skuImageMapper.insert(skuImage);
        }
    }

    /**
     *从db中查询skuId
     * @param skuId
     * @return
     */
    public SkuInfo getSkuByIdFromDB(String skuId){

        //查询sku信息
        SkuInfo skuInfoParam = new SkuInfo();
        skuInfoParam.setId(skuId);
        System.out.println(skuId);
        SkuInfo skuInfo = skuInfoMapper.selectOne(skuInfoParam);

        //查询图片集合
        SkuImage skuImageParam = new SkuImage();
        skuImageParam.setSkuId(skuId);
        List<SkuImage> skuImages = skuImageMapper.select(skuImageParam);
        skuInfo.setSkuImageList(skuImages);

        return skuInfo;
    }
    /**
     * 根据id查询sku和图片集合
     * @param skuId
     * @return
     */
    @Override
    public SkuInfo getSkuById(String skuId) {

        String custom = Thread.currentThread().getName();

        System.err.println(custom+"线程进入sku查询方法");

        SkuInfo skuInfo = null;
        String skuKey = "sku:"+skuId+":info";

        // 缓存redis查询
        Jedis jedis = redisUtil.getJedis();
        String s = jedis.get(skuKey);

        if (StringUtils.isNotBlank(s)&&s.equals("emplt")){
            System.err.println(custom+"线程发现数据库中没有数据，返回");
            return null;
        }

        if (StringUtils.isNotBlank(s)&&!"empty".equals(s)){
            System.err.println(custom+"线程能够从redis中获取数据");
            skuInfo = JSON.parseObject(s, SkuInfo.class);
        }else {
            System.err.println(custom+"线程没有从redis中取出数据库，申请访问数据库的分布式锁");
            //db查询(限制db的访问量）
            String OK = jedis.set("sku:" + skuId + ":lock", "1", "nx", "px", 10000);
            if (StringUtils.isNotBlank(OK)){
                System.err.println(custom+"线程得到分布式锁 开始访问数据库");
                skuInfo = getSkuByIdFromDB(skuId);
                if (null!=skuInfo){
                    System.err.println(custom+"线程成功访问数据库，释放锁");
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    jedis.del("sku:" + skuId + ":lock");
                }
            }else {
                System.err.println(custom+"线程需要访问数据库，但是未得到分布式锁 开始自旋");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //自旋
                jedis.close();
                getSkuById(skuId);
            }

            //如果数据库为空
            if (null==skuInfo){
                System.err.println(custom+"线程访问数据库发现数据库为空，将空值同步redis");
                jedis.set(skuKey,"empty");
            }

            //同步redis
            System.err.println(custom+"线程将数据库中获取的数据同步redis");
            if (null!=skuInfo&&!"empty".equals(s)){
                jedis.set(skuKey,JSON.toJSONString(skuInfo));
            }
        }
        System.err.println(custom+"线程结束访问 返回");
        System.err.println("------------------------------------------------------------------------------");
        jedis.close();
        return skuInfo;
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String spuId,String skuId) {

        List<SpuSaleAttr> spuSaleAttrs = skuSaleAttrValueMapper.selectSpuSaleAttrListCheckBySku(Integer.parseInt(spuId),Integer.parseInt(skuId));

        return  spuSaleAttrs;
    }

    @Override
    public List<SkuInfo> getSkuSaleAttrValueListBySpu(String spuId) {

        List<SkuInfo> skuInfos = skuSaleAttrValueMapper.selectSkuSaleAttrValueListBySpu(Integer.parseInt(spuId));

        return skuInfos;
    }
}

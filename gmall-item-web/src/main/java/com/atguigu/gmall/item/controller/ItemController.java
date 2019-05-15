package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {

    @Reference
    SkuService skuService;

    @RequestMapping("demo")
    public String demo(ModelMap map){

        ArrayList<String> strings = new ArrayList<>();

        for (int i = 1; i <=5 ; i++) {
            strings.add("thymeleaf"+i);
        }

        map.put("hello","hello thymeleaf !");
        map.put("strings",strings);
        map.put("ifFlag","1");
        return "demo";
    }

    @RequestMapping("{skuId}.html")
    public String index(@PathVariable String skuId,ModelMap map){
        // 当前sku
        SkuInfo skuInfo = skuService.getSkuById(skuId);

        if (null!=skuInfo){
            map.put("skuInfo",skuInfo);
            String spuId = skuInfo.getSpuId();

            // 查询销售属性列表
            List<SpuSaleAttr> spuSaleAttrs = skuService.getSpuSaleAttrListCheckBySku(spuId,skuInfo.getId());
            map.put("spuSaleAttrListCheckBySku",spuSaleAttrs);

            // 查询sku的兄弟姐妹的hash表
            // {"215|218":"97","215|218":"98","215|218":"99","215|218":"100","215|218":"101"}
            Map<String,String> skuMap = new HashMap<String,String>();
            List<SkuInfo> skuInfos = skuService.getSkuSaleAttrValueListBySpu(spuId);
            for (SkuInfo info : skuInfos) {
                String v = info.getId();
                String k = "";
                List<SkuSaleAttrValue> skuSaleAttrValueList = info.getSkuSaleAttrValueList();
                for (SkuSaleAttrValue skuSaleAttrValue : skuSaleAttrValueList) {
                    String saleAttrValueId = skuSaleAttrValue.getSaleAttrValueId();
                    k = k + "|" +  saleAttrValueId;
                }
                skuMap.put(k,v);
            }

            // 用json工具将hashmap转化成json字符串
            String skuMapJson = JSON.toJSONString(skuMap);
            map.put("skuMapJson",skuMapJson);
            System.out.println(skuMapJson);
        }
        return "item";

    }
}

package com.wb.controller;

import com.wb.hotkey.HotSpotDetector;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;

@RestController
@RequestMapping("/index")
public class IndexController {

    private HotSpotDetector hotSpotDetector = new HotSpotDetector();

    @RequestMapping("/get/{key}")
    public Object get(@PathVariable String key) {
        String cacheKey = "skuId__" + key;
        if (hotSpotDetector.isHot(cacheKey)) {
            System.out.println("hotkey:"+ cacheKey);
            //注意是get，不是getValue。getValue会获取并上报，get是纯粹的本地获取
            Object skuInfo = hotSpotDetector.get(cacheKey);
            if (skuInfo == null) {
                Object theSkuInfo = "123" + "[" + key + "]" + key;
                hotSpotDetector.set(cacheKey, theSkuInfo);
                return theSkuInfo;
            } else {
                //使用缓存好的value即可
                return skuInfo;
            }
        } else {
            System.out.println("not hot:"+ cacheKey);
            return "123" + "[" + key + "]" + key;
        }
    }
}

package com.lhr.controller;

import com.lhr.service.StockService;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by LHR on 2018/11/28.
 */
@RestController
public class StockController {

    @Autowired
    private StockService stockService;

    @RequestMapping(value = "stock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object stock() {
        //商品id
        long productId = 1;
        //库存id
        String redisKey = "redis_key:stock:" + productId;
        Long stock = stockService.stock(redisKey, 60 * 60, 2, () -> initStock(productId));
        return stock >= 0;
    }


    @RequestMapping(value = "getStock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object getStock() {
        // 商品ID
        long commodityId = 1;
        // 库存ID
        String redisKey = "redis_key:stock:" + commodityId;

        return stockService.getStock(redisKey);
    }


    @RequestMapping(value = "addStock", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Object addStock() {
        // 商品ID
        long commodityId = 2;
        // 库存ID
        String redisKey = "redis_key:stock:" + commodityId;

        return stockService.addStock(redisKey, 60 * 60L, 2);
    }

    /**
     * 获取初始的库存
     *
     * @return
     */
    private int initStock(long commodityId) {
        // TODO 这里做一些初始化库存的操作
        return 1000;
    }

}

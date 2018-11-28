package com.lhr.service;

/**
 * 函数式接口
 * 获取库存的回调
 * Created by L HR on 2018/11/28.
 */
public interface IStockCallback {

    /**
     * 获取库存
     * @return
     */
    int getStock();
}

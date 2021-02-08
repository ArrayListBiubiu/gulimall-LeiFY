package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.ITestRedis;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
public class TestRedis extends ServiceImpl<CategoryDao, CategoryEntity> implements ITestRedis {
    @Override
    public void testRedis() {
        List<CategoryEntity> entities = baseMapper.selectList(null);
        System.out.println(entities);
    }
}

package com.atguigu.gulimall.product.service;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author lzx
 * @email 1010015306@qq.com
 * @date 2020-09-13 16:14:24
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void removeMenuByIds(List<Long> asList);

    Map<String, List<Catalog2Vo>> getCategoriesDb();


    /**
     * 找到该三级分类的完整路径
     * @param categorygId
     * @return
     */
//    Long[] findCatelogPathById(Long categorygId);
//
//    void updateCascade(CategoryEntity category);
//
//    List<CategoryEntity> getLevel1Catagories();
//
    Map<String, List<Catalog2Vo>> getCategoryMap();

    List<CategoryEntity> listFromRedis();

    List<CategoryEntity> listFromMysql();

    List<CategoryEntity> listLocal();

    List<CategoryEntity> listRedisson();
//
//    Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisLock();
//
//    Map<String, List<Catalog2Vo>> getCatalogJsonDbWithRedisson();
//
//    Map<String, List<Catalog2Vo>> getCatalogJsonDbWithSpringCache();
}


package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.vo.Catalog2Vo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.ref.Reference;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {


    @Autowired
    private CategoryBrandRelationServiceImpl categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );
        return new PageUtils(page);
    }


    /**
     * 基于 redisson 优化 listLocal() 方法，可以看到简化了很多，只需要写上加锁和解锁就可以了，底层实现交给了redisson
     */
    @Override
    public List<CategoryEntity> listRedisson() {
        // 获取一个分布式锁
        // 注：锁名一定要保证一定的粒度，越细越好，否则很多不同通能的方法都用一个名，那么彼此之间可能都没人任何关联，却还需要互相等待
        RLock lock = redissonClient.getLock("catalogJson-lock");
        lock.lock();
        List<CategoryEntity> categoryEntities = null;
        try {
            categoryEntities = listFromRedis();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return categoryEntities;
    }





    @Override
    public List<CategoryEntity> listLocal() {
        // 使用setnx指令，完成分布式锁
        String uuid = UUID.randomUUID().toString();
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 100, TimeUnit.SECONDS);
        if (lock) {
            // 1.1.加锁成功，执行业务
            List<CategoryEntity> categoryEntities = listFromRedis();
            // 1.2.获取key=lock的value值
            String lockValue = stringRedisTemplate.opsForValue().get("lock");
            // 1.3.lua脚本
            String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            // 1.4.使用lua脚本释放锁，保证（1）获取key的value值（2）释放锁，这2步是原子操作
            stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), lockValue);
            // 1.5.返回数据
            return categoryEntities;
        } else {
            // 2.1.加锁失败，再次尝试获取锁
            // 2.2.为了防止循环次数过多，中间休眠1秒钟
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return listLocal();
        }
    }

    /**
     * 结合redis缓存技术，查询三级分类数据
     */
    @Override
    public List<CategoryEntity> listFromRedis() {
        // 1.先尝试从redis中获取
        String listWithTree = stringRedisTemplate.opsForValue().get("listWithTree");

        if (StringUtils.isEmpty(listWithTree)) {
            System.out.println("缓存未命中。。。" + Thread.currentThread().getName());
            // 2.1.如果缓存中不存在，再从数据库中获取
            List<CategoryEntity> categoryEntities = listFromMysql();
            return categoryEntities;
        }
        System.out.println("缓存已命中。。。" + Thread.currentThread().getName());
        // 3.如果redis中可以直接获取，需要将json数据转换为需要的对象类型，这里转换为List
        List<CategoryEntity> result = JSON.parseObject(listWithTree, new TypeReference<List<CategoryEntity>>() {
        });
        return result;
    }

    /**
     * 查询数据库
     */
    public List<CategoryEntity> listFromMysql() {
        synchronized (this) {
            String listWithTree = stringRedisTemplate.opsForValue().get("listWithTree");
            if (!StringUtils.isEmpty(listWithTree)) {
                List<CategoryEntity> result = JSON.parseObject(listWithTree, new TypeReference<List<CategoryEntity>>() {
                });
                return result;
            }

            System.out.println("查询数据库。。。" + Thread.currentThread().getName());

            //1、查出所有分类
            List<CategoryEntity> entities = baseMapper.selectList(null);

            //2、组装成父子的树形结构
            //2.1）、找到所有的一级分类
            List<CategoryEntity> level1Menus = entities.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0).map((menu) -> {
                menu.setChildren(getChildrens(menu, entities));
                return menu;
            }).sorted((menu1, menu2) -> {
                return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
            }).collect(Collectors.toList());


            // 将放入redis的操作与，查询数据库的操作，合并为原子操作
            // 防止出现，将查询到的结果插入redis之前就释放synchronized锁，下个线程还傻傻的以为redis中没有数据呢
            String s = JSON.toJSONString(level1Menus);
            stringRedisTemplate.opsForValue().set("listWithTree", s);

            return level1Menus;
        }
    }


    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO  1、检查当前删除的菜单，是否被别的地方引用

        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //递归查找所有菜单的子菜单
    private List<CategoryEntity> getChildrens(CategoryEntity root, List<CategoryEntity> all) {

        List<CategoryEntity> children = all.stream().filter(categoryEntity -> {
            return categoryEntity.getParentCid() == root.getCatId();
        }).map(categoryEntity -> {
            //1、找到子菜单
            categoryEntity.setChildren(getChildrens(categoryEntity, all));
            return categoryEntity;
        }).sorted((menu1, menu2) -> {
            //2、菜单的排序
            return (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort());
        }).collect(Collectors.toList());

        return children;
    }


    public Map<String, List<Catalog2Vo>> getCategoryMap() {


//        //缓存改写1：使用map作为本地缓存
//        Map<String, List<Catalog2Vo>> catalogMap = (Map<String, List<Catalog2Vo>>) cache.get("catalogMap");
//        if (catalogMap == null) {
//            catalogMap = getCategoriesDb();
//            cache.put("catalogMap",catalogMap);
//        }
//        return catalogMap;

//        //缓存改写2：使用redis作为本地缓存
//        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
//        String catalogJson = ops.get("catalogJson");
//        if (StringUtils.isEmpty(catalogJson)) {
//            Map<String, List<Catalog2Vo>> categoriesDb = getCategoriesDb();
//            String toJSONString = JSON.toJSONString(categoriesDb);
//            ops.set("catalogJson",toJSONString);
//            return categoriesDb;
//        }
//        Map<String, List<Catalog2Vo>> listMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {});
//        return listMap;


        // 缓存改写3：加锁解决缓存穿透问题
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        String catalogJson = ops.get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            System.out.println("缓存不命中，`准备查询数据库。。。");
//            synchronized (this) {
//                String synCatalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
//                if (StringUtils.isEmpty(synCatalogJson)) {
            Map<String, List<Catalog2Vo>> categoriesDb = getCategoriesDb();
            String toJSONString = JSON.toJSONString(categoriesDb);
            ops.set("catalogJson", toJSONString);
            return categoriesDb;
//                }else {
//                    Map<String, List<Catalog2Vo>> listMap = JSON.parseObject(synCatalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {});
//                    return listMap;
//                }
//            }

        }
        System.out.println("缓存命中。。。。");
        Map<String, List<Catalog2Vo>> listMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catalog2Vo>>>() {
        });
        return listMap;
    }


    // 从数据库中查出三级分类
    public Map<String, List<Catalog2Vo>> getCategoriesDb() {
        System.out.println("查询了数据库");
        // 优化业务逻辑，仅查询一次数据库
        List<CategoryEntity> categoryEntities = this.list();
        // 查出所有一级分类
        List<CategoryEntity> level1Categories = getCategoryByParentCid(categoryEntities, 0L);
        Map<String, List<Catalog2Vo>> listMap = level1Categories.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            // 遍历查找出二级分类
            List<CategoryEntity> level2Categories = getCategoryByParentCid(categoryEntities, v.getCatId());
            List<Catalog2Vo> catalog2Vos = null;
            if (level2Categories != null) {
                // 封装二级分类到vo并且查出其中的三级分类
                catalog2Vos = level2Categories.stream().map(cat -> {
                    // 遍历查出三级分类并封装
                    List<CategoryEntity> level3Catagories = getCategoryByParentCid(categoryEntities, cat.getCatId());
                    List<Catalog2Vo.Catalog3Vo> catalog3Vos = null;
                    if (level3Catagories != null) {
                        catalog3Vos = level3Catagories.stream()
                                .map(level3 -> new Catalog2Vo.Catalog3Vo(level3.getParentCid().toString(), level3.getCatId().toString(), level3.getName()))
                                .collect(Collectors.toList());
                    }
                    Catalog2Vo catalog2Vo = new Catalog2Vo(v.getCatId().toString(), cat.getCatId().toString(), cat.getName(), catalog3Vos);
                    return catalog2Vo;
                }).collect(Collectors.toList());
            }
            return catalog2Vos;
        }));
        return listMap;
    }


    private List<CategoryEntity> getCategoryByParentCid(List<CategoryEntity> categoryEntities, long l) {
        List<CategoryEntity> collect = categoryEntities.stream().filter(cat -> cat.getParentCid() == l).collect(Collectors.toList());
        return collect;
    }


}
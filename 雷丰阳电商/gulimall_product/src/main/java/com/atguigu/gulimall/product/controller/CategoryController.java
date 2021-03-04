package com.atguigu.gulimall.product.controller;

import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import com.atguigu.gulimall.product.service.ITestRedis;
import com.atguigu.gulimall.product.service.impl.TestRedis;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

//import org.apache.shiro.authz.annotation.RequiresPermissions;


/**
 * 商品三级分类
 *
 * @author lzx
 * @email 1010015306@qq.com
 * @date 2020-09-13 16:14:24
 */
@RestController
@RequestMapping("product/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;


    // 查出所有分类以及子分类，以树形结构组装起来
    @RequestMapping("/list/tree")
    public R list() {
//        List<CategoryEntity> entities = categoryService.listFromMysql();
//        List<CategoryEntity> entities = categoryService.listFromRedis();
        List<CategoryEntity> entities = categoryService.listLocal();
        return R.ok().put("data", entities);
    }


    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:category:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = categoryService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    //@RequiresPermissions("product:category:info")
    public R info(@PathVariable("catId") Long catId) {
        CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("category", category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:category:save")
    public R save(@RequestBody CategoryEntity category) {
        categoryService.save(category);

        return R.ok();
    }

    @RequestMapping("/update/sort")
    //@RequiresPermissions("product:category:update")
    public R updateSort(@RequestBody CategoryEntity[] category) {
        categoryService.updateBatchById(Arrays.asList(category));
        return R.ok();
    }


    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:category:update")
    public R update(@RequestBody CategoryEntity category) {
        categoryService.updateById(category);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:category:delete")
    public R delete(@RequestBody Long[] catIds) {
//		categoryService.removeByIds(Arrays.asList(catIds));
        categoryService.removeMenuByIds(Arrays.asList(catIds));
        return R.ok();
    }


    @RequestMapping("/hello")
    public String hello() {
        RLock rLock = redissonClient.getLock("my-lock");
        // 阻塞是等待
//        rLock.lock();
        rLock.lock(15, TimeUnit.SECONDS);
        try {
            System.out.println("加锁成功。。。" + Thread.currentThread().getId());
            Thread.sleep(40000); // 15秒
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("。。。解锁成功" + Thread.currentThread().getId());
            rLock.unlock();
        }
        return "hello";
    }


    /**
     * 读锁
     */
    @RequestMapping("/read")
    public String read() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
        RLock rLock = lock.readLock();
        String s = "";
        try {
            rLock.lock();
            System.out.println("读锁加锁。。。" + Thread.currentThread().getId());
            s = (String) redisTemplate.opsForValue().get("lock-value");
        } finally {
            System.out.println("。。。读锁解锁" + Thread.currentThread().getId());
            rLock.unlock();
            return s;
        }
    }

    /**
     * 写锁
     */
    @RequestMapping("/write")
    public String write() {
        RReadWriteLock lock = redissonClient.getReadWriteLock("ReadWrite-Lock");
        RLock wLock = lock.writeLock();
        String uuid = UUID.randomUUID().toString();
        try {
            wLock.lock();
            System.out.println("写锁加锁。。。" + Thread.currentThread().getId());
            Thread.sleep(10000);
            redisTemplate.opsForValue().set("lock-value", uuid);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println("。。。写锁解锁" + Thread.currentThread().getId());
            wLock.unlock();
            return uuid;
        }
    }


    /**
     * 信号量，acquire，停车，调用一次停车位+1，（初始值为0，当是0的时候，该方法会阻塞）
     */
    @RequestMapping("/park")
    public String park() {
        RSemaphore park = redissonClient.getSemaphore("park");
        try {
            park.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "停进";
    }

    /**
     * 信号量，release，开走，调用一次停车位-1，（初始值为0）
     */
    @RequestMapping("/go")
    public String go() {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release();
        return "开走";
    }


    /**
     * 闭锁，await() 方法的调用是会阻塞，直到计数器为 0 的时候才会继续执行
     */
    @RequestMapping("/lockDoor")
    public String setLatch() {
        System.out.println(1);
        RCountDownLatch latch = redissonClient.getCountDownLatch("CountDownLatch");
        try {
            latch.trySetCount(5); // 给一个初始值为 5
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return "关门";
    }

    /**
     * 闭锁，countDown() 方法的调用一次，计数器 -1
     */
    @RequestMapping("/leave")
    public String offLatch() {
        RCountDownLatch latch = redissonClient.getCountDownLatch("CountDownLatch");
        latch.countDown(); // 调用一次 -1
        return "放学了";
    }
}

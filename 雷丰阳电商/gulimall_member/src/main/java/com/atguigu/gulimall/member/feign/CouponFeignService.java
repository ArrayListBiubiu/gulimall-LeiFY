package com.atguigu.gulimall.member.feign;


import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-coupon") // 需要被调用的服务名
public interface CouponFeignService {

    // 当调用该方法时，（1）先找到模块 gulimall_coupon ，（2）再找映射的方法 coupon/coupon/test
    @RequestMapping("coupon/coupon/test")
    R membercoupons();


}


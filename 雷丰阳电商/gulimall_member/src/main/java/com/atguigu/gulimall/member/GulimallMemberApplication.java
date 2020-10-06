package com.atguigu.gulimall.member;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


// 启动类启动的时候，发现有 @EnableFeignClients 注解
// （1）知道了开启了远程调用的功能
// （2）并且"com.atguigu.gulimall.member.feign"包下是远程调用的接口，进而再扫描这个包下的接口
@EnableFeignClients("com.atguigu.gulimall.member.feign") // 开启远程调用功能
@EnableDiscoveryClient  // 开启注册中心
@SpringBootApplication
public class GulimallMemberApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallMemberApplication.class, args);
    }

}

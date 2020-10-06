package com.atguigu.gulimall.third.party;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GulimallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    // 整合 springcloud，直接调用方法即可
    @Test
    public void testSpringCloud() throws FileNotFoundException {

        // 2.上传文件流
        InputStream inputStream = new FileInputStream("C:\\Users\\ArrayList_biubiu\\Desktop\\3.png");
        ossClient.putObject("gulimall-leifengyang", "3.png", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("完成图片的上传，测试成功");
    }


    // 使用原生 SKD 方法
    @Test
    public void test() throws FileNotFoundException {
        String endpoint = "oss-cn-beijing.aliyuncs.com";
        String accessKeyId = "LTAI4GErhLbvFqsUmv2NTayz";
        String accessKeySecret = "dIG6GN5km6CyB8NArrscECah30yZYK";

        // 1.创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        // 2.上传文件流
        InputStream inputStream = new FileInputStream("C:\\Users\\ArrayList_biubiu\\Desktop\\1.jpeg");
        ossClient.putObject("gulimall-leifengyang", "1.jpeg", inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        System.out.println("完成图片的上传，测试成功");
    }

}

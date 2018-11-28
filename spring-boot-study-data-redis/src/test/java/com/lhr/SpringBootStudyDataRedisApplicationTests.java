package com.lhr;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootStudyDataRedisApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void contextLoads() {
    }


    @Test
    public void connectTest(){
        stringRedisTemplate.opsForValue().set("test","123");
        Assert.assertEquals("123",stringRedisTemplate.opsForValue().get("test"));
    }

}

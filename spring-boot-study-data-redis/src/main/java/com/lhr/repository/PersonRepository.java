package com.lhr.repository;

import com.lhr.domain.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Created by LHR on 2018/11/28.
 */
@Repository
public class PersonRepository {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    public void stringRedisTemplateDemo() {
        stringRedisTemplate.opsForValue().set("xx", "yy");
    }

    public void save(Person person) {
        redisTemplate.opsForValue().set(person.getId(), person);
    }

    public String getString() {
        return stringRedisTemplate.opsForValue().get("xx");
    }

    public Person getPerson() {
        return (Person) redisTemplate.opsForValue().get("1");
    }
}

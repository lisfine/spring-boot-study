package com.lhr.controller;

import com.lhr.domain.Person;
import com.lhr.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by LHR on 2018/11/28.
 */
@RestController
public class DateController {

    @Autowired
    PersonRepository personRepository;

    @RequestMapping(value = "set",produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public void set(){
        Person person = new Person("1", "lhr", 22);
        personRepository.save(person);

        personRepository.stringRedisTemplateDemo();
    }

    @RequestMapping(value = "getStr", produces = MediaType.APPLICATION_JSON_UTF8_VALUE) //2
    public String getStr(){
        return personRepository.getString();
    }

    @RequestMapping(value = "getPerson", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Person getPerson(){
        return personRepository.getPerson();
    }
}

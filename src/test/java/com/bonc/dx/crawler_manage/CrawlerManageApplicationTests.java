package com.bonc.dx.crawler_manage;

import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

@SpringBootTest
class CrawlerManageApplicationTests {
    @Autowired
    CommonUtil commonUtil;

    @Test
    void contextLoads() {

        Map<String,String> days =commonUtil.getDays("AA.class");
        System.out.println(days.get("start"));
        System.out.println(days.get("end"));
    }

}

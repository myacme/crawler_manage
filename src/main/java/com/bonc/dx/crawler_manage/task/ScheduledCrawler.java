package com.bonc.dx.crawler_manage.task;

import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ScheduledCrawler {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Scheduled(cron = "0 12 15  * * ?")
    public void execute() {
        System.out.println("====================定时启动=================");
        ApplicationContext applicationContext = SpringUtil.getApplicationContext();
        Map<String, Crawler> crawlers = applicationContext.getBeansOfType(Crawler.class);
        for(Map.Entry entry : crawlers.entrySet()){
            System.out.println(entry.getKey() + ":" + entry.getValue());
            //需要测试某一个就放开if条件 匹配类的bean
            if(entry.getKey().equals("htgsCcgpCrawler")){
                crawlers.get(entry.getKey()).run();
            }
        }
    }

}

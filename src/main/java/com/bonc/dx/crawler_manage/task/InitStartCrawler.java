package com.bonc.dx.crawler_manage.task;

import com.bonc.dx.crawler_manage.service.TaskConfService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InitStartCrawler implements CommandLineRunner, ApplicationContextAware {
    @Autowired
    TaskConfService taskConfService;

    @Override
    public void run(String... args) throws Exception {
//        List<Map<String,String>> list = taskConfService.getType1("1");


    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        /*Map<String, Crawler> crawlers = applicationContext.getBeansOfType(Crawler.class);
        for(Map.Entry entry : crawlers.entrySet()){
            System.out.println(entry.getKey() + ":" + entry.getValue());
            if(entry.getKey().equals("GANSUGOVCrawller")){
                crawlers.get(entry.getKey()).run();

            }
        }*/

    }
}

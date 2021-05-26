package com.bonc.dx.crawler_manage.task;

import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.pool.driver.InitSystemProperty;
import com.bonc.dx.crawler_manage.pool.driver.ProxyChromeDriverPool;
import com.bonc.dx.crawler_manage.service.TaskConfService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Component
public class InitStartCrawler implements CommandLineRunner, ApplicationContextAware {
    @Autowired
    TaskConfService taskConfService;
    @Autowired
    ChromeDriverPool driverPool;

    @Autowired
    ProxyChromeDriverPool proxyDriverPool;
    @Autowired
    InitSystemProperty initSystemProperty;
    @Override
    public void run(String... args) throws Exception {
//        List<Map<String,String>> list = taskConfService.getType1("1");


    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Crawler> crawlers = applicationContext.getBeansOfType(Crawler.class);
//        driverPool.init(initSystemProperty);
        proxyDriverPool.init(initSystemProperty);
        for(Map.Entry entry : crawlers.entrySet()){
            System.out.println(entry.getKey() + ":" + entry.getValue());
            //需要测试某一个就放开if条件 匹配类的bean
                if(entry.getKey().equals("WEAINMILCrawller")){
                crawlers.get(entry.getKey()).run();

            }
        }
    }
}

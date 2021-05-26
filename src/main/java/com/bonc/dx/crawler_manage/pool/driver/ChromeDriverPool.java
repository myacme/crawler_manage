package com.bonc.dx.crawler_manage.pool.driver;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 阻塞队列实现一个池
 * 存放driver
 */
@Component
@DependsOn("initSystemProperty")
public class ChromeDriverPool {

    private  InitSystemProperty initSystemProperty;

    private static Logger log = LoggerFactory.getLogger(ChromeDriverPool.class);

    //todo 正式的池大小需扩大
    private LinkedBlockingQueue<WebDriver> queue = new LinkedBlockingQueue();

//    ChromeDriverPool(InitSystemProperty initSystemProperty){
//
//        initSystemProperty.init();
//        int size = Integer.parseInt(System.getProperty("driver_size"));
//        for(int i=0; i<size; i++){
//            add();
//        }
//        log.info("driver池 初始化完成， 数量 : {}",queue.size());
//    }

    public void init(InitSystemProperty initSystemProperty) {
        initSystemProperty.init();
        int size = Integer.parseInt(System.getProperty("driver_size"));
        for (int i = 0; i < size; i++) {
            add();
        }
        log.info("Driver池 定时初始化完成， 数量 : {}", queue.size());
    }

    /**
     * 阻塞获取，获取不到时，等待
     * @return
     */
    public WebDriver get() {
        try {
            log.info("driver池 获取一个资源，现有 : {}",queue.size());
            return  queue.take();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    private void add() {
        //设置代理
//        String path = System.getProperty("proxyDir") + "proxy" + i +".zip";
        ChromeOptions chromeOptions = new ChromeOptions();
//        File file = proxyZipUtil.getZip(path);
//        chromeOptions.addExtensions(file);
        //最大化
        chromeOptions.addArguments("start-maximized");
        WebDriver driver = new ChromeDriver(chromeOptions);
        //设置5分钟超时时间
        driver.manage().timeouts().pageLoadTimeout(300 , TimeUnit. SECONDS);
        queue.offer(driver);
    }

    /**
     * release 释放driver资源
     * @param driver
     */
    public void release(WebDriver driver) {
        queue.offer(driver);
        log.info("driver池 释放一个资源，剩余 : {}",queue.size());

    }
}

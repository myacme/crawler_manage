package com.bonc.dx.crawler_manage.pool.driver;

import com.bonc.dx.crawler_manage.pool.ip.ProxyZipUtil;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 阻塞队列实现一个池
 * 存放driver
 */
@Component
@DependsOn("initSystemProperty")
public class ChromeDriverPool implements DriverPool{

    private  InitSystemProperty initSystemProperty;

    private static Logger log = LoggerFactory.getLogger(ChromeDriverPool.class);

    //todo 正式的池大小需扩大
    private LinkedBlockingQueue<WebDriver> queue = new LinkedBlockingQueue();

    ChromeDriverPool(InitSystemProperty initSystemProperty){

        initSystemProperty.init();
        ProxyZipUtil proxyZipUtil = new ProxyZipUtil();
        int size = Integer.parseInt(System.getProperty("driver_size"));
        for(int i=0; i<size; i++){
            add(proxyZipUtil, i);
        }
        log.info("driver池 初始化完成， 数量 : {}",queue.size());
    }

    /**
     * 阻塞获取，获取不到时，等待
     * @return
     */
    @Override
    public WebDriver get() {
        try {
            log.info("driver池 获取一个资源，现有 : {}",queue.size());
            return  queue.take();
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }


    private void add(ProxyZipUtil proxyZipUtil, int i) {
        //设置代理
        String path = System.getProperty("proxyDir") + "proxy" + i +".zip";
        ChromeOptions chromeOptions = new ChromeOptions();
        File file = proxyZipUtil.getZip(path);
        chromeOptions.addExtensions(file);
        WebDriver driver = new ChromeDriver(chromeOptions);

        queue.offer(driver);
    }

    /**
     * release 释放driver资源
     * @param driver
     */
    @Override
    public void release(WebDriver driver) {

        queue.offer(driver);
        log.info("driver池 释放一个资源，剩余 : {}",queue.size());

    }
}

package com.bonc.dx.crawler_manage.pool.driver;

import com.bonc.dx.crawler_manage.pool.ip.ProxyZipUtil;
import com.bonc.dx.crawler_manage.util.ProperGet;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;


/**
 * 设置系统环境变量
 */
@Component
public class InitSystemProperty {

    public void init(){
        initProperties();
//        initProxyZip();
        String chromedriver = System.getProperty("chromedriver");
        System.setProperty("webdriver.chrome.driver", chromedriver);
        System.setProperty("webdriver.chrome.bin", "./");
    }

    /**
     * 初始化配置信息
     */
    public void initProperties() {
        Properties proper = new ProperGet().getPro();
        for (Map.Entry<Object, Object> one: proper.entrySet()){
            System.setProperty(String.valueOf(one.getKey()), String.valueOf(one.getValue()));
        }

    }

    /**
     * 初始化代理ip
     */
    public void initProxyZip(){
       new ProxyZipUtil().init();
    }


}

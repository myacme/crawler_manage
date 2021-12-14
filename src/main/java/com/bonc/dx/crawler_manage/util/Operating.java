package com.bonc.dx.crawler_manage.util;


import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * @author wuqhh
 * @date 2020/12/14 14:44
 */
public class Operating {
    public static WebDriver driver;
    public static final Logger log = LoggerFactory.getLogger(Operating.class);
    public Operating(){}

    public Operating(WebDriver webDriver) {
        driver = webDriver;
        driver.get("https://www.meituan.com");
    }
    public Operating(WebDriver webDriver, String url) {
        driver = webDriver;
        driver.get(url);
    }
    public void sleep(int seconds) {
        try {
            TimeUnit.SECONDS.sleep(seconds);
        } catch (InterruptedException e) {
            log.error("Sleep Fail {} seconds", seconds);
        }
    }
    public void setCookie(String cookies) {
//        driver.manage().deleteAllCookies();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 3);
        String[] list= cookies.split(";");
        for (String cookie : list) {
            String[] sp = cookie.trim().split("=");
            if (sp.length == 2) {
                System.out.println(sp[0] + " ---- " + sp[1]);
                Cookie cookie1 = new Cookie(sp[0], sp[1], ".meituan.com", "/", cal.getTime());
                driver.manage().addCookie(cookie1);
            }
        }
    }
    public void setCookie(String cookies,String domain) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 3);
        String[] list= cookies.split(";");
        for (String cookie : list) {
            String[] sp = cookie.trim().split("=");
            if (sp.length == 2) {
                System.out.println(sp[0] + " ---- " + sp[1]);
                Cookie cookie1 = new Cookie(sp[0], sp[1], domain, "/", cal.getTime());
                driver.manage().addCookie(cookie1);
            }
        }
    }
}

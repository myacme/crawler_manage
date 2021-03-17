package com.bonc.dx.crawler_manage.task.crawler.xl;


import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * 黑龙江公共资源交易中心
 */
@Component
public class HljGgzyjywCrawller implements Crawler {
    @Autowired
    CommonUtil commonUtil;
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired HljGgzyjywCrawllerUsePool hljGgzyjywCrawllerUsePool;

    private static Logger log = LoggerFactory.getLogger(HljGgzyjywCrawller.class);
    private static final String URL_PREFIX = "http://hljggzyjyw.gov.cn";


   @Async("taskpool")
    @Override
    public  void run() {

       Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());

       WebDriver driver = driverPool.get();
        try {

            String origin = "http://hljggzyjyw.gov.cn/trade/tradezfcg?cid=16&type=0";
            driver.get(origin);
            List<Map<String,String>> list1 = getType1(Jsoup.parse(driver.getPageSource()));
            for( Map<String,String> map : list1){
                hljGgzyjywCrawllerUsePool.run(map, days);
            }
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if(driver != null){
                driverPool.release(driver);
            }

        }
        System.out.println("exit");
    }



    public static List<Map<String,String>> getType1(Document document){
        List<Map<String,String>> list = new ArrayList<>();

        Elements elementsLi = document.select("ul.trade_ul > li");

        for(Element li : elementsLi){
            //一级分类
            Map<String,String> info = new HashMap<>(2);
            String type = li.getElementsByTag("a").text();
            String url = li.getElementsByTag("a").attr("href");
            info.put("type",type);
            info.put("url",url);
            log.info("type: {} {}",type,url);

            list.add(info);
        }
        return list;

    }

}

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 天津市政府采购网
 * 这个网站控制了访问频率，限制爬取速度，或者使用动态代理
 */
@Component
public class TjCcgpCrawller implements Crawler {
    @Autowired
    CommonUtil commonUtil;
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    TjCcgpCrawllerUsePool tjCcgpCrawllerUsePool;
    private static Logger log = LoggerFactory.getLogger(TjCcgpCrawller.class);

    private static final String URL_PREFIX = "http://www.ccgp-tianjin.gov.cn/portal/documentView.do?method=view&";
    private static String TABLE_NAME;



    @Async("taskpool")
    @Override
    public  void run( ) {
        WebDriver driver = driverPool.get();
        List<Map<String,String>> list = new ArrayList<>();
        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
        try {
            //获取要爬取url列表
            String origin = "http://www.ccgp-tianjin.gov.cn/portal/topicView.do";
            driver.get(origin);
            Document document = Jsoup.parse(driver.getPageSource());
            list = getUrlPart(document);
            for(Map<String,String> map : list){
                tjCcgpCrawllerUsePool.run(map, days);
            }

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if(driver != null){
                driverPool.release(driver);
            }
        }

    }



    public static List<Map<String,String>> getUrlPart(Document document){
        List<Map<String,String>> list = new ArrayList<>();

        Elements elementsUl = document.select("ul.oneWrap");
        Elements elementsLi = elementsUl.get(1).getElementsByTag("li");
        for(Element li : elementsLi){
            //一级分类
            Map<String,String> info = new HashMap<>(6);
            String type = li.getElementsByClass("twoHead").text();
            info.put("type",type);

            log.info("type: {}",type);

            //二级分类 市/区
            Elements elementsDiv = li.getElementsByTag("div");
            for(Element div : elementsDiv){
                Elements elementsA = div.getElementsByTag("a");
                for(Element a : elementsA){
                    info.put(a.text().split("\\(")[0].trim(),a.attr("href"));
                }
            }
            list.add(info);
        }
        return list;

    }


}

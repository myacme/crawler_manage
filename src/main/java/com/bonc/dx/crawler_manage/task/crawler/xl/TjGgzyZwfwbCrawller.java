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
 * 天津市公共资源交易网
 * http://60.28.163.169/jyxx/index.jhtml = http://ggzy.zwfwb.tj.gov.cn/jyxxzfcg/index.jhtml
 */
@Component
public class TjGgzyZwfwbCrawller implements Crawler {
    @Autowired
    CommonUtil commonUtil;
    @Autowired
    ChromeDriverPool driverPool;

    private static Logger log = LoggerFactory.getLogger(TjGgzyZwfwbCrawller.class);

    private static String TABLE_NAME;



    @Override
    @Async("taskpool")
    public  void run() {
        WebDriver driver = driverPool.get();

        try {
          String origin = "http://ggzy.zwfwb.tj.gov.cn/jyxxzfcg/index.jhtml";
          driver.get(origin);
          List<Map<String,String>> list = getType1(Jsoup.parse(driver.getPageSource()));


        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if(driver != null){
                driverPool.release(driver);
            }
        }
        System.out.println("exit");
    }


    public  List<Map<String,String>> getType1(Document document){
        List<Map<String,String>> list = new ArrayList<>();

        Elements elementsLi = document.select("ul.menu_list > li");

        for(Element li : elementsLi){
            //一级分类
            Map<String,String> info = new HashMap<>(4);
            String type = li.getElementsByTag("h3").text();
            String url = li.getElementsByTag("h3").attr("onclick").split("=")[1].replace("'","");
            info.put("type",type);
            info.put("url",url);
            log.info("type: {} {}",type,url);

            list.add(info);
        }
        return list;

    }

}

package com.bonc.dx.crawler_manage.task.crawler.xl.sub;


import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
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

import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 西藏自治区招投标网
 */
@Component
public class XzZbtbCrawllerUsePool {
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    CommonService commonService;
    private static Logger log = LoggerFactory.getLogger(XzZbtbCrawllerUsePool.class);


    private static final String URL_PREFIX = "http://www.xzzbtb.gov.cn/";
    private static String TABLE_NAME;


    @Async("taskpool2")
    public void run(Map<String,String> map,Map<String,String> days){


        WebDriver driver = driverPool.get();

        try {
            String url = map.get("url");
            System.out.println(url);
            int num = getPageSize(url, driver);
            for(int i=0; i<num; i++){
                boolean isContinue = getData(newUrl(i, url), driver, map.get("name"),days);
                if(!isContinue){
                    log.info("完成 : {}",map.get("name") );
                    break;
                }
            }

//            OtherDbDao.insertLogInfo("西藏自治区招投标网",Thread.currentThread().getStackTrace()[1].getClassName(),
//                    "success",Thread.currentThread().getName());

        } catch (Exception e) {
            e.printStackTrace();
//            OtherDbDao.insertLogInfo("西藏自治区招投标网",Thread.currentThread().getStackTrace()[1].getClassName(),
//                    "error",Thread.currentThread().getName() + ":" + e.getMessage());

        } finally {
            if(driver != null){
                driverPool.release(driver);
            }
        }
        System.out.println("exit");
    }

    /**
     * 增量爬取默认爬取100页
     * @param url
     * @param driver
     * @return
     */
    public static int getPageSize(String url, WebDriver driver){
        int total = 0;
        driver.get(url);
        try {
            System.out.println(driver.manage().timeouts());
        }catch (Exception e){
            e.printStackTrace();
        }
        Document document = Jsoup.parse(driver.getPageSource());
        String pageInfo = document.select("div.pagination").text();
        log.info("pages: {}", pageInfo);


        String reg = "(\\d+)/(\\d+)";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(pageInfo);
        if(matcher.find()){
            total = Integer.parseInt(matcher.group(0).split("/")[1]);
        }



        log.info("total: {}", total);

        return total;

    }

    public  String newUrl(int index, String url){
        return url + "?PAGE=" + (index+1);
    }



    public  boolean getData(String url, WebDriver driver,String type1,
                                Map<String,String> days){

        driver.get(url);


        Document document = Jsoup.parse(driver.getPageSource());
        Elements elementsLi = document.select("ul.x-main-jr-top-content > li");

        for(Element li : elementsLi){
            try {
                Map<String,String> res = new HashMap<>();
                res.put("type1", type1);
                String title = li.getElementsByTag("a").text();

                String time = li.getElementsByClass("jr-t-date").text();
                res.put("time",time);

                if (time.compareTo(days.get("start")) < 0) {
                    return false;
                }

                if(!(time.compareTo(days.get("end")) > 0)){
                    CrawlerEntity entity = new CrawlerEntity();
                    entity.setIsCrawl("1");
                    entity.setSource("西藏自治区招投标网");
                    entity.setCity("西藏");
                    entity.setType(type1);
                    entity.setSample("");
                    entity.setTitle(title);
                    entity.setDate(time);

                    entity.setUrl(URL_PREFIX + li.getElementsByTag("a").first().attr("href"));
                    System.out.println(entity.getUrl());
                    driver.get(entity.getUrl());
                    Document document2 = Jsoup.parse(driver.getPageSource());
                    String content = document2.select("div.myPrintArea").text();
                    entity.setContent(content);
                    System.out.println(entity);
                    commonService.insert(entity);

                    Thread.sleep(  (int)(Math.random()*1000));
                }



            }catch (Exception e){
                e.printStackTrace();
            }


        }

        return true;

    }
}

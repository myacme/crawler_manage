package com.bonc.dx.crawler_manage.task.crawler.xl;


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

import java.util.Map;

/**
 * 天津市政府采购网
 * 这个网站控制了访问频率，限制爬取速度，或者使用动态代理
 */
@Component
public class TjCcgpCrawllerUsePool {

    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    CommonService commonService;
    private static Logger log = LoggerFactory.getLogger(TjCcgpCrawllerUsePool.class);

    private static final String URL_PREFIX = "http://www.ccgp-tianjin.gov.cn/portal/documentView.do?method=view&";

    @Async("taskpool2")
    public  void run(Map<String,String> map,Map<String,String> days ) {
        WebDriver driver = driverPool.get();

        try {
          //获取要爬取url列表
            String origin = "http://www.ccgp-tianjin.gov.cn/portal/topicView.do";

            String urlTemp1 = origin + map.get("市级");
            String urlTemp2 = origin + map.get("区级");
            int num1 = getPageSize(urlTemp1,driver);
            int num2 = getPageSize(urlTemp2,driver);
            Thread.sleep(3000);

            for(int i=0; i<num1; i++){
                String url = newUrl(i,urlTemp1);
                boolean isContinue = getData(url, driver,map.get("type"),"市级",  days);
                if(!isContinue){
                    break;
                }

            }

            for(int i=0; i<num2; i++){
                String url = newUrl(i,urlTemp2);
                boolean isContinue = getData(url,driver,map.get("type"),"区级", days);
                if(!isContinue){
                    break;
                }
            }


//            OtherDbDao.insertLogInfo("天津市政府采购网",Thread.currentThread().getStackTrace()[1].getClassName(),
//                    "success",Thread.currentThread().getName());

        } catch (Exception e) {
            e.printStackTrace();
//            OtherDbDao.insertLogInfo("天津市政府采购网",Thread.currentThread().getStackTrace()[1].getClassName(),
//                    "error",Thread.currentThread().getName() + ":" + e.getMessage());
        } finally {
            if(driver != null){
                driverPool.release(driver);
            }
        }
        System.out.println("exit");
    }

    public  int getPageSize(String url, WebDriver driver){

        int total = 0;
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        try {
            total = Integer.parseInt(document.select("span.countPage > b").text());

        }catch (Exception e){
            e.printStackTrace();
        }

        log.info("total: {}", total);

        return total;

    }

    public  String newUrl(int index, String url){
        return url + "&page=" + (index+1);
    }



    public  boolean getData(String url, WebDriver driver,String type1, String type2,
                                Map<String,String> days){
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elementsLi = document.select("ul.dataList > li");


        try {
            Thread.sleep(1000 * 10 + (int) (Math.random() * 1000));
            
            for(Element li : elementsLi) {
                CrawlerEntity entity = new CrawlerEntity();
                entity.setIsCrawl("1");
                entity.setSource("天津市政府采购网");
                entity.setCity("天津");
                entity.setType(type1 );
                entity.setSample(li.getElementsByTag("a").text());
                entity.setTitle(li.getElementsByTag("a").text());
                String time = li.getElementsByTag("span").text();
                entity.setDate(time);

                String urlPart = li.getElementsByTag("a").first().attr("href");
                entity.setUrl(URL_PREFIX + urlPart.replace("/viewer.do?", ""));

                //只爬今年的
                if (time.compareTo(days.get("start")) < 0) {
                    return false;
                }
                if (!(time.compareTo(days.get("end")) > 0)){
                    driver.get(entity.getUrl());
                    Document document2 = Jsoup.parse(driver.getPageSource());
                    String content = document2.select("table > tbody").text();
                    entity.setContent(content);

                    commonService.insert(entity);
                    System.out.println(entity);


                    Thread.sleep(1000 * 10 + (int) (Math.random() * 1000));
                }


            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return true;
    }
}

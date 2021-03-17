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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 山东政府采购网
 */
@Component
public class SdCcgpCrawllerUsePool {

    private static Logger log = LoggerFactory.getLogger(SdCcgpCrawllerUsePool.class);
    private static final String URL_PREFIX = "http://www.ccgp-shandong.gov.cn";
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    CommonService commonService;
    @Async("taskpool2")
    public void run(Map<String,String> urlInfo,Map<String,String> days){

        System.out.println(Thread.currentThread().getName());

        WebDriver driver = driverPool.get();

        try {
            String urlTemp = "http://www.ccgp-shandong.gov.cn/sdgp2017/site/";

            String origin = urlInfo.get("url");
            driver.get(origin);
            List<Map<String,String>> list = getType2(Jsoup.parse(driver.getPageSource()));

            for(Map<String,String> map : list){
                String url = urlTemp + map.get("url");
                int num = getPageSize(url,driver);

                for(int i=0; i<num; i++){

                    boolean isCountinue = getData(newUrl(i,url),driver, map.get("type2"), days);
                    if(!isCountinue){
                        log.info("完成: {}",urlInfo.get("name") + "--" + map.get("type2"));
                        break;
                    }
                }
//                OtherDbDao.insertLogInfo("山东政府采购网",Thread.currentThread().getStackTrace()[1].getClassName(),
//                        "success",Thread.currentThread().getName());
            }


        } catch (Exception e) {
//            OtherDbDao.insertLogInfo("山东政府采购网",Thread.currentThread().getStackTrace()[1].getClassName(),
//                    "error",Thread.currentThread().getName() + ":" + e.getMessage());
            e.printStackTrace();
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
    public  int getPageSize(String url, WebDriver driver){
        int total = 0;
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        String pageInfo = document.select("div.page_list > span#totalnum").text();
        log.info("pages: {}", pageInfo);

        total = Integer.parseInt(pageInfo);

        log.info("total: {}", total);

        return total;

    }

    public  String newUrl(int index, String url){
        String s= url + "&curpage=" + (index+1) ;
        System.out.println(url);
        System.out.println(index);
        System.out.println(s);
        return s;
    }



    public  List<Map<String,String>> getType2(Document document){
        List<Map<String,String>> list = new ArrayList<>();

        Elements elementsLi = document.select("div.n_left > ul > li");

        for(Element li : elementsLi){
            //二级分类
            Map<String,String> info = new HashMap<>(4);
            String type2 = li.getElementsByTag("a").text();
            String url =  li.getElementsByTag("a").attr("href");
            info.put("type2",type2);
            info.put("url",url);
            log.info("type: {} {}",type2,url);
            list.add(info);

        }


        return list;

    }

    public  boolean getData(String url, WebDriver driver,String type2,
                                  Map<String,String> days){
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elementsLi = document.select("ul.news_list2 > li");

        try {
            Thread.sleep(1000 * 10 + (int) (Math.random() * 1000));
        }catch (Exception e){
            e.printStackTrace();
        }
        for(Element li : elementsLi){

            try {

                String title = li.getElementsByTag("a").first().attr("title");
                String sample = li.getElementsByTag("a").text();
                String time = li.getElementsByClass("hits").text();
                String city = "";
                try{
                    city = sample.split("【")[1].split("】")[0].substring(0,3);
                }catch (Exception e1){
                    e1.printStackTrace();
                }

                //只爬今年的
                if (time.compareTo(days.get("start")) < 0) {
                    return false;
                }

                if(!(time.compareTo(days.get("end")) > 0)){
                    CrawlerEntity entity = new CrawlerEntity();
                    entity.setIsCrawl("1");
                    entity.setSource("山东省政府采购网");
                    entity.setCity(city);
                    entity.setType(type2);
                    entity.setSample(sample);
                    entity.setTitle(title);
                    entity.setDate(time);

                    entity.setUrl(URL_PREFIX + li.getElementsByTag("a").first().attr("href"));
                    driver.get(entity.getUrl());
                    Document document2 = Jsoup.parse(driver.getPageSource());
                    String content = document2.select("div.listConts").text();
                    entity.setContent(content);

                    log.info("{} {}",Thread.currentThread().getName(), entity.getTitle());
                    commonService.insert(entity);

                    Thread.sleep( 1000*10 + (int)(Math.random()*1000));
                }

            }catch ( Exception e){
                e.printStackTrace();
            }
        }

        return true;
    }
}

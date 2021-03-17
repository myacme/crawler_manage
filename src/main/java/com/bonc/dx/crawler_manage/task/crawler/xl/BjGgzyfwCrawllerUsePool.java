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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 北京市公共资源交易服务平台
 */
@Component
public class BjGgzyfwCrawllerUsePool {
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    CommonService commonService;
    private static Logger log = LoggerFactory.getLogger(BjGgzyfwCrawllerUsePool.class);
    private static final String URL_PREFIX = "https://ggzyfw.beijing.gov.cn";

    @Async("taskpool2")
    public void run(Map<String,String> urlInfo,Map<String,String> days){

        System.out.println(Thread.currentThread().getName());

        WebDriver driver = driverPool.get();

        try {

            String urlTemp = urlInfo.get("url");
            int num = getPageSize(URL_PREFIX+urlTemp, driver);
            //翻页
            label:
            for(int i=0; i<num; i++){
                String url = urlTemp;

                if(i>0){
                    url = newUrl(i, urlTemp);
                }
                System.out.println(url);
                driver.get(URL_PREFIX+url);
                Thread.sleep(100);
                Document document = Jsoup.parse(driver.getPageSource());
                Elements elementsUl = document.select("ul.article-listjy2");
                //每页数据

                for(Element ui : elementsUl){
                    Elements li = ui.getElementsByTag("li");
                    for(Element element : li){

                        CrawlerEntity entity = new CrawlerEntity();
                        entity.setIsCrawl("1");
                        entity.setSource("北京市公共资源交易服务平台");
                        entity.setCity("北京");
                        entity.setType(urlInfo.get("type"));

                        String sample = element.getElementsByTag("a").text();
                        String title = element.getElementsByTag("a").first().attr("title");
                        String time = element.getElementsByClass("list-times1").text();
                        String urlPart = element.getElementsByTag("a").first().attr("href");
                        entity.setTitle(title);
                        entity.setDate(time);
                        entity.setSample(sample);
                        entity.setUrl(URL_PREFIX + urlPart);

                        driver.get(entity.getUrl());

                        // 比较当前网站发布日期是否大于指定日期，指定日期之后的才抓取
                        if(time.compareTo(days.get("start")) < 0 ){
                            break label;
                        }
                        //抛弃今天的数据
                        if(!(time.compareTo(days.get("end")) > 0)){
                            Document document2 = Jsoup.parse(driver.getPageSource());
                            String content = document2.select("div.content-list").text();
                            entity.setContent(content);
                            System.out.println(entity);
                            // 数据入库
                            commonService.insert(entity);
                        }

                    }

                }

                Thread.sleep((int)(Math.random()*100));
            }

//            OtherDbDao.insertLogInfo("北京市公共资源交易服务平台",Thread.currentThread().getStackTrace()[1].getClassName(),
//                    "success",Thread.currentThread().getName());
        } catch (Exception e) {
            e.printStackTrace();
//            OtherDbDao.insertLogInfo("北京市公共资源交易服务平台",Thread.currentThread().getStackTrace()[1].getClassName(),"error",Thread.currentThread().getName() + ":" + e.getMessage());
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
        System.out.println(url);
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        String pageInfo = document.select("ul.pages-list > li > a").text();
        log.info("pages: {}", pageInfo);


        String reg = "(\\d+)/(\\d+)";
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(pageInfo);
        if(matcher.find()){
            total = Integer.parseInt(matcher.group(0).split("/")[1]);
        }


        return total;

    }

    public  String newUrl(int index, String url){
        return url.replace("index.html","index_" + (index+1) + ".html");
    }


}

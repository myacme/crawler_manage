package com.bonc.dx.crawler_manage.task.crawler.xl.sub;


import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;

import com.bonc.dx.crawler_manage.service.CommonService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
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
 * 山东省公共资源交易中心
 */
@Component
public class SdGgzyjyCrawllerUsePool {

    private static Logger log = LoggerFactory.getLogger(SdGgzyjyCrawllerUsePool.class);
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    CommonService commonService;


    @Async("taskpool2")
    public void run(Map<String,String> urlInfo,Map<String,String> days) {
        System.out.println(Thread.currentThread().getName());

        WebDriver driver = driverPool.get();



        try {

            String origin = "http://ggzyjy.shandong.gov.cn/queryContent-jyxxgk.jspx" + "?channelId=" + urlInfo.get("channelId");

            int num = getPageSize(origin,driver);
            String type1 = urlInfo.get("name");
            for(int i=0; i<num; i++){
                String url = newUrl(i,origin);
                boolean isContinue = getData(url,driver,type1,days);
                if(!isContinue){
                    log.info("完成 ： {}", type1);
                    break;
                }
            }


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



    public  int getPageSize(String url, WebDriver driver){
        int total = 0;
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        String pageInfo = document.select("div.page-list > ul > li").first().getElementsByTag("a").text();
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
        return url.replace("queryContent","queryContent_" + (index+1) );
    }



    public  boolean getData(String url, WebDriver driver,String type1,
                                  Map<String,String> days){
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elementsLi = document.select("ul.article-list-a > li");

        try {
            Thread.sleep( 1000*10 + (int)(Math.random()*1000));
        }catch (Exception e){
            e.printStackTrace();
        }

        for(Element li : elementsLi){

           try {
               String title = li.getElementsByClass("article-list3-t").first().getElementsByTag("a").text();
               String time = li.getElementsByClass("list-times").text();

               String temp = time.split(" ")[0];
               //只爬今年的
               if ( temp.compareTo(days.get("start")) < 0) {
                   return false;
               }
               if(!(temp.compareTo(days.get("end")) > 0)){
                   CrawlerEntity entity = new CrawlerEntity();
                   entity.setIsCrawl("1");
                   entity.setSource("山东省公共资源交易中心");
                   if(title.contains("【") && title.contains("】")){
                       entity.setCity(title.split("【")[1].split("】")[0]);
                   }else {
                       entity.setCity("");
                   }
                   String typeTemp = li.getElementsByClass("article-list3-t2").first().getElementsByTag("div").last().text();


                   try {
                       entity.setType(typeTemp.split("：")[1]);
                   }catch (Exception e2){
                       e2.printStackTrace();
                       entity.setType("");
                   }
                   entity.setSample("");
                   entity.setTitle(title);
                   entity.setDate(temp);

                   //跳转新页面也需要控制频率
                   Thread.sleep( 1000*10 + (int)(Math.random()*1000));
                   String urlDetail = li.getElementsByTag("a").first().attr("href");

                   driver.findElement(By.cssSelector("a[href=\"" + urlDetail + "\"]")).click();


                   //跳转到新页面，获取内容和url 然后跳转回原来页面
                   String originalWindow = driver.getWindowHandle();
                   // 循环执行，直到找到一个新的窗口句柄
                   for (String windowHandle : driver.getWindowHandles()) {
                       if(!originalWindow.contentEquals(windowHandle)) {
                           driver.switchTo().window(windowHandle);
                           Document document2 = Jsoup.parse(driver.getPageSource());
                           String content = document2.select("div.div-content").text();

                           entity.setContent(content);
                           entity.setUrl(driver.getCurrentUrl());

                           log.info("{}",entity);
                           commonService.insert(entity);

                           driver.close();
                           driver.switchTo().window(originalWindow);

                       }
                   }

                   Thread.sleep( 1000*10 + (int)(Math.random()*1000));
               }

           }catch ( Exception e){
               e.printStackTrace();
           }


        }


        return true;
    }
}

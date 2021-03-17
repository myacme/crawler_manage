package com.bonc.dx.crawler_manage.task.crawler.xl;


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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 天津市公共资源交易网
 * http://60.28.163.169/jyxx/index.jhtml = http://ggzy.zwfwb.tj.gov.cn/jyxxzfcg/index.jhtml
 */
@Component
public class TjGgzyZwfwbCrawllerUsePool {
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    CommonService commonService;
    private static Logger log = LoggerFactory.getLogger(TjGgzyZwfwbCrawllerUsePool.class);

    private static String TABLE_NAME;



    @Async("taskpool2")
    public void run(Map<String,String> map,Map<String,String> days){



        System.out.println(Thread.currentThread().getName());

        WebDriver driver = driverPool.get();


        try {


          String origin2 = "http://ggzy.zwfwb.tj.gov.cn/queryContent-jyxx.jspx?";

          driver.get(map.get("url"));
          Thread.sleep(1000*3 + (int)(Math.random()*1000));
          List<Map<String,String>> list2 = getType2(Jsoup.parse(driver.getPageSource()));

          for(Map<String,String> map1 : list2){
              String url1 = origin2 + "channelId=" + map1.get("channelId");
              int num = getPageSize(url1, driver);
              for(int i=0; i<num; i++){
                 boolean isContinue =  getData(newUrl(i,url1),driver, map.get("type"),map1.get("type2"),days);
                 if(!isContinue){
                     log.info("完成 : {}",map.get("type") + "--" + map1.get("type2"));
                     break;
                 }
              }

          }



//            OtherDbDao.insertLogInfo("天津市公共资源交易网",Thread.currentThread().getStackTrace()[1].getClassName(),
//                    "success",Thread.currentThread().getName());

        } catch (Exception e) {
            e.printStackTrace();
//            OtherDbDao.insertLogInfo("天津市公共资源交易网",Thread.currentThread().getStackTrace()[1].getClassName(),
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

            String pageInfo = document.select("ul.pages-list > li").first().text();

            String reg = "(\\d+)/(\\d+)";
            Pattern pattern = Pattern.compile(reg);
            Matcher matcher = pattern.matcher(pageInfo);
            if(matcher.find()){
                total = Integer.parseInt(matcher.group(0).split("/")[1]);
            }


        }catch (Exception e){
            e.printStackTrace();
        }

        log.info("total: {}", total);

        return total;

    }

    public  String newUrl(int index, String url){
        return url.replace ("queryContent-jyxx","queryContent_" + (index+1) + "-jyxx");
    }


    public  List<Map<String,String>> getType2(Document document){
        List<Map<String,String>> list = new ArrayList<>();

        Element li1 = document.select("li.ywlx").first();
        Elements elementsLi = li1.select("ul > li");
        for(Element li : elementsLi){
            //二级分类
            Map<String,String> info = new HashMap<>(4);
            String type2 = li.text();
            String channelId = li.attr("value");
            info.put("type2",type2);
            info.put("channelId",channelId);
            log.info("type: {} {}",type2,channelId);
            if (!"全部".equals(type2)) {
                list.add(info);
            }
        }


        return list;

    }
    public  boolean getData(String url, WebDriver driver,String type1, String type2,
                               Map<String,String> days){
        driver.get(url);
        Document document = Jsoup.parse(driver.getPageSource());
        Elements elementsLi = document.select("ul.article-list2 > li");


        for(Element li : elementsLi){


            try {
                String title = li.getElementsByTag("a").text();
                String time = li.getElementsByClass("list-times").text();
                if ( time.compareTo(days.get("start")) < 0) {
                    return false;
                }

                if (!(time.compareTo(days.get("end")) > 0)) {
                    CrawlerEntity entity = new CrawlerEntity();
                    entity.setIsCrawl("1");
                    entity.setSource("天津市公共资源交易网");
                    entity.setCity("天津");
                    entity.setType(type2);
                    entity.setSample("");
                    entity.setTitle(title);
                    entity.setDate(time);

                    String urlDetail = li.getElementsByTag("a").first().attr("url");

                    //跳转新页面也需要控制频率
                    Thread.sleep( 1000*10 + (int)(Math.random()*1000));

                    driver.findElement(By.cssSelector("a[url=\"" + urlDetail + "\"]")).click();


                    //跳转到新页面，获取内容和url 然后跳转回原来页面
                    String originalWindow = driver.getWindowHandle();
                    // 循环执行，直到找到一个新的窗口句柄
                    for (String windowHandle : driver.getWindowHandles()) {
                        if(!originalWindow.contentEquals(windowHandle)) {
                            driver.switchTo().window(windowHandle);
                            Document document2 = Jsoup.parse(driver.getPageSource());
                            String content = document2.select("div#content").text();

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

            }catch (Exception e){
                e.printStackTrace();
            }
        }


        return true;

    }
}

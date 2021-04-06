//package com.bonc.dx.crawler_manage.task.crawler.xl.sub;
//
//
//import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
//import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
//
//import com.bonc.dx.crawler_manage.service.CommonService;
//import org.jsoup.Jsoup;
//import org.jsoup.nodes.Document;
//import org.jsoup.nodes.Element;
//import org.jsoup.select.Elements;
//import org.openqa.selenium.WebDriver;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//
//import java.util.*;
//
///**
// * 黑龙江公共资源交易中心
// */
//@Component
//public class HljGgzyjywCrawllerUsePool {
//
//    @Autowired
//    ChromeDriverPool driverPool;
//    @Autowired
//    CommonService commonService;
//    private static Logger log = LoggerFactory.getLogger(BjGgzyfwCrawllerUsePool.class);
//    private static final String URL_PREFIX = "https://ggzyfw.beijing.gov.cn";
//
//    @Async("taskpool2")
//    public void run(Map<String, String> map, Map<String, String> days) {
//        System.out.println(Thread.currentThread().getName());
//
//        WebDriver driver = driverPool.get();
//
//        try {
//            String prefix = "http://hljggzyjyw.gov.cn";
//
//            String urlTemp = prefix + map.get("url");
//            driver.get(urlTemp);
//            List<Map<String, String>> list2 = getType2(Jsoup.parse(driver.getPageSource()));
//            Thread.sleep(1000 * 3);
//
//            for (Map<String, String> map1 : list2) {
//
//                String url = prefix + map1.get("url");
//                int num = getPageSize(url, driver);
//                for (int i = 0; i < num; i++) {
//                    boolean isContinue = getData(newUrl(i, url), driver,
//                            map.get("type"), map1.get("type2"), days);
//                    if (!isContinue) {
//                        log.info("完成: {}", map.get("type") + " -- " + map1.get("type2"), days);
//                        break;
//                    }
//                }
//
//            }
//
//
////            OtherDbDao.insertLogInfo("黑龙江公共资源交易中心",Thread.currentThread().getStackTrace()[1].getClassName(),
////                    "success",Thread.currentThread().getName());
//        } catch (Exception e) {
//            e.printStackTrace();
////            OtherDbDao.insertLogInfo("黑龙江公共资源交易中心",Thread.currentThread().getStackTrace()[1].getClassName(),
////                    "error",Thread.currentThread().getName() + ":" +e.getMessage());
//        } finally {
//            if (driver != null) {
//                driverPool.release(driver);
//            }
//
//        }
//        System.out.println("exit");
//
//    }
//
//    public  int getPageSize(String url, WebDriver driver){
//
//        int total = 0;
//        driver.get(url);
//        Document document = Jsoup.parse(driver.getPageSource());
//        try {
//
//            total = Integer.parseInt(document.select("div.page > span > b.num").last().text());
//
//        }catch (Exception e){
//            e.printStackTrace();
//        }
//
//        log.info("total: {}", total);
//
//        return total;
//
//    }
//
//    public  String newUrl(int index, String url){
//        return url + "&pageNo=" + (index + 1);
//    }
//
//
//    public  List<Map<String,String>> getType2(Document document){
//        List<Map<String,String>> list = new ArrayList<>();
//
//        Elements elementsLi = document.select("ul.type > li.lis");
//        for(Element li : elementsLi){
//            //二级分类
//            Map<String,String> info = new HashMap<>(2);
//            String type2 = li.getElementsByTag("a").text();
//            String url =  li.getElementsByTag("a").attr("href");
//            info.put("type2",type2);
//            info.put("url",url);
//            log.info("type2: {} {}",type2,url);
//            if (!"全部".equals(type2) && !"".equals(type2) && !"搜索".equals(type2) ) {
//                list.add(info);
//            }
//        }
//
//
//
//        return list;
//
//    }
//    public  boolean getData(String url, WebDriver driver,String type1, String type2,
//                                  Map<String,String> days){
//        driver.get(url);
//        Document document = Jsoup.parse(driver.getPageSource());
//        Elements elementsLi = document.select("div.news_inf > div.right_box > ul > li");
//
//        for(Element li : elementsLi){
//
//            try {
//                String sample = li.getElementsByTag("a").text();
//                String title = li.getElementsByTag("a").first().attr("title");
//                String time = li.getElementsByClass("date").text();
//                String city = "";
//                try{
//                    System.out.println(title);
//                    city = title.split("\\[")[1].split("]")[0].substring(0,3);
//                }catch (Exception e1){
//                    e1.printStackTrace();
//                }
//                if (time.compareTo(days.get("start")) < 0) {
//                    return false;
//                }
//
//                if(!(time.compareTo(days.get("end")) > 0)){
//                    CrawlerEntity entity = new CrawlerEntity();
//                    entity.setIsCrawl("1");
//                    entity.setSource("黑龙江公共资源交易中心");
//                    entity.setCity(city);
//                    entity.setType(type2);
//                    entity.setSample(sample);
//                    entity.setTitle(title);
//                    entity.setDate(time);
//                    String crawlUrl = li.getElementsByTag("a").first().attr("href");
////                entity.setUrl(li.getElementsByTag("a").first().attr("href"));
//                    String saveUrl = URL_PREFIX+crawlUrl;
//                    entity.setUrl(saveUrl);
//                    driver.get(saveUrl);
//                    Document document2 = Jsoup.parse(driver.getPageSource());
//                    String content = document2.select("div.news_inf").text();
//                    entity.setContent(content);
//                    // 入库
//                        commonService.insert(entity);
//                    log.info("完成: {}",entity);
//
//                    Thread.sleep( 1000 + (int)(Math.random()*100));
//                }
//
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//
//
//        }
//
//
//        return true;
//
//    }
//}
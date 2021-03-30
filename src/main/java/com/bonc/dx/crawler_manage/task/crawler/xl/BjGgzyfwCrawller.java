package com.bonc.dx.crawler_manage.task.crawler.xl;



import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.task.crawler.xl.sub.BjGgzyfwCrawllerUsePool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 北京市公共资源交易服务平台
 */
@Component
public class BjGgzyfwCrawller implements Crawler {

    @Autowired
    BjGgzyfwCrawllerUsePool bjGgzyfwCrawllerUsePool;
    @Autowired
    CommonUtil commonUtil;
    @Autowired
    ChromeDriverPool driverPool;


    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getStackTrace()[1].getClassName());
    }
    @Async("taskpool")
    @Override
    public  void run( ) {
        List<Map<String,String>> list = getType1(initUrlInfo());
        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());

        for(Map<String,String> map : list){
            bjGgzyfwCrawllerUsePool.run(map,days);
        }

    }



    public List<Map<String,String>> initUrlInfo(){
        List<Map<String,String>> list = new ArrayList<>(5);
        Map<String,String> map1 = new HashMap<>(2);
        Map<String,String> map2 = new HashMap<>(2);
        Map<String,String> map3 = new HashMap<>(2);
        Map<String,String> map4 = new HashMap<>(2);
        Map<String,String> map5 = new HashMap<>(2);
        Map<String,String> map6 = new HashMap<>(2);
        Map<String,String> map7 = new HashMap<>(2);
        Map<String,String> map8 = new HashMap<>(2);

        map1.put("name","工程建设");
        map1.put("url","https://ggzyfw.beijing.gov.cn/jyxxggjtbyqs/index.html");
        map2.put("name","政府采购");
        map2.put("url","https://ggzyfw.beijing.gov.cn/jyxxcggg/index.html");
        map3.put("name","土地使用权");
        map3.put("url","https://ggzyfw.beijing.gov.cn/jyxxzpggg/index.html");
        map4.put("name","国有产权");
        map4.put("url","https://ggzyfw.beijing.gov.cn/jyxxswzcgpplxx/index.html");
        map5.put("name","碳排放权");
        map5.put("url","https://ggzyfw.beijing.gov.cn/jyxxtpfjyjg/index.html");
        map6.put("name","软件和信息服务");
        map6.put("url","https://ggzyfw.beijing.gov.cn/jyxxrjxxzbgg/index.html");
        map7.put("name","课题单位公开比选");
        map7.put("url","https://ggzyfw.beijing.gov.cn/gkbxgg/index.html");
        map8.put("name","其它");
        map8.put("url","https://ggzyfw.beijing.gov.cn/jyxxqtjygg/index.html");


        list.add(map1);
        list.add(map2);
        list.add(map3);
        list.add(map4);
//        list.add(map5);
        list.add(map6);
        list.add(map7);
        list.add(map8);
        return list;

    }
    public  List<Map<String,String>> getType1(List<Map<String,String>> types){
        List<Map<String,String>> list = new ArrayList<>(16);

        WebDriver driver = driverPool.get();
        try {
            for(Map<String,String> type : types) {
                driver.get(type.get("url"));
                Document document = Jsoup.parse(driver.getPageSource());

                Elements elementsLi = document.select("ul.panel-tab2").first().getElementsByTag("li");

                for (Element li : elementsLi) {

                    if (li.text().contains("公示类型")) {
                        Elements elementsLi2 = li.getElementsByTag("ul").first().getElementsByTag("li");

                        for (Element li2 : elementsLi2) {
                            Map<String, String> temp = new HashMap<>(2);
                            temp.put("type", li2.getElementsByTag("a").text());
                            temp.put("url", li2.getElementsByTag("a").attr("href"));
                            list.add(temp);
                        }
                    }
                }
            }
                Thread.sleep(1000);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(driver != null){
                driverPool.release(driver);
            }
        }




        return list;

    }
}

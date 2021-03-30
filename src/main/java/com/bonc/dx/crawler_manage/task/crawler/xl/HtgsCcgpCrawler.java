package com.bonc.dx.crawler_manage.task.crawler.xl;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.task.crawler.xl.sub.BjGgzyfwCrawllerUsePool;
import com.bonc.dx.crawler_manage.util.HtmlUtil;
import com.bonc.dx.crawler_manage.util.ffcode.Api;
import com.bonc.dx.crawler_manage.util.ffcode.Util;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static com.bonc.dx.crawler_manage.util.ffcode.Util.GetUrlImage;

@Component
public class HtgsCcgpCrawler implements Crawler {

    @Autowired
    CommonUtil commonUtil;
    @Autowired
    ChromeDriverPool driverPool;
    @Autowired
    CommonService commonService;

    @Async("taskpool")
    @Override
    public void run() {
        String origin = "http://htgs.ccgp.gov.cn/GS8/contractpublish/index";

        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());

        int flag = 0;
        WebDriver driver = driverPool.get();
        recursionGet(origin, driver, days,flag);

        System.out.println("exit");
    }

    public void recursionGet(String origin, WebDriver driver,Map<String,String> days, int flag){
        try {

            driver.get(origin);
            Thread.sleep(1000*1);
            String src = Jsoup.parse(driver.getPageSource()).select("div#codeImgDiv > img").first().attr("src");
            System.out.println(src);

            //获取图片码
            String codeTemp = Arrays.stream(src.split("\\/")).filter( e -> e.contains(".jpg")).findFirst().orElse("");
            if("".equals(codeTemp)){
                return;
            }

            String code = codeTemp.split("\\.")[0];


            String fadeRe = fateadm(code,driver,days);
            //打码失败 或 文件读取不到，重试30次
            if("".equals(fadeRe) || "fileNotFound".equals(fadeRe)){
                Thread.sleep(10000);
                if(flag > 30){
                    return;
                }
                flag++;
                recursionGet(origin, driver, days,flag);
            }

            Thread.sleep(10000);
            getData(driver, origin);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(driver != null){
                driverPool.release(driver);
            }
        }
    }
    /**
     * 打码
     * @param code
     * @param driver
     * @return
     */
    public String fateadm(String code, WebDriver driver,Map<String,String> days){

        try {

            String imageUrl = "http://htgs.ccgp.gov.cn/GS8/upload/verifyCodes/" + code + ".jpg";

            String url1= "http://htgs.ccgp.gov.cn/GS8/contractpublish/search?" +
                    "contractSign=0" +
                    "&searchPlacardStartDate=#{start}" +
                    "&searchPlacardEndDate=#{end}" +
                    "&codeResult=#{codeResult}" +
                    "&code=#{code}";


//            driver.get(imageUrl);

//            byte[] imgData = driver.getPageSource().getBytes(StandardCharsets.UTF_8);



            Api api = new Api();
            api.Init2();
            String pred_type = "30400";
            String codeString = api.PredictExtend(pred_type, GetUrlImage(imageUrl));

//            System.out.println(imgData);
//            String codeString = api.PredictExtend(pred_type, imgData);
            System.out.println(codeString);

            driver.get(url1.replace("#{codeResult}",code)
                .replace("#{code}",codeString)
                .replace("#{start}",days.get("start"))
                .replace("#{end}", days.get("end")));
            return codeString;

        }catch (FileNotFoundException fe){
            fe.printStackTrace();
            return "fileNotFound";
        } catch ( Exception e){
            e.printStackTrace();

        }
        return "";
    }

    public void getData(WebDriver driver, String origin){
        driver.get(origin);
        int total = getPage(Jsoup.parse(driver.getPageSource()));

        System.out.println("total: " + total);
        String prefix = "http://htgs.ccgp.gov.cn/GS8/contractpublish";

        for(int i=0; i<total; i++){
            System.out.println(newUrl(origin,i));
            driver.get(newUrl(origin,i));


            Document document =  Jsoup.parse(driver.getPageSource());
            Elements elementsLi = document.select("ul.ulst > li");

            try{
                Thread.sleep(1000 * 10 + (int) (Math.random() * 1000));
            }catch (Exception e){
                e.printStackTrace();
            }
            for(Element li : elementsLi){
                try{
                    CrawlerEntity entity = new CrawlerEntity();
                    entity.setSource("中国政府采购网");
                    entity.setType("政府采购合同公告");

                    entity.setTitle(li.getElementsByTag("a").first().text());
                    entity.setDate(li.getElementsByTag("span").get(1).text());
                    entity.setCity("");
                    entity.setIsCrawl("1");

                    String urlPart = li.getElementsByTag("a").first().attr("href");
                    entity.setUrl(prefix + urlPart.substring(1, urlPart.length()));

                    System.out.println(entity.getUrl());
                    driver.get(entity.getUrl());

                    entity.setContent(Jsoup.parse(driver.getPageSource()).text());

                    System.out.println(entity);
                    commonService.insert(entity);
                    Thread.sleep(1000 * 10 + (int) (Math.random() * 1000));
                }catch (Exception e){
                    e.printStackTrace();
                }

            }

        }


    }


    public int getPage(Document document){
        try {
            Elements elementsA = document.select("p.pager > a");
            return Integer.parseInt(elementsA.get(elementsA.size()-2).text());
        }catch (Exception e){
            e.printStackTrace();
        }
        return 1;
    }

    public String newUrl(String origin, int index){
        return origin.replace("index", "index_" + (index+1));
    }
}

package com.bonc.dx.crawler_manage.task.crawler.xl;

import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.task.crawler.xl.sub.BjGgzyfwCrawllerUsePool;
import com.bonc.dx.crawler_manage.task.crawler.xl.sub.DealGGzyCrawlerUserPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全国公共资源交易平台
 */
@Component
public class DealGGzyCrawler implements Crawler {


    @Autowired
    CommonUtil commonUtil;

    @Autowired
    CommonService commonService;

    @Autowired
    DealGGzyCrawlerUserPool dealGGzyCrawlerUserPool;


    public static void main(String[] args) {
        System.out.println(Thread.currentThread().getStackTrace()[1].getClassName());
    }
    @Async("taskpool")
    @Override
    public  void run( ) {


        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());

        List<Map<String,String>> types = initType();
        try {
            for(Map<String,String> map : types){
                dealGGzyCrawlerUserPool.run(map,days);
            }
            commonService.insertLogInfo("全国公共资源交易平台",DealGGzyCrawler.class.getName(),"success","");
        }catch ( Exception e){
            commonService.insertLogInfo("全国公共资源交易平台",DealGGzyCrawler.class.getName(),"error",e.getMessage());
        }


    }

    public List<Map<String,String>> initType(){
        String url = "http://deal.ggzy.gov.cn/ds/deal/dealList_find.jsp?" +
                "TIMEBEGIN_SHOW=#{startTime}" +
                "&TIMEEND_SHOW=#{endTime}" +
                "&TIMEBEGIN=#{startTime}" +
                "&TIMEEND=#{endTime}" +
                "&SOURCE_TYPE=#{sourceType}" +
                "&isShowAll=1" +
                "&DEAL_TIME=06" +
                "&DEAL_CLASSIFY=00" +
                "&DEAL_PROVINCE=0" +
                "&DEAL_STAGE=#{dealStage}" +
                "&DEAL_CITY=0" +
                "&PAGENUMBER=#{pageNum}" +
                "&DEAL_PLATFORM=0" +
                "&BID_PLATFORM=0&DEAL_TRADE=0" +
                "&FINDTXT";

        List<Map<String,String>> list = new ArrayList<>(2);
        Map<String,String> type1 = new HashMap<>(2);
        type1.put("url", url.replace("#{sourceType}","1")
                .replace("#{dealStage}","0001"));
        type1.put("type","交易公告");
        Map<String,String> type12 = new HashMap<>(2);
        type12.put("url", url.replace("#{sourceType}","1")
                .replace("#{dealStage}","0002"));
        type12.put("type","成交公示");

        Map<String,String> type2 = new HashMap<>(2);
        type2.put("url",url.replace("#{sourceType}","2")
                .replace("#{dealStage}","0001"));
        type2.put("type","交易公告");
        Map<String,String> type22 = new HashMap<>(2);
        type22.put("url", url.replace("#{sourceType}","1")
                .replace("#{dealStage}","0002"));
        type22.put("type","成交公示");

        list.add(type1);
        list.add(type12);
        list.add(type2);
        list.add(type22);

        return list;
    }

}

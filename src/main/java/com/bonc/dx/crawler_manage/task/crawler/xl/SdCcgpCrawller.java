package com.bonc.dx.crawler_manage.task.crawler.xl;

import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;

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
public class SdCcgpCrawller implements Crawler {

    @Autowired
    CommonUtil commonUtil;
    @Autowired
    SdCcgpCrawllerUsePool sdCcgpCrawllerUsePool;
    private static Logger log = LoggerFactory.getLogger(SdCcgpCrawller.class);

    private static String TABLE_NAME;


    @Override
    @Async("taskpool")
    public  void run() {
        log.info("thread: {}",Thread.currentThread().getName());

        List<Map<String,String>> list = getType();
        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());

        for(Map<String,String> map : list){
            sdCcgpCrawllerUsePool.run(map, days);
        }


    }



    public static List<Map<String,String>> getType(){
        List<Map<String,String>> list = new ArrayList<>(2);
        Map<String,String> map1 = new HashMap<>(2);
        Map<String,String> map2 = new HashMap<>(2);


        map1.put("name","市县信息公开");
        map1.put("url","http://www.ccgp-shandong.gov.cn/sdgp2017/site/listnew.jsp?grade=city&colcode=0303");
        map2.put("name","省级信息公开");
        map2.put("url","http://www.ccgp-shandong.gov.cn/sdgp2017/site/listnew.jsp?grade=province&colcode=2500");

        list.add(map1);
        list.add(map2);

        return list;

    }


}

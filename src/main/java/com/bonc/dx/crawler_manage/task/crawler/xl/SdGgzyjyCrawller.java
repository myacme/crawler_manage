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
 * 山东省公共资源交易中心
 */
@Component
public class SdGgzyjyCrawller implements Crawler {
    @Autowired
    BjGgzyfwCrawllerUsePool bjGgzyfwCrawllerUsePool;
    @Autowired
    CommonUtil commonUtil;
    private static Logger log = LoggerFactory.getLogger(SdGgzyjyCrawller.class);

    private static String TABLE_NAME;


    @Override
    @Async("taskpool")
    public   void run() {
        log.info("thread: {}",Thread.currentThread().getName());
        List<Map<String,String>> list = getType();
        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());

        for (Map<String,String> map : list){

        }


    }



    public static List<Map<String,String>> getType(){
        List<Map<String,String>> list = new ArrayList<>(8);
        Map<String,String> map1 = new HashMap<>(2);
        Map<String,String> map2 = new HashMap<>(2);
        Map<String,String> map3 = new HashMap<>(2);
        Map<String,String> map4 = new HashMap<>(2);
        Map<String,String> map5 = new HashMap<>(2);
        Map<String,String> map6 = new HashMap<>(2);
        Map<String,String> map7 = new HashMap<>(2);
        Map<String,String> map8 = new HashMap<>(2);

        map1.put("name","工程建设");
        map1.put("channelId","78");
        map2.put("name","土地使用权");
        map2.put("channelId","80");
        map3.put("name","矿业权出让");
        map3.put("channelId","81");
        map4.put("name","国有产权");
        map4.put("channelId","83");
        map5.put("name","政府采购");
        map5.put("channelId","79");
        map6.put("name","药械采购");
        map6.put("channelId","84");
        map7.put("name","国企采购");
        map7.put("channelId","167");
        map8.put("name","其他交易");
        map8.put("channelId","162");

        list.add(map1);
        list.add(map2);
        list.add(map3);
        list.add(map4);
        list.add(map5);
        list.add(map6);
        list.add(map7);
        list.add(map8);
        return list;

    }


}

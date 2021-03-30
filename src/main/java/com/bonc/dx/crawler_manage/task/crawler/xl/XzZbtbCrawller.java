package com.bonc.dx.crawler_manage.task.crawler.xl;

import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;

import com.bonc.dx.crawler_manage.task.crawler.xl.sub.XzZbtbCrawllerUsePool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.*;


/**
 * 西藏自治区招投标网
 */
@Component
public class XzZbtbCrawller implements Crawler {
    @Autowired
    CommonUtil commonUtil;
    @Autowired
    XzZbtbCrawllerUsePool xzZbtbCrawllerUsePool;

    private static Logger log = LoggerFactory.getLogger(XzZbtbCrawller.class);

    private static String TABLE_NAME;





    @Async("taskpool")
    @Override
    public  void run( ) {

        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());

        List<Map<String,String>> list = getType();
        for(Map<String,String> map : list){
            xzZbtbCrawllerUsePool.run(map,days);
        }

    }


    public static List<Map<String,String>> getType(){
        List<Map<String,String>> list = new ArrayList<>(3);
        Map<String,String> map1 = new HashMap<>(2);
        Map<String,String> map2 = new HashMap<>(2);
        Map<String,String> map3 = new HashMap<>(2);


        map1.put("name","招标公告");
        map1.put("url","http://www.xzzbtb.gov.cn/xz/publish-notice!tenderNoticeView.do");
        map2.put("name","拉萨市招标公告");
        map2.put("url","http://www.xzzbtb.gov.cn/xz/publish-notice!sccinNoticeView.do");
        map3.put("name","中标公告");
        map3.put("url","http://www.xzzbtb.gov.cn/xz/publish-notice!preAwardNoticeView.do");


        list.add(map1);
        list.add(map2);
        list.add(map3);

        return list;

    }


}

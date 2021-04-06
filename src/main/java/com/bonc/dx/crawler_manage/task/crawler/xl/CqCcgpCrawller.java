//package com.bonc.dx.crawler_manage.task.crawler.xl;
//
//
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
//import com.bonc.dx.crawler_manage.service.CommonService;
//import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
//import com.bonc.dx.crawler_manage.task.crawler.Crawler;
//import com.bonc.dx.crawler_manage.util.HtmlUtil;
//import org.apache.http.HttpEntity;
//import org.apache.http.HttpResponse;
//import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.impl.client.HttpClientBuilder;
//import org.apache.http.util.EntityUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Async;
//
//import java.util.*;
//
//
///**
// * 重庆市政府采购网
// */
//public class CqCcgpCrawller implements Crawler {
//
//    @Autowired
//    CommonUtil commonUtil;
//    @Autowired
//    CommonService commonService;
//
//    private static Logger log = LoggerFactory.getLogger(CqCcgpCrawller.class);
//    private static final String URL_PREFIX2 = "https://www.ccgp-chongqing.gov.cn/gwebsite/api/v1/notices/stable/";
//    private static final String URL_PREFIX = "https://www.ccgp-chongqing.gov.cn/notices/detail/";
//
//
//    public static HttpClient httpClient;
//
//
//    static {
//        httpClient = HttpClientBuilder.create().build();
//
//    }
//
//
//
//    /**
//     * 开始时间结束时间
//     */
//    @Async("taskpool")
//    @Override
//    public  void run() {
//        Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
//        String origin = "https://www.ccgp-chongqing.gov.cn/gwebsite/api/v1/notices/stable/new?"
//                + "startDate=" + days.get("start") + "&endDate=" + days.get("end")
//                + "&ps=100";
//        List<Map<String,String>> list = getType();
//        //循环爬取分类数据
//        for(Map<String,String> map : list){
//            int index = 0;
//            int total = 0;
//            //避免死循环
//            int flag = 0;
//            do {
//                String url =  getUrl(origin, map.get("typeId"), index);
//                HttpGet get = new HttpGet(url);
//                try {
//                    //http
//                    HttpResponse response = httpClient.execute(get);
//
//                    JSONObject jsonObject = new JSONObject();
//                    if(response.getStatusLine().getStatusCode() == 200){
//                        HttpEntity entity = response.getEntity();
//
//                        String resJson = EntityUtils.toString(entity);
//                        jsonObject = JSONObject.parseObject(resJson);
//                    }
//
//                    //结果
//                    if(jsonObject != null && "success".equals(jsonObject.getString("msg"))){
//                        total = Integer.parseInt(jsonObject.getString("total"));
//                        JSONArray jsonArray = jsonObject.getJSONArray("notices");
//
//                        jsonArray.forEach( e -> {
//
//                            String title =  ((JSONObject)e).getString("title");
//                            String time = ((JSONObject)e).getString("issueTime").split(" ")[0];
//                            String type =  map.get("name");
//                            String id = ((JSONObject)e).getString("id");
//                            String urlDetail = "";
//                            try {
//                                urlDetail = URL_PREFIX + id + "?title=" + title;
//                            }catch (Exception e1){
//                                e1.printStackTrace();
//
//                            }
//
//                            CrawlerEntity entity = new CrawlerEntity();
//                            entity.setIsCrawl("1");
//                            entity.setSource("重庆市政府采购网");
//                            entity.setCity("重庆");
//                            entity.setType(type);
//                            entity.setSample("");
//                            entity.setTitle(title);
//                            entity.setDate(time);
//                            entity.setUrl(urlDetail);
//
//                            try {
//                                String url2 = URL_PREFIX2 + id ;
//                                HttpGet get2 = new HttpGet(url2);
//                                HttpResponse response2 = httpClient.execute(get2);
//
//                                JSONObject jsonObject2 = new JSONObject();
//                                if(response2.getStatusLine().getStatusCode() == 200){
//                                    HttpEntity entity2 = response2.getEntity();
//
//                                    String resJson = EntityUtils.toString(entity2);
//                                    jsonObject2 = JSONObject.parseObject(resJson);
//                                }
//                                String content = jsonObject2.getJSONObject("notice").getString("html");
//                                entity.setContent(HtmlUtil.deatilHtml(content));
//                                Thread.sleep(1000);
//
//                            }catch (Exception e2){
//                                e2.printStackTrace();
//                            }
//                            //保存数据
//                            commonService.insert(entity);
//                            log.info("完成: {}",entity);
//
//                        });
//                        index++;
//
//                    }else {
//                        flag++;
//                    }
//
//                }catch ( Exception e){
//                    e.printStackTrace();
//                    flag++;
////                    OtherDbDao.insertLogInfo("重庆市政府采购网",Thread.currentThread().getStackTrace()[1].getClassName(),"error",
////                            Thread.currentThread().getName() + ":" + e.getMessage());
//                }finally {
//                    try {
//                        get.releaseConnection();
//                        Thread.sleep(1000);
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }
//                }
//
//            }while ((index)*100 <= total && flag <= 10);
//        }
//
////        OtherDbDao.insertLogInfo("重庆市政府采购网",Thread.currentThread().getStackTrace()[1].getClassName(),"success",Thread.currentThread().getName());
//
//    }
//
//    public  String getUrl(String url, String typeId, int index){
//       return url + "&projectPurchaseWay=" + typeId + "&pi=" + (index+1);
//
//    }
//
//    public  List<Map<String,String>> getType(){
//        List<Map<String,String>> list = new ArrayList<>(2);
//        Map<String,String> map1 = new HashMap<>(2);
//        Map<String,String> map2 = new HashMap<>(2);
//        Map<String,String> map3 = new HashMap<>(2);
//        Map<String,String> map4 = new HashMap<>(2);
//        Map<String,String> map5 = new HashMap<>(2);
//        Map<String,String> map6 = new HashMap<>(2);
//        Map<String,String> map7 = new HashMap<>(2);
//        Map<String,String> map8 = new HashMap<>(2);
//
//        map1.put("name","公开招标");
//        map1.put("typeId","100");
//        map2.put("name","邀请招标");
//        map2.put("typeId","200");
//        map3.put("name","竞争性谈判");
//        map3.put("typeId","300");
//        map4.put("name","询价");
//        map4.put("typeId","400");
//        map5.put("name","单一来源");
//        map5.put("typeId","500");
//        map6.put("name","竞争性磋商");
//        map6.put("typeId","800");
//        map7.put("name","协议竞价");
//        map7.put("typeId","6001");
//        map8.put("name","网上询价");
//        map8.put("typeId","6003");
//
//        list.add(map1);
//        list.add(map2);
//        list.add(map3);
//        list.add(map4);
//        list.add(map5);
//        list.add(map6);
//        list.add(map7);
//        list.add(map8);
//
//        return list;
//
//    }
//
//
//
//}

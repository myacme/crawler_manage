package com.bonc.dx.crawler_manage.task.crawler.xl.sub;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.util.HtmlUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;


@Component
public class DealGGzyCrawlerUserPool {

    @Autowired
    CommonService commonService;
    @Autowired
    CommonUtil commonUtil;

    public  HttpClient httpClient = HttpClientBuilder.create().build();

    /**
     * 不使用二级线程池
     * @param urlInfo
     * @param days
     */

//    @Async("taskpool2")
    public void run(Map<String, String> urlInfo, Map<String, String> days) {

        String orgin = urlInfo.get("url")
                .replace("#{startTime}",days.get("start"))
                .replace("#{endTime}",days.get("end"));


        int index = 1;
        int total = 1;
        do {
            String url =  orgin.replace("#{pageNum}",String.valueOf(index));
            HttpGet get = new HttpGet(url);
            try {
                //http
                HttpResponse response = httpClient.execute(get);

                JSONObject jsonObject = new JSONObject();
                if(response.getStatusLine().getStatusCode() == 200){
                    HttpEntity entity = response.getEntity();

                    String resJson = EntityUtils.toString(entity);
                    jsonObject = JSONObject.parseObject(resJson);
                }

                //结果
                if(jsonObject != null && "true".equals(jsonObject.getString("success"))){
                    total = Integer.parseInt(jsonObject.getString("ttlpage"));
                    JSONArray jsonArray = jsonObject.getJSONArray("data");

                    jsonArray.forEach( e -> {

                        String title =  ((JSONObject)e).getString("title");
                        String time = ((JSONObject)e).getString("timeShow");
                        String type =  urlInfo.get("type");
                        String source = "全国公共资源交易网";
                        String urlDetail = ((JSONObject)e).getString("url");
                        //替换详情页url  爬取正确content 访问也更快
                        urlDetail = urlDetail.replace("html/a/", "html/b/");
                        String city = ((JSONObject)e).getString("districtShow");


                        CrawlerEntity entity = new CrawlerEntity();
                        entity.setIsCrawl("1");
                        entity.setSource(source);
                        entity.setCity(city);
                        entity.setType(type);
                        entity.setSample("");
                        entity.setTitle(title);
                        entity.setDate(time);
                        entity.setUrl(urlDetail);


                        HttpGet get2 = new HttpGet(entity.getUrl());
                        try {

                            HttpResponse response2 = httpClient.execute(get2);


                            if(response2.getStatusLine().getStatusCode() == 200){
                                HttpEntity entity2 = response2.getEntity();

                                String res = EntityUtils.toString(entity2);
                                entity.setContent(HtmlUtil.deatilHtml(res));
                            }



                        }catch (Exception e2){
                            e2.printStackTrace();
                        }finally {
                            get2.releaseConnection();
                        }
                        //保存数据
//                        commonService.insertTable(entity,commonUtil.getTableName());
                        commonService.insertTable(entity,"bj_bidweb64_info_20210309_20210421");
                        try {
                            Thread.sleep(10000);

                        }catch (Exception e2){
                            e2.printStackTrace();
                        }

                    });
                    index++;

                }else {
                   index++;
                }

            }catch ( Exception e){
                e.printStackTrace();
                index++;

            }finally {
                try {
                    get.releaseConnection();
                    Thread.sleep(10000);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }

        }while (index <= total );

    }



}
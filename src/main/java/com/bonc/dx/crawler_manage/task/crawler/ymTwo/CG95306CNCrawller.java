package com.bonc.dx.crawler_manage.task.crawler.ymTwo;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.util.ffcode.Api;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.bonc.dx.crawler_manage.util.ffcode.Util.GetUrlImage;

/**
 * @author ym
 * @date 2021-5-26 09:43:28
 */
@Component
public class CG95306CNCrawller implements Crawler {

	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CG95306CNCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "中国铁路总公司";
	private static final String CITY = "直管政务网站";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "https://cg.95306.cn/proxy/portal/elasticSearch/queryDataToEs?projBidType=01&pageNum=";
	private static String reg2 = "https://cg.95306.cn/proxy/portal/elasticSearch/indexView?noticeId=";
	private static String reg3 = "https://cg.95306.cn/proxy/portal/elasticSearch/indexView?noticeId=11a699bb7b54d11fff6b11da863b7f88&code=";
	private static String reg4 = "https://cg.95306.cn/baseinfor/notice/informationShow?id=";

	@Autowired
	CommonUtil commonUtil;

	@Override
	@Async("taskpool")
	public  void run() {
		log.info("thread: {}",Thread.currentThread().getName());
		runTest();


	}


	public void runTest() {
		HttpGet get = null;
		try {
			HttpClient httpClient = HttpClientBuilder.create().build();
			Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			String table_name = commonUtil.getTableName();

			begin_time = days.get("start");
			end_time = days.get("end");

			isNext = true;
			int pageNum = 31;
			int repeat = 1;
			while (isNext) {
				System.out.println(reg+pageNum);
				get = new HttpGet(reg+pageNum);

				//http
				HttpResponse response = httpClient.execute(get);

				JSONObject jsonObject = new JSONObject();
				//访问成功
				if(response.getStatusLine().getStatusCode() == 200){
					HttpEntity entity = response.getEntity();

					String resJson = EntityUtils.toString(entity);
					jsonObject = JSONObject.parseObject(resJson);
				}
				if(jsonObject != null ){
					if (jsonObject.getBoolean("success")) {
						//页码调下一页
                        pageNum++;
                        //列表页记次数重置
						repeat = 1;
						JSONObject resultData = jsonObject.getJSONObject("data").getJSONObject("resultData");
						isNext = !resultData.getBoolean("lastPage");
						JSONArray lis = resultData.getJSONArray("result");
						for (Object e : lis) {
							String title =  ((JSONObject)e).getString("notTitle");
							String date = ((JSONObject)e).getString("checkTime");
							String type = ((JSONObject)e).getString("bidTypeName");
							String id = ((JSONObject)e).getString("id");

							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
//                                isNext = true;
								continue;
							} else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前
//                                isNext = true;
//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
                                int verification_num = verification(1, get, httpClient, id, title, type, date, TABLE_NAME,"");
//                                int verification_num = verification(1, get, httpClient, id, title, type, date, table_name,"");
                                if (verification_num > 5){
                                    commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"error","详情页验证码重试5次不成功");
//                                    return;
                                }
                            } else {
//								isNext = false;
							}


						}
					}else {
						//列表页有验证码了
                        if (repeat > 5){
                            commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"error","列表页验证码重试5次不成功");
                            return;
                        }
                        fateadmPage(get,httpClient);
                        repeat++;
					}
				}

			}

            commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"error",e.getMessage());
		} finally {
			try {
				if (get!=null) {
					get.releaseConnection();
				}
			}catch (Exception e){
				e.printStackTrace();
			}
		}
		System.out.println("exit");
	}

	public String fateadm() {
		try {
			String imageUrl = "https://cg.95306.cn//proxy/portal/enterprise/base/loadComplexValidCodeImg?validCodeKey=1622013362744";
			Api api = new Api();
			api.Init2();
			String pred_type = "30500";
			String codeString = api.PredictExtend(pred_type, GetUrlImage(imageUrl)).toUpperCase();
			System.out.println("codeString="+codeString);
			return codeString;
		} catch (FileNotFoundException fe) {
			fe.printStackTrace();
			return "fileNotFound";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

    public void fateadmPage(HttpGet get,HttpClient httpClient)throws Exception{
        String fateadm = fateadm();
        get = new HttpGet(reg3+fateadm);
		HttpResponse response2 = httpClient.execute(get);
		JSONObject jsonObject2 = new JSONObject();
		//访问成功
		if(response2.getStatusLine().getStatusCode() == 200){
			HttpEntity entity = response2.getEntity();

			String resJson = EntityUtils.toString(entity);
			jsonObject2 = JSONObject.parseObject(resJson);
		}
    }

	public int verification(int num,HttpGet get,HttpClient httpClient,String id,String title,String type,String date,String table_name,String fateadm) throws Exception{
		String urlDetail = reg2+id;
		System.out.println(urlDetail);
		if (fateadm.equals("")) {
            //第一次进来  没有验证码
            get = new HttpGet(urlDetail);
        }else {
            get = new HttpGet(urlDetail+"&code="+fateadm);
        }
        Thread.sleep(2000);

        //http
		HttpResponse response2 = httpClient.execute(get);

        JSONObject jsonObject2 = new JSONObject();
        //访问成功
        if(response2.getStatusLine().getStatusCode() == 200){
            HttpEntity entity = response2.getEntity();

            String resJson = EntityUtils.toString(entity);
            jsonObject2 = JSONObject.parseObject(resJson);
        }
        if(jsonObject2 != null ){
            if (jsonObject2.getBoolean("success")) {
                String string = jsonObject2.getJSONObject("data").getJSONObject("noticeContent").getString("notCont");
                Document doc = Jsoup.parse(string);
                String content = doc.text();
                //加入实体类 入库
                CrawlerEntity insertMap = new CrawlerEntity();
                insertMap.setUrl(reg4+id);
                insertMap.setTitle(title);
                insertMap.setCity(CITY);
                insertMap.setType(type);
                insertMap.setDate(date);
                insertMap.setContent(content);
                insertMap.setSource(SOURCE);
                insertMap.setIsCrawl("1");
                commonService.insertTable(insertMap, table_name);
            }else {
                //详情页有验证码了
                fateadm = fateadm();
                if (num > 5){
					System.out.println("重试超过5次");
                    return num;
                }else {
                    num++;
					num = verification(num, get, httpClient, id, title, type, date, table_name,fateadm);
                }
            }
        }
	    return num;
    }
}


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
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

	@Autowired(required = false)
	ChromeDriverPool driverPool;
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
	private static String reg = "https://cg.95306.cn/baseinfor/notice/procurementNotice?bidType=&noticeType=000&transactionType=01&wzType=&title=&bidding=&navigation=";

	private static String reg2 = "https://cg.95306.cn/baseinfor/notice/informationShow?id=";

	@Autowired
	CommonUtil commonUtil;

	@Override
	@Async("taskpool")
	public  void run() {
		log.info("thread: {}",Thread.currentThread().getName());
		runTest();


	}


	public void runTest() {
		WebDriver driver = null;
		WebDriver driver2 = null;
		try {
			Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			String table_name = commonUtil.getTableName();

			begin_time = days.get("start");
			end_time = days.get("end");
//			end_time = "2021-5-28";
			driver = driverPool.get();
			driver2 = driverPool.get();

			driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);

			System.out.println(reg);
			driver.get(reg);
			Thread.sleep(2000);
			//进入验证码验证
			int num = fateadmPage(driver, 1);
			if (num > 5){
				commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"error","列表页验证码重试5次不成功");
				return;
			}

			isNext = true;
			int repeat = 1;
			while (isNext) {
				Document list_doc = Jsoup.parse(driver.getPageSource());
				Elements lis = list_doc.select("div.text_con > div.text_row");
//				List<WebElement> lis = driver.findElements(By.cssSelector("div.text_con > div.text_row"));
				for (Element li : lis) {
					String title =  li.select("div.text_row_htit.clearfix > div.htit").text();
					String date = li.select("div.text_row_htit.clearfix > div.declare_time >span:nth-child(2)").text();
					String type = li.select("div.text_row_tab > ul > li.first").text().trim();
					String id = li.select("div.text_row_htit.clearfix > div.htit > a").attr("onclick");
					id = id.replace("noticeDetailNotice('","").replace("')","");
					String url = reg2+id;



					if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
						//结束时间在爬取到的时间之前 就下一个
							isNext = true;
						continue;
					} else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
						//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前
						isNext = true;

						driver2.get(url);
						Thread.sleep(2000);

						//进入验证码验证
						int num3 = fateadmPage(driver2, 1);
						if (num3 > 10){
							commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"error","详情页验证码重试10次不成功");
							return;
						}
						Document doc = Jsoup.parse(driver2.getPageSource());
						String content = doc.select("#main >div.main_content > div.article_detail").text();

						//加入实体类 入库
						CrawlerEntity insertMap = new CrawlerEntity();
						insertMap.setUrl(url);
						insertMap.setTitle(title);
						insertMap.setCity(CITY);
						insertMap.setType(type);
						insertMap.setDate(date);
						insertMap.setContent(content);
						insertMap.setSource(SOURCE);
						insertMap.setIsCrawl("1");
//						System.out.println("=====================" + insertMap.toString());
//						commonService.insertTable(insertMap, TABLE_NAME);
						commonService.insertTable(insertMap, table_name);
					} else {
							isNext = false;
					}


				}

				if (isNext) {
					log.info(driver.getCurrentUrl());
					WebElement next = driver.findElement(By.cssSelector("div.next_page"));
					next.click();
					Thread.sleep(2000);
					//进入验证码验证
					int num2 = fateadmPage(driver, 1);
					if (num2 > 10){
						commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"error","列表页验证码重试10次不成功");
						return;
					}
				} else {
					break;
				}
			}
            commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CG95306CNCrawller.class.getName(),"error",e.getMessage());
		} finally {
			if(driver != null){
				driverPool.release(driver);
			}
			if(driver2 != null){
				driverPool.release(driver2);
			}
		}
		System.out.println("exit");
	}

	public String fateadm(String imageUrl) {
		try {
//			String imageUrl = "https://cg.95306.cn"+src;
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

    public int fateadmPage(WebDriver driver,int num){
		if (num > 10){
			return num;
		}
		try {
			//没找到时进入catch表示没有验证码
			//点击刷新验证码，防止识别错误后图片不变
			driver.findElement(By.cssSelector("#validCodeImg")).click();
			Thread.sleep(500);
			String src = driver.findElement(By.cssSelector("#validCodeImg")).getAttribute("src");
			String codeString = fateadm(src);
			driver.findElement(By.cssSelector("#validateCode")).clear();
			driver.findElement(By.cssSelector("#validateCode")).sendKeys(codeString);
			Thread.sleep(500);
			driver.findElement(By.cssSelector("div.layui-layer-btn.layui-layer-btn- > a")).click();
			Thread.sleep(500);
			num++;
			try {
				//验证错误时候  通过找到下面的标签判断是否验证失败
				driver.findElement(By.cssSelector("div.layui-layer.layui-layer-dialog.layui-layer-border.layui-layer-msg"));
				Thread.sleep(5000);
				num = fateadmPage(driver,num);
			}catch (Exception e){

			}
		}catch (Exception e){
			return num;
		}
		return num;
	}

}


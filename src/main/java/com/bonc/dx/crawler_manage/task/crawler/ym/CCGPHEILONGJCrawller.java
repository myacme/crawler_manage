package com.bonc.dx.crawler_manage.task.crawler.ym;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * @author ym
 * @date 2021-3-29 10:10:23
 */
@Component
public class CCGPHEILONGJCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CCGPHEILONGJCrawller.class);
	private static long ct = 0;
	private static boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "黑龙江政府采购网";
	private static final String CITY = "黑龙江省";
	private static String begin_time;
	private static String end_time;
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "http://www.ccgp-heilongj.gov.cn/index.jsp";
	private static String front = "http://www.ccgp-heilongj.gov.cn";
//	private static String reg = "http://search.ccgp.gov.cn/bxsearch?searchtype=1&page_index=138&bidSort=&buyerName=&projectId=&pinMu=&bidType=&dbselect=bidx&kw=&start_time=2021%3A01%3A26&end_time=2021%3A02%3A02&timeType=2&displayZone=&zoneId=&pppStatus=0&agentName=";

	@Autowired
	CommonUtil commonUtil;

	@Override
	@Async("taskpool")
	public  void run() {
		log.info("thread: {}",Thread.currentThread().getName());
		runTest();


	}


	public void runTest() {
		Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
		WebDriver driver = driverPool.get();
//		chromeOptions2.addArguments("--headless --no-sandbox\n".split(" "));
		WebDriver driver2 = driverPool.get();
		String table_name = commonUtil.getTableName();
		try {
			driver.get(reg);
			Thread.sleep(2000);

			end_time = days.get("start");
            begin_time = days.get("end");


			for (int i = 0; i < 5; i++) {
				WebElement souye = driver.findElement(By.cssSelector("#nav > ul > li:first-child > a"));
				((JavascriptExecutor)driver).executeScript("arguments[0].click();",souye);
				Thread.sleep(2000);
				if (i == 0) {
					WebElement gengduo = driver.findElement(By.cssSelector("#confive1 > div > a"));
					((JavascriptExecutor)driver).executeScript("arguments[0].click();",gengduo);
					type = "采购公告";
				} else if (i == 1) {
					WebElement gengduo = driver.findElement(By.cssSelector("#consix1 > div > a"));
					((JavascriptExecutor)driver).executeScript("arguments[0].click();",gengduo);
					type = "特殊公告";
				} else if (i == 2) {
					WebElement gengduo = driver.findElement(By.cssSelector("#contt1 > div > a"));
					((JavascriptExecutor)driver).executeScript("arguments[0].click();",gengduo);
					type = "需求公告";
				} else if (i == 3) {
					WebElement gengduo = driver.findElement(By.cssSelector(".right_foot > div:first-child > div.zbcg > div > div > a"));
					((JavascriptExecutor)driver).executeScript("arguments[0].click();",gengduo);
					type = "单一来源公示";
				} else {
					WebElement gengduo = driver.findElement(By.cssSelector("#conseven1 > div.cen_new04 > div > a"));
					((JavascriptExecutor)driver).executeScript("arguments[0].click();",gengduo);
					type = "中标成交公告";
				}

				Thread.sleep(4000);

//			List<WebElement> left = driver.findElements(By.cssSelector("ul.type-list > li > a"));
//			List<String> lfurls = new ArrayList<>();
//			List<Map<String,String>> lfurls = new ArrayList<>();


//				String type = lf.getText();
//				lf.click();
//				Thread.sleep(5000);

				isNext = true;



					while (true) {
						List<WebElement> lis = driver.findElements(By.cssSelector("div.yahoo > div.xxei"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						for (WebElement li : lis) {
							String url = front+li.findElement(By.cssSelector("span.lbej > a")).getAttribute("onclick").replace("javascript:location.href='","").replace("';return false;","");
							String title = li.findElement(By.cssSelector("span.lbej > a")).getText();
							String date = li.findElement(By.cssSelector("span.sjej")).getText();
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(2000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("div.xxej").text();

								//加入实体类 入库
								CrawlerEntity insertMap = new CrawlerEntity();
								insertMap.setUrl( url);
								insertMap.setTitle(title);
								insertMap.setCity(CITY);
								insertMap.setType(type);
								insertMap.setDate( date);
								insertMap.setContent( content);
								insertMap.setSource(SOURCE);
								insertMap.setIsCrawl("1");
//								System.out.println("=====================" + insertMap.toString());
//								commonService.insertTable(insertMap, TABLE_NAME);
								commonService.insertTable(insertMap, table_name);
							} else {
								isNext = false;
							}
						}
						if (isNext) {
							log.info(driver.getCurrentUrl());
							WebElement next = null;
							try {
								next = driver.findElements(By.cssSelector(".next")).get(0);
							}catch (Exception e){
								break;
							}
							next.click();
							Thread.sleep(2000);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,CCGPHEILONGJCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CCGPHEILONGJCrawller.class.getName(),"error",e.getMessage());
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


}


package com.bonc.dx.crawler_manage.task.crawler.ym;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 * @date 2021-4-12 16:01:02
 */
@Component
public class CHINABIDDINGCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CHINABIDDINGCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "机电招标网";
	private static final String CITY = "";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "https://www.chinabidding.com/search/proj.htm";
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
		WebDriver driver = null;
		WebDriver driver2 = null;
		try {
			Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			driver = driverPool.get();
//		chromeOptions2.addArguments("--headless --no-sandbox\n".split(" "));
			driver2 = driverPool.get();
//			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
//			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			driver.get(reg);
			Thread.sleep(2000);

			begin_time = days.get("start");
			end_time = days.get("end");
//			end_time = "2021-04-12";
			List<WebElement> types = driver.findElements(By.cssSelector("ul.table-list-items.as-index-list > li:nth-child(2) > a"));
			for (int tj = 0; tj < types.size(); tj++) {
				WebElement type_click = types.get(tj);
				String type = type_click.getText();
				type_click.click();
				Thread.sleep(2000);


				driver.findElement(By.cssSelector("#zoneCode + span + i")).click();
				Thread.sleep(500);
				List<WebElement> areas = driver.findElements(By.cssSelector("#zoneCode + span + i + ul > li"));
				for (int i = 0; i < areas.size(); i++) {
					WebElement area = areas.get(i);
					String city = area.getText();
					//只要确定城市
					if (!city.contains("--")) {
						continue;
					}
					city = city.replace("--", "");
					area.click();
					Thread.sleep(2000);
					isNext = true;


					while (true) {
						List<WebElement> lis = driver.findElements(By.cssSelector("div.as-pager > ul > li"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						for (WebElement li : lis) {
							String url = li.findElement(By.cssSelector("a")).getAttribute("href");
							String title = li.findElement(By.cssSelector("a> h5 >span.txt")).getAttribute("title");
							String date = li.findElement(By.cssSelector("a> h5 >span.time")).getText().replace(" ", "");
							date = date.substring(date.length() - 10, date.length());
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								isNext = true;
								continue;
							} else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前
								isNext = true;
//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(2000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("div.allNoticCont").text();
								if (content == null || content.equals("")) {
									content = doc.select("div.as-article").text();
								}
								if (content == null || content.equals("")) {
									content = doc.text();
								}

								//加入实体类 入库
								CrawlerEntity insertMap = new CrawlerEntity();
								insertMap.setUrl(url);
								insertMap.setTitle(title);
								insertMap.setCity(city);
								insertMap.setType(type);
								insertMap.setDate(date);
								insertMap.setContent(content);
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
							try {
								WebElement next = driver.findElement(By.cssSelector("a.next"));
								next.click();
								Thread.sleep(2000);
							}catch (Exception e){
								System.out.println("没有下一页");
								break;
							}
						} else {
							break;
						}
					}
					driver.findElement(By.cssSelector("#zoneCode + span + i")).click();
					areas = driver.findElements(By.cssSelector("#zoneCode + span + i + ul > li"));
				}
				types = driver.findElements(By.cssSelector("ul.table-list-items.as-index-list > li:nth-child(2) > a"));
			}

			commonService.insertLogInfo(SOURCE,CHINABIDDINGCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CHINABIDDINGCrawller.class.getName(),"error",e.getMessage());
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


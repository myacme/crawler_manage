package com.bonc.dx.crawler_manage.task.crawler.ym;

import com.bonc.dx.crawler_manage.entity.ChromeDriverPro;
import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ProxyChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
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
 * @date 2021-3-3 11:03:52
 */
@Component
public class CQGGZYCrawller implements Crawler {

	@Autowired
	ProxyChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CQGGZYCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "重庆市万州区公共资源交易信息网";
	private static final String CITY = "重庆市";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "https://www.cqggzy.com/jyxx/jyxx-page.html";
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
		ChromeDriverPro driver = null;
		ChromeDriverPro driver2 = null;
		try {
			Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			driver = driverPool.get();
//		chromeOptions2.addArguments("--headless --no-sandbox\n".split(" "));
			driver2 = driverPool.get();
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			driver.get(reg);
			Thread.sleep(5000);

			begin_time = days.get("start");
			end_time = days.get("end");

			isNext = true;
			while (true) {
//						driver.switchTo().frame(driver.findElement(By.cssSelector("#searchForm")));
				List<WebElement> lis = driver.findElements(By.cssSelector("tr.list-tr"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
				if (lis.size() == 0) {
					isNext = false;
				}
				for (WebElement li : lis) {

//							String url = li.findElement(By.cssSelector("dd > p > a")).getAttribute("href");

					String title = li.findElement(By.cssSelector("td.w498 > a")).getAttribute("title");

					String date = li.findElement(By.cssSelector("td.w138")).getText();
					System.out.println("date:" + date);
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
					if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
						//结束时间在爬取到的时间之前 就下一个
						continue;
					}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
						//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

						li.findElement(By.cssSelector("td.w498 > a")).click();
						Thread.sleep(4000);
						//跳转到新页面，获取内容和url 然后跳转回原来页面
						String originalWindow = driver.getWindowHandle();
						// 循环执行，直到找到一个新的窗口句柄
						for (String windowHandle : driver.getWindowHandles()) {
							if(!originalWindow.contentEquals(windowHandle)) {
								driver.switchTo().window(windowHandle);

								String type = driver.findElement(By.cssSelector("div.location")).getText();
								type = type.replace("\"","").replace(" ","").split(">")[4];
								Document doc = Jsoup.parse(driver.getPageSource());
								String content = doc.select("#mainContent").text();

								String url = driver.getCurrentUrl();
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
//								System.out.println("=====================" + insertMap.toString());
//										commonService.insertTable(insertMap, TABLE_NAME);
						commonService.insertTable(insertMap, table_name);

								driver.close();
								driver.switchTo().window(originalWindow);
								Thread.sleep(500);
							}
						}


					} else {
						isNext = false;
					}
				}
				if (isNext) {
					log.info(driver.getCurrentUrl());
					WebElement next = driver.findElements(By.cssSelector("a.next")).get(0);
					String hasnext = driver.findElement(By.cssSelector("span.pg_maxpagenum")).getText();
					if (hasnext.split("/")[0].equals(hasnext.split("/")[1])) {
						break;
					}
					next.click();
					Thread.sleep(1500);
				} else {
					break;
				}
			}


			commonService.insertLogInfo(SOURCE,CQGGZYCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CQGGZYCrawller.class.getName(),"error",e.getMessage());
		} finally {
			if(driver != null){
				driverPool.release(driver);
			}
//			if(driver2 != null){
//				driverPool.release(driver2);
//			}
		}
		System.out.println("exit");
	}

}


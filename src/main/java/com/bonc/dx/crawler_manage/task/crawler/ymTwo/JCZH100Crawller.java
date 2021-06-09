package com.bonc.dx.crawler_manage.task.crawler.ymTwo;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 * @date 2021-5-26 09:43:28
 */
@Component
public class JCZH100Crawller implements Crawler {

	@Autowired(required = false)
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(JCZH100Crawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "精彩纵横";
	private static final String CITY = "其他外网资源";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "http://jczh100.com/index/tendering/li/t/z.html?zhuanti=&hangye=&quyu=&gonggao=&xinxi=&pEndTime=&so=";
	private static String fix = "http://jczh100.com";

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
//		WebDriver driver2 = null;
		try {
			commonService.insertLogInfo(SOURCE,JCZH100Crawller.class.getName(),"begin","");
			Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			String table_name = commonUtil.getTableName();

			begin_time = days.get("start");
			end_time = days.get("end");
//			begin_time = "2021-06-04";
			driver = driverPool.get();
//			driver2 = driverPool.get();

			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
//			driver2.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
			String initUrl = "";
			String type = "";
			for (int i = 0; i < 1; i++) {
				if (i==0){
					initUrl = reg;
				}

				System.out.println(initUrl);
				driver.get(initUrl);
				Thread.sleep(1000);
				isNext = true;
				while (isNext) {
					Document list_doc = Jsoup.parse(driver.getPageSource());
					Elements lis = list_doc.select("ul.pinfolist.pinfolist01 > li");
//				List<WebElement> lis = driver.findElements(By.cssSelector("div.text_con > div.text_row"));
					for (Element li : lis) {
						String title = li.select("div.txt.fl > a.tit > span.soGaoliang").text();
						type = li.select("div.txt.fl > a.tit > span.span1").text();
						String date = li.select("div.date.fl > p.p2").text().replace("发布时间：","");
						String url = li.select("div.txt.fl > a.tit").attr("href");
						url = fix+url;
						String city = li.select("div.txt.fl > div > span.ic01:first-child").text();
						if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
							//结束时间在爬取到的时间之前 就下一个
							isNext = true;
							continue;
						} else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
							//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前
							isNext = true;
							String originalWindow = driver.getWindowHandle();
							JavascriptExecutor js = (JavascriptExecutor) driver;
							js.executeScript("window.open('"+url+"')");
							ArrayList<String> tabs = new ArrayList<String>(driver.getWindowHandles());
							driver.switchTo().window(tabs.get(1));
							Thread.sleep(1000);
							Document doc = Jsoup.parse(driver.getPageSource());
							driver.close();
							driver.switchTo().window(originalWindow);
							String content = doc.select("div.con").text();
							if (content.equals("")){
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
//							System.out.println("=====================" + insertMap.toString());
							commonService.insertTable(insertMap, TABLE_NAME);
//							commonService.insertTable(insertMap, table_name);
						} else {
							isNext = false;
						}


					}

					if (isNext) {
						log.info(driver.getCurrentUrl());
						WebElement next = null;
						try {
							next = driver.findElement(By.cssSelector("section.page-num > a:last-child"));
						}catch (Exception e){
							break;
						}
						next.click();
						Thread.sleep(1000);

					} else {
						break;
					}
				}
			}
            commonService.insertLogInfo(SOURCE,JCZH100Crawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,JCZH100Crawller.class.getName(),"error",e.getMessage());
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


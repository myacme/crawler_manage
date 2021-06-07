package com.bonc.dx.crawler_manage.task.crawler.ymTwo;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.util.ffcode.Api;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.bonc.dx.crawler_manage.util.ffcode.Util.GetUrlImage;

/**
 * @author ym
 * @date 2021-5-26 09:43:28
 */
@Component
public class AHSYZTBCrawller implements Crawler {

	@Autowired(required = false)
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(AHSYZTBCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "安徽双赢招投标咨询有限责任公司";
	private static final String CITY = "其他外网";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "http://www.ahsyztb.com/Home/List/1?cid=3";
	private static String reg2 = "http://www.ahsyztb.com/Home/List/1?cid=4";
	private static String reg3 = "http://www.ahsyztb.com/Home/List/1?cid=10";
	private static String fix = "http://www.ahsyztb.com";

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
			commonService.insertLogInfo(SOURCE,AHSYZTBCrawller.class.getName(),"begin","");
			Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			String table_name = commonUtil.getTableName();

			begin_time = days.get("start");
			end_time = days.get("end");
//			begin_time = "2021-05-16";
			driver = driverPool.get();
//			driver2 = driverPool.get();

			driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
//			driver2.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
			String initUrl = "";
			String type = "";
			for (int i = 0; i < 3; i++) {
				if (i==0){
					initUrl = reg;
					type = "招标公告";
				}else if (i==1){
					initUrl = reg2;
					type = "结果公示";
				}else if (i==2){
					initUrl = reg3;
					type = "变更公告";
				}

				System.out.println(initUrl);
				driver.get(initUrl);
				Thread.sleep(1000);
				isNext = true;
				while (isNext) {
					Document list_doc = Jsoup.parse(driver.getPageSource());
					Elements lis = list_doc.select("div.into_right_content > ul > li");
//				List<WebElement> lis = driver.findElements(By.cssSelector("div.text_con > div.text_row"));
					for (Element li : lis) {
						String title = li.select("a").text();
						String date = li.select("span").text();
						String url = li.select("a").attr("href");
						url = fix+url;

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
							String content = doc.select("div.middle").text();
							if (content.equals("")){
								content = doc.text();
							}
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
//							System.out.println("=====================" + insertMap.toString());
//							commonService.insertTable(insertMap, TABLE_NAME);
							commonService.insertTable(insertMap, table_name);
						} else {
							isNext = false;
						}


					}

					if (isNext) {
						log.info(driver.getCurrentUrl());
						WebElement next = driver.findElement(By.cssSelector("div.pagination > a:nth-last-child(2)"));
						if (next.getAttribute("disabled") != null){
							break;
						}
						next.click();
						Thread.sleep(1000);

					} else {
						break;
					}
				}
			}
            commonService.insertLogInfo(SOURCE,AHSYZTBCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,AHSYZTBCrawller.class.getName(),"error",e.getMessage());
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


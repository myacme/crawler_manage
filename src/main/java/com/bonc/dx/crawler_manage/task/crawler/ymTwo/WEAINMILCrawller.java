package com.bonc.dx.crawler_manage.task.crawler.ymTwo;

import com.bonc.dx.crawler_manage.entity.ChromeDriverPro;
import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.pool.driver.ProxyChromeDriverPool;
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
 * @date 2021-5-26 09:43:28
 */
@Component
public class WEAINMILCrawller implements Crawler {

	@Autowired(required = false)
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(WEAINMILCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全军武器装备采购信息网";
	private static final String CITY = "全国级网站";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "http://www.weain.mil.cn/cggg/jdgg/list.shtml";
	private static String reg2 = "http://www.weain.mil.cn/cggg/jggg/list.shtml";
//
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
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			for (int i = 0; i < 2; i++) {
				if (i==0){
					driver.get(reg);
					continue;
				}else if (i==1){
					driver.get(reg2);
				}
				Thread.sleep(2000);

				begin_time = days.get("start");
				end_time = days.get("end");

				isNext = true;


				while (true) {
					List<WebElement> lis = driver.findElements(By.cssSelector("#list > li"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
					for (WebElement li : lis) {
						String url = li.findElement(By.cssSelector("div.right > span > a")).getAttribute("href");

						String title = li.findElement(By.cssSelector("div.right > span > a")).getText().replace(" ","");
						String type = li.findElement(By.cssSelector("div.left")).getText();
						String date = li.findElement(By.cssSelector("div.right > span.time")).getText();

						if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
							//结束时间在爬取到的时间之前 就下一个
							continue;
						} else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
							//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
							//详情页面爬取content  单独开窗口
							driver2.get(url);
							Thread.sleep(2000);
							Document doc = Jsoup.parse(driver2.getPageSource());
							String content = doc.select("#content").text();

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
//								commonService.insertTable(insertMap, TABLE_NAME);
							commonService.insertTable(insertMap, table_name);
						} else {
							isNext = false;
						}
					}
					if (isNext) {
						log.info(driver.getCurrentUrl());
						WebElement next = driver.findElement(By.cssSelector("#pageBar > ul > li:nth-last-child(2)"));
						if (next.getAttribute("class").contains("disabled")) {
							break;
						}
						next.findElement(By.cssSelector("a")).click();
						Thread.sleep(2000);
					} else {
						break;
					}
				}
			}
			commonService.insertLogInfo(SOURCE,WEAINMILCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,WEAINMILCrawller.class.getName(),"error",e.getMessage());
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


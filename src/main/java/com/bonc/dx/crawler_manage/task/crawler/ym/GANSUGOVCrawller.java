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

/**
 * @author ym
 * @date 2021-3-3 11:03:52
 */
@Component
public class GANSUGOVCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(GANSUGOVCrawller.class);
	private static long ct = 0;
	private static boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "甘肃省公共资源交易局";
	private static final String CITY = "甘肃省";
	private static String begin_time;
	private static String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "https://ggzyjy.gansu.gov.cn/f/newprovince/annogoods/list";
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
//		WebDriver driver2 = driverPool.get();
		try {
			String table_name = commonUtil.getTableName();
			driver.get(reg);
			Thread.sleep(6000);

			end_time = days.get("start");
            begin_time = days.get("end");

//			driver.findElement(By.cssSelector("#stime")).clear();
//			driver.findElement(By.cssSelector("#stime")).sendKeys(begin_time);
//			driver.findElement(By.cssSelector("#etime")).clear();
//			driver.findElement(By.cssSelector("#etime")).sendKeys(end_time);
			List<WebElement> areas = driver.findElements(By.cssSelector("dl.areaType > dt > ul > li > label"));

//			List<WebElement> left = driver.findElements(By.cssSelector("ul.type-list > li > a"));
//			List<String> lfurls = new ArrayList<>();
//			List<Map<String,String>> lfurls = new ArrayList<>();

			for (WebElement area : areas) {

//				String type = lf.getText();
				area.click();
				Thread.sleep(5000);


				List<WebElement> projects = driver.findElements(By.cssSelector("dl.main-projecttype > dt > ul > li"));

				for (WebElement project : projects) {
					if (project.findElement(By.cssSelector("div.icheck-inner")).getText().equals("药品采购") || "display: none;".equals(project.getAttribute("style"))) {
						continue;
					}
					project.click();
					Thread.sleep(3000);

					isNext = true;
					while (true) {
//						driver.switchTo().frame(driver.findElement(By.cssSelector("#searchForm")));
						List<WebElement> lis = driver.findElements(By.cssSelector("dl.sDisclosurLeftConDetailList"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						if (lis.size() == 0) {
							isNext = false;
						}
						for (WebElement li : lis) {

//							String url = li.findElement(By.cssSelector("dd > p > a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("dd > p > a")).getText();
							String type = "";
							String date = li.findElement(By.cssSelector("dd > i")).getText();
							System.out.println("date:" + date);
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							} else if (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

								li.findElement(By.cssSelector("dd > p > a")).click();
								Thread.sleep(5000);
								//跳转到新页面，获取内容和url 然后跳转回原来页面
								String originalWindow = driver.getWindowHandle();
								// 循环执行，直到找到一个新的窗口句柄
								for (String windowHandle : driver.getWindowHandles()) {
									if(!originalWindow.contentEquals(windowHandle)) {
										driver.switchTo().window(windowHandle);
										Document doc = Jsoup.parse(driver.getPageSource());
										String content = doc.text();

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
							WebElement next = driver.findElement(By.cssSelector("ul.pagination.pagination-outline > li:nth-last-child(2)"));
							System.out.println("class:" + next.getAttribute("class"));
							if ("paginate_button next disabled".equals(next.getAttribute("class"))) {
								break;
							}
							next.findElement(By.cssSelector("a")).click();
							Thread.sleep(2000);
						} else {
							break;
						}
					}
				}
			}
			commonService.insertLogInfo(SOURCE,GANSUGOVCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,GANSUGOVCrawller.class.getName(),"error",e.getMessage());
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


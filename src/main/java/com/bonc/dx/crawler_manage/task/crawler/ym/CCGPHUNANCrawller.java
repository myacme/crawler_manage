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
 * @date 2021-4-6 12:13:54
 */
@Component
public class CCGPHUNANCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CCGPHUNANCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "湖南省政府采购网";
	private static final String CITY = "湖南省";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "http://www.ccgp-hunan.gov.cn/page/notice/more.jsp#";
	private static String reg_begin = "http://www.ccgp-hunan.gov.cn/page/notice/notice.jsp?noticeId=";

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
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		try {
			String table_name = commonUtil.getTableName();
			driver.get(reg);
			Thread.sleep(2000);

			begin_time = days.get("start");
//			begin_time = "2020-04-06";
			end_time = days.get("end");
//			end_time = "2021-04-06";

				isNext = true;



					while (true) {
						List<WebElement> lis = driver.findElements(By.cssSelector("#noticeList > table > tbody > tr"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						for (WebElement li : lis) {
							String url = reg_begin+li.findElement(By.cssSelector("td.ggnr_div > a")).getAttribute("onclick").replace("viewNotcieContent(","").replace(")","");
							String title = li.findElement(By.cssSelector("td.ggnr_div > a")).getAttribute("title");
							String date = li.findElement(By.cssSelector("td.riqi")).getText();
							String type = li.findElement(By.cssSelector("td.cggg")).getText();
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(1000);
								driver2.switchTo().frame(driver2.findElement(By.cssSelector("#content")));
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.text();

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

							WebElement next = driver.findElement(By.cssSelector("#pageCount > a:last-child"));
							if (!next.getText().equals("下一页")){
								break;
							}
							next.click();
							Thread.sleep(2000);
						} else {
							break;
						}
					}


			commonService.insertLogInfo(SOURCE,CCGPHUNANCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CCGPHUNANCrawller.class.getName(),"error",e.getMessage());
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


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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 * @date 2021-4-1 11:03:52
 */
@Component
public class CCGPHUBEICrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CCGPHUBEICrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "湖北省政府采购网";
	private static final String CITY = "湖北省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "http://www.ccgp-hubei.gov.cn/notice/cggg/pzgysgg/index_1.html";
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
			driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			begin_time = days.get("start");
			end_time = days.get("end");


//			List<WebElement> left = driver.findElements(By.cssSelector("ul.type-list > li > a"));
//			List<String> lfurls = new ArrayList<>();
//			List<Map<String,String>> lfurls = new ArrayList<>();
			for (int i = 0; i < 14; i++) {
				//更新url和type
				this.setUrl(i);

				driver.get(initUrl);
				Thread.sleep(2000);

				isNext = true;

					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement ul = driver.findElement(By.cssSelector(".news-list-content "));
						try {
							lis = ul.findElements(By.cssSelector("li"));
						}catch (Exception ue){
							isNext = false;
						}
						if (lis.size() == 0 ){
							isNext = false;
						}
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						for (WebElement li : lis) {
							String url = li.findElement(By.cssSelector("a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("a")).getText();
							System.out.println("title:"+title);
							String font = li.findElement(By.cssSelector("a > font")).getText();
							title = title.replace(font,"").replace("[","").replace("]","").replace("\"","");
							String date = li.findElement(By.cssSelector("span")).getText();
							System.out.println("date:"+date);
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(1000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select(".art_con").text();
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
							WebElement next = driver.findElement(By.cssSelector("ul.pagination > li:nth-last-child(3)"));

							if (next.getAttribute("class") != null && next.getAttribute("class").equals("disabled")){
								break;
							}
							next.findElement(By.cssSelector("a")).click();
							Thread.sleep(500);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,CCGPHUBEICrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CCGPHUBEICrawller.class.getName(),"error",e.getMessage());
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


	public void setUrl(int index) {
		key = index;
		switch (index) {
			case 0:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/pzgysgg/index_1.html";
				type = "资格预审公告";
				break;
			case 1:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/pzbgg/index_1.html";
				type = "招标(采购)公告";
				break;
			case 2:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/pzhbgg/index_1.html";
				type = "中标(成交)公告";
				break;
			case 3:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/pgzgg/index_1.html";
				type = "更正公告";
				break;
			case 4:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/pfbgg/index_1.html";
				type = "终止公告";
				break;
			case 5:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/pdylygg/index_1.html";
				type = "单一来源采购公示";
				break;
			case 6:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/pxqgsgg/index_1.html";
				type = "需求公示(征询意见)";
				break;
			case 7:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/czgysgg/index_1.html";
				type = "资格预审公告";
				break;
			case 8:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/czbgg/index_1.html";
				type = "招标(采购)公告";
				break;
			case 9:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/czhbgg/index_1.html";
				type = "中标(成交)公告";
				break;
			case 10:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/cgzgg/index_1.html";
				type = "更正公告";
				break;
			case 11:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/cfbgg/index_1.html";
				type = "终止公告";
				break;
			case 12:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/cdylygg/index_1.html";
				type = "单一来源采购公示";
				break;
			case 13:
				initUrl = "http://www.ccgp-hubei.gov.cn/notice/cggg/cxqgsgg/index_1.html";
				type = "需求公示(征询意见)";
				break;
			default:
				break;
		}
	}


}


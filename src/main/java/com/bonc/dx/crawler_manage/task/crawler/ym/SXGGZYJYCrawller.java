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
 * @date 2021-4-1 11:09:02
 */
@Component
public class SXGGZYJYCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(SXGGZYJYCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全国公共资源交易平台-陕西省";
	private static final String CITY = "陕西省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "http://www.sxggzyjy.cn/jydt/001001/001001001/subPage_jyxx.html";
	private static String reg2 = "http://www.sxggzyjy.cn/jydt/001001/001001002/subPage_jyxx.html";
	private static String reg3 = "http://www.sxggzyjy.cn/jydt/001001/001001003/subPage_jyxx.html";
	private static String reg4 = "http://www.sxggzyjy.cn/jydt/001001/001001004/subPage_jyxx.html";
	private static String reg5 = "http://www.sxggzyjy.cn/jydt/001001/001001006/subPage_jyxx.html";
	private static String reg6 = "http://www.sxggzyjy.cn/jydt/001001/001001008/subPage_jyxx.html";
	private static String reg7 = "http://www.sxggzyjy.cn/jydt/001001/001001013/subPage_jyxx.html";
	private static String reg8 = "http://www.sxggzyjy.cn/jydt/001001/001001014/subPage_jyxx.html";
	private static String reg9 = "http://www.sxggzyjy.cn/jydt/001001/001001012/subPage_jyxx.html";
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
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			begin_time = days.get("start");
			end_time = days.get("end");
			for (int i = 0; i < 9; i++) {
				setUrl(i);
				driver.get(initUrl);
				Thread.sleep(1000);
				isNext = true;
					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement div = driver.findElement(By.cssSelector("#categorypagingcontent"));
						try {
							lis = div.findElements(By.cssSelector("ul.ewb-list > li"));
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

							String title = li.findElement(By.cssSelector("a")).getAttribute("title");
							String date = li.findElement(By.cssSelector("span")).getText();
							System.out.println("date:"+date);
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(1000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("#mainContent").text();
								String type = doc.select("#viewGuid").text();

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
							WebElement next;
							try {
								next = driver.findElements(By.cssSelector(".ewb-page-li.ewb-page-hover")).get(1).findElement(By.cssSelector("a"));
							}catch (Exception enext){
								break;
							}
							next.click();
							Thread.sleep(500);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,SXGGZYJYCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,SXGGZYJYCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = reg1;
				break;
			case 1:
				initUrl = reg2;
				break;
			case 2:
				initUrl = reg3;
				break;
			case 3:
				initUrl = reg4;
				break;
			case 4:
				initUrl = reg5;
				break;
			case 5:
				initUrl = reg6;
				break;
			case 6:
				initUrl = reg7;
				break;
			case 7:
				initUrl = reg8;
				break;
			case 8:
				initUrl = reg9;
				break;
			default:
				break;
		}
	}
}


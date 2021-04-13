package com.bonc.dx.crawler_manage.task.crawler.ym;

import com.alibaba.fastjson.JSONObject;
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
 * @date 2021-3-3 11:03:52
 */
@Component
public class CCGPQINGHAICrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CCGPQINGHAICrawller.class);
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "青海省政府采购网";
	private static final String CITY = "青海省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	public String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement11/index.html";
	private static String reg2 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement1/index.html";
	private static String reg3 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement2/index.html";
	private static String reg4 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement3009/index.html";
	private static String reg5 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement3002/index.html";
	private static String reg6 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement3011/index.html";
	private static String reg7 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement3003/index.html";
	private static String reg8 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement4/index.html";
	private static String reg9 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement3/index.html";
	private static String reg10 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement9999/index.html";
	private static String reg11 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement8888/index.html";
	private static String reg12 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement5/index.html";
	private static String reg13 = "http://www.ccgp-qinghai.gov.cn/ZcyAnnouncement/ZcyAnnouncement8/index.html";
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


//			List<WebElement> left = driver.findElements(By.cssSelector("ul.type-list > li > a"));
//			List<String> lfurls = new ArrayList<>();
//			List<Map<String,String>> lfurls = new ArrayList<>();
			for (int i = 0; i < 13; i++) {
				//更新url和type
				setUrl(i);

				driver.get(initUrl);
				Thread.sleep(3000);

				isNext = true;



					while (true) {
						List<WebElement> lis = driver.findElements(By.cssSelector("li.list-item"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						for (WebElement li : lis) {
							String url = li.findElement(By.cssSelector("a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("a")).getAttribute("title");

							String date = li.findElement(By.cssSelector("span.date")).getText();
							System.out.println("date:"+date);
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(2000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String input = doc.select("input[name=articleDetail]").val();
//							System.out.println(input);
								JSONObject jsonObject = JSONObject.parseObject(input);
								String text = jsonObject.getString("content");
								doc = Jsoup.parse(text);
								String content = doc.text();
								content = content.replaceAll("\"", "");
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
							WebElement next = driver.findElement(By.cssSelector(".paginationjs-next"));
							if (next.getAttribute("title") == null){
								break;
							}
							next.click();
							Thread.sleep(1500);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,CCGPQINGHAICrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CCGPQINGHAICrawller.class.getName(),"error",e.getMessage());
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
				type = "采购意向";
				break;
			case 1:
				initUrl = reg2;
				type = "采购公示";
				break;
			case 2:
				initUrl = reg3;
				type = "公开招标";
				break;
			case 3:
				initUrl = reg4;
				type = "邀请招标公告";
				break;
			case 4:
				initUrl = reg5;
				type = "竞争性谈判公告";
				break;
			case 5:
				initUrl = reg6;
				type = "竞争性磋商公告";
				break;
			case 6:
				initUrl = reg7;
				type = "询价采购公告";
				break;
			case 7:
				initUrl = reg8;
				type = "中标公告";
				break;
			case 8:
				initUrl = reg9;
				type = "变更公告";
				break;
			case 9:
				initUrl = reg10;
				type = "废流标公告";
				break;
			case 10:
				initUrl = reg11;
				type = "资格预审公告";
				break;
			case 11:
				initUrl = reg12;
				type = "合同公告";
				break;
			case 12:
				initUrl = reg13;
				type = "电子卖场公告";
				break;
			default:
				break;
		}
	}


}


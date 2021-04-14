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
 * @date 2021-4-7 14:50:08
 */
@Component
public class JXSGGZYGOVCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(JXSGGZYGOVCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "江西省公共资源交易网";
	private static final String CITY = "江西省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "https://jxsggzy.cn/web/jyxx/002001/002001001/jyxx.html";
	private static String reg2 = "1";
	private static String reg3 = "2";
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
			for (int i = 0; i < 25; i++) {
				//更新url和type
				setUrl(i);

				driver.get(initUrl);
				Thread.sleep(2000);

				driver.findElement(By.cssSelector("#prepostDate")).sendKeys(begin_time);
				driver.findElement(By.cssSelector("#nxtpostDate")).sendKeys(end_time);
				driver.findElement(By.cssSelector("li.clearfix > button")).click();
				Thread.sleep(5000);

				isNext = true;



					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement ul = driver.findElement(By.cssSelector("#showList "));
						try {
							lis = ul.findElements(By.cssSelector("li"));
						}catch (Exception ue){
							isNext = false;
						}
						if (lis.size() == 0 ){
							isNext = false;
						}
						for (WebElement li : lis) {
							String url = li.findElement(By.cssSelector("a")).getAttribute("href");
							String title = li.findElement(By.cssSelector("a")).getText();
							String date = li.findElement(By.cssSelector("span.ewb-list-date")).getText();
							System.out.println("date:"+date);
							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								isNext = true;
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前
								isNext = true;
//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(2000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select(".article-info").text();
								if (content == null || content.equals("")){
									content = doc.text();
								}
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
							WebElement next = null;
							try {
								next = driver.findElement(By.cssSelector("a.next"));
							}catch (Exception e){
								break;
							}
							next.click();
							Thread.sleep(4000);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,JXSGGZYGOVCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,JXSGGZYGOVCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "https://jxsggzy.cn/web/jyxx/002001/002001001/jyxx.html";
				type = "招标公告";
				break;
			case 1:
				initUrl = "https://jxsggzy.cn/web/jyxx/002001/002001004/jyxx.html";
				type = "中标公示";
				break;
			case 2:
				initUrl = "https://jxsggzy.cn/web/jyxx/002002/002002002/jyxx.html";
				type = "招标公告";
				break;
			case 3:
				initUrl = "https://jxsggzy.cn/web/jyxx/002002/002002005/jyxx.html";
				type = "中标公示";
				break;
			case 4:
				initUrl = "https://jxsggzy.cn/web/jyxx/002003/002003001/jyxx.html";
				type = "资格预审公告/招标公告";
				break;
			case 5:
				initUrl = "https://jxsggzy.cn/web/jyxx/002003/002003004/jyxx.html";
				type = "中标候选人公示";
				break;
			case 6:
				initUrl = "https://jxsggzy.cn/web/jyxx/002003/002003005/jyxx.html";
				type = "中标结果公示";
				break;
			case 7:
				initUrl = "https://jxsggzy.cn/web/jyxx/002005/002005001/jyxx.html";
				type = "招标公告";
				break;
			case 8:
				initUrl = "https://jxsggzy.cn/web/jyxx/002005/002005004/jyxx.html";
				type = "结果公示";
				break;
			case 9:
				initUrl = "https://jxsggzy.cn/web/jyxx/002006/002006001/jyxx.html";
				type = "采购公告";
				break;
			case 10:
				initUrl = "https://jxsggzy.cn/web/jyxx/002006/002006002/jyxx.html";
				type = "变更公告";
				break;
			case 11:
				initUrl = "https://jxsggzy.cn/web/jyxx/002006/002006004/jyxx.html";
				type = "结果公示";
				break;
			case 12:
				initUrl = "https://jxsggzy.cn/web/jyxx/002006/002006005/jyxx.html";
				type = "单一来源公示";
				break;
			case 13:
				initUrl = "https://jxsggzy.cn/web/jyxx/002006/002006006/jyxx.html";
				type = "合同公示";
				break;
			case 14:
				initUrl = "https://jxsggzy.cn/web/jyxx/002007/002007001/jyxx.html";
				type = "交易公告";
				break;
			case 15:
				initUrl = "https://jxsggzy.cn/web/jyxx/002007/002007002/jyxx.html";
				type = "成交公示";
				break;
			case 16:
				initUrl = "https://jxsggzy.cn/web/jyxx/002008/002008001/jyxx.html";
				type = "交易公告";
				break;
			case 17:
				initUrl = "https://jxsggzy.cn/web/jyxx/002008/002008002/jyxx.html";
				type = "成交公示";
				break;
			case 18:
				initUrl = "https://jxsggzy.cn/web/jyxx/002009/002009002/jyxx.html";
				type = "成交公示";
				break;
			case 19:
				initUrl = "https://jxsggzy.cn/web/jyxx/002010/002010001/jyxx.html";
				type = "采购公告";
				break;
			case 20:
				initUrl = "https://jxsggzy.cn/web/jyxx/002010/002010002/jyxx.html";
				type = "结果公示";
				break;
			case 21:
				initUrl = "https://jxsggzy.cn/web/jyxx/002013/002013001/jyxx.html";
				type = "交易公告";
				break;
			case 22:
				initUrl = "https://jxsggzy.cn/web/jyxx/002013/002013002/jyxx.html";
				type = "成交公示";
				break;
			case 23:
				initUrl = "https://jxsggzy.cn/web/jyxx/002002/002002006/jyxx.html";
				type = "招标计划";
				break;
			case 24:
				initUrl = "https://jxsggzy.cn/web/jyxx/002006/002006007/jyxx.html";
				type = "采购意向";
				break;
			default:
				break;
		}
	}


}


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
public class QHGGZYJYGOVCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(QHGGZYJYGOVCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全国公共资源交易平台-青海省";
	private static final String CITY = "青海省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001001/001001001/secondPage.html";
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
		Map<String,String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
		WebDriver driver = driverPool.get();
//		chromeOptions2.addArguments("--headless --no-sandbox\n".split(" "));
		WebDriver driver2 = driverPool.get();
		driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
		driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		try {
			String table_name = commonUtil.getTableName();
			begin_time = days.get("start");
			end_time = days.get("end");


//			List<WebElement> left = driver.findElements(By.cssSelector("ul.type-list > li > a"));
//			List<String> lfurls = new ArrayList<>();
//			List<Map<String,String>> lfurls = new ArrayList<>();
			for (int i = 0; i < 19; i++) {
				//更新url和type
				setUrl(i);

				driver.get(initUrl);
				Thread.sleep(2000);

				isNext = true;



					while (true) {
						List<WebElement> lis = driver.findElements(By.cssSelector("#record > tr"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						for (WebElement li : lis) {
							String url = li.findElement(By.cssSelector("td:nth-child(2) > a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("td:nth-child(2) > a")).getAttribute("title");

							String date = li.findElement(By.cssSelector("td:nth-child(4) > span")).getText();
							System.out.println("date:"+date);
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(2000);
								driver2.switchTo().frame(driver2.findElement(By.cssSelector("#iframeList")));
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select(".xiangxiyekuang").text();
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
							List<WebElement> elements = driver.findElements(By.cssSelector("#pager > ul > li"));
							WebElement next = null;
							for (int j = 0; j < elements.size(); j++) {
								if (elements.get(j).getAttribute("class") != null && elements.get(j).getAttribute("class").equals("active")){
									if (j==elements.size()-1){
										isNext = false;
									}else {
										next = elements.get(j+1);
										break;
									}
								}
							}
							if (!isNext){
								break;
							}
							next.click();
							Thread.sleep(1500);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,QHGGZYJYGOVCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,QHGGZYJYGOVCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001001/001001001/secondPage.html";
				type = "招标公告";
				break;
			case 1:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001001/001001002/secondPage.html";
				type = "资格预审公告";
				break;
			case 2:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001001/001001003/secondPage.html";
				type = "澄清变更";
				break;
			case 3:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001001/001001005/secondPage.html";
				type = "中标候选人公示";
				break;
			case 4:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001001/001001006/secondPage.html";
				type = "中标结果公告";
				break;
			case 5:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001001/001001007/secondPage.html";
				type = "终止公告";
				break;
			case 6:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001002/001002001/secondPage.html";
				type = "采购公告";
				break;
			case 7:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001002/001002002/secondPage.html";
				type = "澄清变更";
				break;
			case 8:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001002/001002004/secondPage.html";
				type = "中标公示";
				break;
			case 9:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001002/001002005/secondPage.html";
				type = "终止公告";
				break;
			case 10:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001003/001003001/secondPage.html";
				type = "采购公告";
				break;
			case 11:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001004/001004001/secondPage.html";
				type = "交易公告";
				break;
			case 12:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001004/001004003/secondPage.html";
				type = "结果公示";
				break;
			case 13:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001004/001004004/secondPage.html";
				type = "终止公告";
				break;
			case 14:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001005/001005001/secondPage.html";
				type = "交易公告";
				break;
			case 15:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001005/001005003/secondPage.html";
				type = "结果公示";
				break;
			case 16:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001005/001005004/secondPage.html";
				type = "转让公示";
				break;
			case 17:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001005/001005006/secondPage.html";
				type = "中止公告";
				break;
			case 18:
				initUrl = "https://www.qhggzyjy.gov.cn/ggzy/jyxx/001005/001005005/secondPage.html";
				type = "终止公告";
				break;
			default:
				break;
		}
	}


}


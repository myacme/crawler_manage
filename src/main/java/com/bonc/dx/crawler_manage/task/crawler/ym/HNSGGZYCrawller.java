package com.bonc.dx.crawler_manage.task.crawler.ym;

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
public class HNSGGZYCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(HNSGGZYCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全国公共资源交易平台-湖南省";
	private static final String CITY = "湖南省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "https://www.hnsggzy.com/gczb/index.jhtml";
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
			driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			begin_time = days.get("start");
			end_time = days.get("end");
//			begin_time = "2021-03-26";
//			end_time = "2021-04-06";
			for (int i = 0; i < 13; i++) {
				if (i==5){
					//医药采购类
					continue;
				}
				setUrl(i);
				driver.get(initUrl);
				Thread.sleep(5000);
				isNext = true;
					while (true) {
						Document doc1 = Jsoup.parse(driver.getPageSource());
						Elements lis = doc1.select("ul.article-list2 > li");
						if (lis.size() == 0 ){
							isNext = false;
						}
						for (Element li : lis) {
							String url = li.select(("div.article-list3-t > a")).attr("href");

							String title = li.select("div.article-list3-t > a").text();
							String date = li.select("div.article-list3-t > div.list-times").text();
							System.out.println("date:"+date);
							String type = li.select("div.article-list3-t2 > div:nth-child(2)").text().replace("信息类型：","");

							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(5000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("div.div-article2").text();

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
								next = driver.findElement(By.cssSelector("ul.pages-list > li:nth-last-child(3) > a"));
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
			commonService.insertLogInfo(SOURCE,HNSGGZYCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,HNSGGZYCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "https://www.hnsggzy.com/gczb/index.jhtml";
				break;
			case 1:
				initUrl = "https://www.hnsggzy.com/jygkzfcg/index.jhtml";
				break;
			case 2:
				initUrl = "https://www.hnsggzy.com/jygktd/index.jhtml";
				break;
			case 3:
				initUrl = "https://www.hnsggzy.com/jygkkyq/index.jhtml";
				break;
			case 4:
				initUrl = "https://www.hnsggzy.com/cqjy/index.jhtml";
				break;
			case 5:
				initUrl = "https://www.hnsggzy.com/yycg/index.jhtml";
				break;
			case 6:
				initUrl = "https://www.hnsggzy.com/jygkqt/index.jhtml";
				break;
			case 7:
				initUrl = "https://www.hnsggzy.com/xxxm/index.jhtml";
				break;
			case 8:
				initUrl = "https://www.hnsggzy.com/blwgjztb/index.jhtml";
				break;
			case 9:
				initUrl = "https://www.hnsggzy.com/lqjy/index.jhtml";
				break;
			case 10:
				initUrl = "https://www.hnsggzy.com/pwqjy/index.jhtml";
				break;
			case 11:
				initUrl = "https://www.hnsggzy.com/tpfjy/index.jhtml";
				break;
			case 12:
				initUrl = "https://www.hnsggzy.com/tdzb/index.jhtml";
				break;
			default:
				break;
		}
	}
}


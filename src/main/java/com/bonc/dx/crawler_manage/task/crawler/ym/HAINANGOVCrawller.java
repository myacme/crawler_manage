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
public class HAINANGOVCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(HAINANGOVCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全国公共资源交易平台-海南省";
	private static final String CITY = "海南省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "http://zw.hainan.gov.cn/ggzy/ggzy/jgzbgg/index.jhtml";
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
			for (int i = 0; i < 17; i++) {
				//更新url和type
				setUrl(i);

				driver.get(initUrl);
				Thread.sleep(2000);
				driver.manage().window().maximize();
				isNext = true;

					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement tbody = driver.findElement(By.cssSelector("table.newtable > tbody"));
						try {
							lis = tbody.findElements(By.cssSelector("tr > td > a"));
						}catch (Exception ue){
							isNext = false;
						}
						if (lis.size() == 0 ){
							isNext = false;
						}
						List<WebElement> dates = tbody.findElements(By.cssSelector("tr > td:last-child"));
						for (int j = 0; j < lis.size(); j++) {
							WebElement li = lis.get(j);
							String url = li.getAttribute("href");
							String title = li.getAttribute("title");
							String date = dates.get(j).getText();
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
								String content = doc.select("div.newsTex").text();
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
								next = driver.findElement(By.cssSelector("div.pagesite > div > a:nth-child(3)"));
							}catch (Exception e){
								break;
							}
							if (next.getAttribute("disabled") != null && next.getAttribute("disabled").equals("disabled")){
								break;
							}
							next.click();
							Thread.sleep(4000);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,HAINANGOVCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,HAINANGOVCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/jgzbgg/index.jhtml";
				type = "招标公告";
				break;
			case 1:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/jgzbgs/index.jhtml";
				type = "中标公示";
				break;
			case 2:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/jsqtgg/index.jhtml";
				type = "其它公告";
				break;
			case 3:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/cggg/index.jhtml";
				type = "采购公告";
				break;
			case 4:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/cgzbgg/index.jhtml";
				type = "中标公告";
				break;
			case 5:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/zfcgqtgg/index.jhtml";
				type = "其它公告";
				break;
			case 6:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/gpgg/index.jhtml";
				type = "挂牌公告";
				break;
			case 7:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/cjgg/index.jhtml";
				type = "成交公告";
				break;
			case 8:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/cpgg/index.jhtml";
				type = "已经撤牌";
				break;
			case 9:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/crgg/index.jhtml";
				type = "出让公告";
				break;
			case 10:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/jggg/index.jhtml";
				type = "结果公告";
				break;
			case 11:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/qtgg/index.jhtml";
				type = "其它公告";
				break;
			case 12:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/hycrgg/index.jhtml";
				type = "出让公告";
				break;
			case 13:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/hyzbgg/index.jhtml";
				type = "招标公告";
				break;
			case 14:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/hyjggg/index.jhtml";
				type = "结果公告";
				break;
			case 15:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/yygg/index.jhtml";
				type = "医药公告";
				break;
			case 16:
				initUrl = "http://zw.hainan.gov.cn:80/ggzy/ggzy/yxgg/index.jhtml";
				type = "医械公告";
				break;
			default:
				break;
		}
	}


}


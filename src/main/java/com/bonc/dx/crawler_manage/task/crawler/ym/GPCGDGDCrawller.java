package com.bonc.dx.crawler_manage.task.crawler.ym;

import com.bonc.dx.crawler_manage.entity.ChromeDriverPro;
import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ProxyChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
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
public class GPCGDGDCrawller implements Crawler {

	@Autowired
	ProxyChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(GPCGDGDCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "广东省政府采购网";
	private static final String CITY = "广东省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "http://gpcgd.gd.gov.cn/bsfw/cgxx/cgxxgg";
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
		ChromeDriverPro driver = null;
		ChromeDriverPro driver2 = null;
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
			for (int i = 0; i < 7; i++) {
				//更新url和type
				this.setUrl(i);

				driver.get(initUrl);
				Thread.sleep(2000);

				isNext = true;

					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement div = driver.findElement(By.cssSelector("ul.list"));
						try {
							lis = div.findElements(By.cssSelector(" li"));
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
							String url = li.findElement(By.cssSelector("h3 > a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("h3 > a")).getText();
							String date = li.findElement(By.cssSelector("span.pubDate")).getText().replace("/","-");
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
								String content = doc.select("div.article_con").text();
								if (content == null || content.equals("")) {
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
							Thread.sleep(2000);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,GPCGDGDCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,GPCGDGDCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "http://gpcgd.gd.gov.cn/bsfw/cgxx/cgxxgg";
				type = "采购信息公告";
				break;
			case 1:
				initUrl = "http://gpcgd.gd.gov.cn/bsfw/cgxx/zbjjgs/index.html";
				type = "中标、成交结果公告";
				break;
			case 2:
				initUrl = "http://gpcgd.gd.gov.cn/bsfw/cgxx/pljzcgzl/index.html";
				type = "批量集中采购";
				break;
			case 3:
				initUrl = "http://gpcgd.gd.gov.cn/bsfw/cgxx/cqgzgg/index.html";
				type = "澄清更正公告";
				break;
			case 4:
				initUrl = "http://gpcgd.gd.gov.cn/bsfw/cgxx/cgxxyg/index.html";
				type = "采购信息预告";
				break;
			case 5:
				initUrl = "http://gpcgd.gd.gov.cn/bsfw/cgxx/zgysgg/index.html";
				type = "资格预审公告";
				break;
			case 6:
				initUrl = "http://gpcgd.gd.gov.cn/bsfw/cgxx/zbhxrgs/index.html";
				type = "中标候选人公示";
				break;
			default:
				break;
		}
	}


}


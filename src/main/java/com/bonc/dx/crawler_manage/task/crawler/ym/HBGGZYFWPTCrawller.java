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
public class HBGGZYFWPTCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(HBGGZYFWPTCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全国公共资源交易平台-湖北省";
	private static final String CITY = "湖北省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "https://www.hbggzyfwpt.cn/jyxx/jsgcXmxx";
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
		driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
		driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		try {
			String table_name = commonUtil.getTableName();
			begin_time = days.get("start");
			end_time = days.get("end");


//			List<WebElement> left = driver.findElements(By.cssSelector("ul.type-list > li > a"));
//			List<String> lfurls = new ArrayList<>();
//			List<Map<String,String>> lfurls = new ArrayList<>();
			for (int i = 0; i < 20; i++) {
				//更新url和type
				this.setUrl(i);

				driver.get(initUrl);
				Thread.sleep(1000);
				driver.findElement(By.cssSelector("#publishTime > a:nth-child(2)")).click();
				Thread.sleep(1000);
				isNext = true;

					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement div = driver.findElement(By.cssSelector("div.newListwenzi "));
						try {
							lis = div.findElements(By.cssSelector("table > tbody > tr"));
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
							String url = li.findElement(By.cssSelector("td:first-child > a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("td:first-child > a")).getAttribute("title");
							String date = li.findElement(By.cssSelector("td:last-child")).getText().replace("\"","").replace(" ","").substring(0,10);
							System.out.println("date:"+date);
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(8000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("#detailNeirong").text();
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
							WebElement next = driver.findElements(By.cssSelector("a.provNext")).get(1);
							if (next.getAttribute("onclick") == null){
								break;
							}
							next.click();
							Thread.sleep(500);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,HBGGZYFWPTCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,HBGGZYFWPTCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/jsgcXmxx";
				type = "项目注册";
				break;
			case 1:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/jsgcZbgg";
				type = "招标公告/预审公告";
				break;
			case 2:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/jsgcKbjl";
				type = "开标记录";
				break;
			case 3:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/jsgcpbjggs";
				type = "中标候选人";
				break;
			case 4:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/jsgcZbjggs";
				type = "中标结果";
				break;
			case 5:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/zfcg/cggg";
				type = "采购公告";
				break;
			case 6:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/zfcg/gzsxs";
				type = "更正事项";
				break;
			case 7:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/zfcg/zbjggs";
				type = "采购结果";
				break;
			case 8:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/zfcg/cghts";
				type = "采购合同";
				break;
			case 9:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/tdsyq/cjqr";
				type = "出让公告";
				break;
			case 10:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/tdsyq/crgg";
				type = "成交公示";
				break;
			case 11:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/ypyx/cggg";
				type = "采购公告";
				break;
			case 12:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/cqjy/crgg";
				type = "交易公告";
				break;
			case 13:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/cqjy/cjqr";
				type = "交易结果";
				break;
			case 14:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/pwjy/pwgg";
				type = "交易公告";
				break;
			case 15:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/pwjy/pwjg";
				type = "交易结果";
				break;
			case 16:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/kyqcr/zpgCrgg";
				type = "出让公告公示";
				break;
			case 17:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/kyqcr/zpgCrjggs";
				type = "成交结果公示";
				break;
			case 18:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/qtjy/tdlzjygg";
				type = "交易公告";
				break;
			case 19:
				initUrl = "https://www.hbggzyfwpt.cn/jyxx/qtjy/tdlzjggs";
				type = "交易结果";
				break;
			default:
				break;
		}
	}


}


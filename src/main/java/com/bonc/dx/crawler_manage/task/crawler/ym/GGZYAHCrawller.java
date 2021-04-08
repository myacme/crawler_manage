package com.bonc.dx.crawler_manage.task.crawler.ym;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
public class GGZYAHCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(GGZYAHCrawller.class);
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
	private static String reg1 = "http://ggzy.ah.gov.cn/jsgc/list?tenderProjectType=A01";
	private static String reg2 = "http://ggzy.ah.gov.cn/jsgc/list?tenderProjectType=A07";
	private static String reg3 = "http://ggzy.ah.gov.cn/jsgc/list?tenderProjectType=AAA";
	private static String reg4 = "http://ggzy.ah.gov.cn/jsgc/list?tenderProjectType=A99";
	private static String reg5 = "http://ggzy.ah.gov.cn/zfcg/list";
	private static String reg6 = "http://ggzy.ah.gov.cn/cqjy/list";
	private static String reg7 = "http://ggzy.ah.gov.cn/kyqcr/list";
	private static String reg8 = "http://ggzy.ah.gov.cn/ppp/list";
	private static String reg9 = "http://ggzy.ah.gov.cn/qtjy/list";
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
		driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
		driver2.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
		try {
			String table_name = commonUtil.getTableName();
			begin_time = days.get("start");
			end_time = days.get("end");
			for (int i = 0; i < 9; i++) {
				setUrl(i);
				driver.get(initUrl);
				driver.manage().window().maximize();
//				JavascriptExecutor js = (JavascriptExecutor) driver;
				Thread.sleep(1000);
				List<WebElement> selects = driver.findElements(By.cssSelector("#search1 > div:nth-child(3) > div.btn-group.select_box > ul.dropdown-menu > li > a"));
				for (int j = 0; j < selects.size(); j++) {
					driver.findElements(By.cssSelector("span.caret")).get(0).click();
					WebElement select = selects.get(j);
					String type = select.getText();
					/*String href = select.getAttribute("href").replace("javascript:", "").replace(";", "");
					js.executeScript(href);*/
					select.click();
					Thread.sleep(1000);
					driver.findElement(By.cssSelector("button.btn")).click();
					Thread.sleep(2000);

					isNext = true;
					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement ul = driver.findElement(By.cssSelector("div.list.clear >ul:last-child "));
						try {
							lis = ul.findElements(By.cssSelector("li.list-item"));
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
							String title = li.findElement(By.cssSelector("a > span.title")).getAttribute("title");
							String date = li.findElement(By.cssSelector("span.date")).getText();
							System.out.println("date:"+date);
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

								if (!url.equals("http://ggzy.ah.gov.cn/jsgc/newDetail?guid=716DD52A-5525-4ECA-93C4-8F70FFF090ED&tenderProjectType=A99&bulletinNature=3")){
									continue;
								}
//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(1000);
								try {
									driver2.findElement(By.cssSelector("div.article-mid-title.m-b-40 > a")).click();
									Thread.sleep(2000);
								}catch (Exception ed2){

								}
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.text();

								if (content.contains("此流程暂无信息")){
									content = "此流程暂无信息";
								}
								if (content.contains("Loading")){
									Thread.sleep(5000);
									doc = Jsoup.parse(driver2.getPageSource());
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
							WebElement next = driver.findElement(By.cssSelector("div.mmggxlh > a:last-child"));
							if (next.getAttribute("onclick") == null) {
								break;
							}
							next.click();
							Thread.sleep(500);
						} else {
							break;
						}
					}
					selects = driver.findElements(By.cssSelector("#search1 > div:nth-child(3) > div.btn-group.select_box > ul.dropdown-menu > li > a"));
				}
			}
			commonService.insertLogInfo(SOURCE,GGZYAHCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,GGZYAHCrawller.class.getName(),"error",e.getMessage());
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


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
import java.util.List;
import java.util.Map;

/**
 * @author ym
 * @date 2021-4-14 10:59:56
 */
@Component
public class GDGGZYCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(GDGGZYCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全国公共资源交易平台-广东省";
	private static final String CITY = "广东省";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "http://dsg.gdggzy.org.cn:8080/Bigdata/InformationPublic/mainView.do#";
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
			driver2 = driverPool.get();
//		chromeOptions2.addArguments("--headless --no-sandbox\n".split(" "));
//			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			driver.get(reg);
			Thread.sleep(1000);

			begin_time = days.get("start");
			end_time = days.get("end");

			driver.findElement(By.cssSelector("a.topresearch")).click();
			Thread.sleep(2000);

			List<WebElement> areas = driver.findElements(By.cssSelector("div.busiType > div.vin-fr > div > a"));

			for (WebElement area : areas) {

				if (area.getAttribute("onclick") == null){
					continue;
				}
//				String type = lf.getText();
				area.click();
				Thread.sleep(2000);

				List<WebElement> types = driver.findElements(By.cssSelector("#clearfix_infotype_data_div > div.vin-fr > a"));
				for (WebElement type_clikc : types) {

					String type = type_clikc.getText();
					if (type.equals("全部")){
						continue;
					}
					type_clikc.click();
					Thread.sleep(3000);

					isNext = true;
					while (true) {

//						driver.switchTo().frame(driver.findElement(By.cssSelector("#searchForm")));
						List<WebElement> lis = driver.findElements(By.cssSelector(".table-list2 > tbody >tr"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						if (lis.size() == 0) {
							isNext = false;
						}
						for (WebElement li : lis) {

							String url = li.findElement(By.cssSelector("td.txt-lf > a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("td.txt-lf > a")).getText();
							String date = li.findElement(By.cssSelector("td > i")).getText().substring(0,10);
							System.out.println("date:" + date);
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								isNext = true;
								continue;
							} else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前
								isNext = true;

								driver2.get(url);
								Thread.sleep(2000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("div.BoxB.marb20").text();

								if (content == null || content.equals("")) {
									content = doc.text();
								}
								//加入实体类 入库
								CrawlerEntity insertMap = new CrawlerEntity();
								insertMap.setUrl(url);
								insertMap.setTitle(title);
								insertMap.setCity(CITY);
								insertMap.setType(type);
								insertMap.setDate(date);
								insertMap.setContent(content);
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
							List<WebElement> elements = driver.findElements(By.cssSelector("ul.m-pagination-page > li"));
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
							next.findElement(By.cssSelector("a")).click();
							Thread.sleep(2000);
						} else {
							break;
						}
					}
				}
			}
			commonService.insertLogInfo(SOURCE,GDGGZYCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,GDGGZYCrawller.class.getName(),"error",e.getMessage());
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

}


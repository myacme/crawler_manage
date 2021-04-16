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
public class YNGGZYCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(YNGGZYCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "云南省公共资源交易中心";
	private static final String CITY = "云南省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "https://www.ynggzy.com/";
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
			driver.manage().timeouts().implicitlyWait(15, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			begin_time = days.get("start");
			end_time = days.get("end");


//			List<WebElement> left = driver.findElements(By.cssSelector("ul.type-list > li > a"));
//			List<String> lfurls = new ArrayList<>();
//			List<Map<String,String>> lfurls = new ArrayList<>();
			for (int i = 0; i < 18; i++) {
				if (i==14){
					continue;
				}
				//更新url和type
				this.setUrl(i);
				int num = 1;

				isNext = true;

					while (true) {

						driver.get(initUrl+"?currentPage="+num);
						Thread.sleep(2000);

						List<WebElement> lis = new ArrayList<>();
						WebElement div = driver.findElement(By.cssSelector("#data_tab "));
						try {
							lis = div.findElements(By.cssSelector("tbody > tr"));
						}catch (Exception ue){
							isNext = false;
						}
						if (lis.size() == 0 ){
							isNext = false;
						}
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						int isfirst = 0;
						for (WebElement li : lis) {
							//跳过第一行表头
							if (isfirst == 0){
								isfirst++;
								continue;
							}
							String url = li.findElement(By.cssSelector("td > a")).getAttribute("href");

							String title = li.findElement(By.cssSelector("td > a")).getAttribute("title");
							String date = li.findElement(By.cssSelector("td:last-child")).getText();
							if (i>14){
								//20171018090000
								date = date.substring(0,4)+"-"+date.substring(4,6)+"-"+date.substring(6,8);
							}
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
								String content = doc.select("div.detail_contect").text();
								if (content == null || content.equals("")) {
									content = doc.select("div.page_contect.bai_bg").text();
								}
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
							List<WebElement> elements = driver.findElements(By.cssSelector("div.mmggxlh > a"));
							for (WebElement element : elements) {

								if (element.getText().contains("下一页")){
									next = element;
								}
							}

							if (next == null || next.getAttribute("onclick") == null){
								break;
							}else {
								num++;
							}
							Thread.sleep(500);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,YNGGZYCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,YNGGZYCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "https://www.ynggzy.com/jyxx/jsgcZbgg";
				type = "招标公告";
				break;
			case 1:
				initUrl = "https://www.ynggzy.com/jyxx/jsgcpbjggs";
				type = "评标结果公示";
				break;
			case 2:
				initUrl = "https://www.ynggzy.com/jyxx/jsgcZbjggs";
				type = "https://www.ynggzy.com/jyxx/jsgcZbjggs";
				break;
			case 3:
				initUrl = "https://www.ynggzy.com/jyxx/jsgcZbyc";
				type = "异常公告";
				break;
			case 4:
				initUrl = "https://www.ynggzy.com/jyxx/zfcg/cggg";
				type = "采购公告";
				break;
			case 5:
				initUrl = "https://www.ynggzy.com/jyxx/zfcg/gzsx";
				type = "变更通知";
				break;
			case 6:
				initUrl = "https://www.ynggzy.com/jyxx/zfcg/zbjggs";
				type = "结果公示";
				break;
			case 7:
				initUrl = "https://www.ynggzy.com/jyxx/zfcg/htgs";
				type = "合同公示";
				break;
			case 8:
				initUrl = "https://www.ynggzy.com/jyxx/zfcg/zfcgYcgg";
				type = "异常公告";
				break;
			case 9:
				initUrl = "https://www.ynggzy.com/jyxx/kyqcr/zpgCrgg";
				type = "招拍挂出让公告";
				break;
			case 10:
				initUrl = "https://www.ynggzy.com/jyxx/kyqcr/bytz";
				type = "招拍挂补遗通知";
				break;
			case 11:
				initUrl = "https://www.ynggzy.com/jyxx/kyqcr/zpgCrjggs";
				type = "招拍挂出让结果公示";
				break;
			case 12:
				initUrl = "https://www.ynggzy.com/jyxx/cqjy/crgg";
				type = "挂牌公告";
				break;
			case 13:
				initUrl = "https://www.ynggzy.com/jyxx/cqjy/bytz";
				type = "补遗通知";
				break;
			case 14:
				initUrl = "https://www.ynggzy.com/jyxx/cqjy/cjqr";
				type = "交易结果";
				break;
			case 15:
				initUrl = "https://www.ynggzy.com/jyxx/qtjy/crgg";
				type = "交易公告";
				break;
			case 16:
				initUrl = "https://www.ynggzy.com/jyxx/qtjy/bgtz";
				type = "变更公告";
				break;
			case 17:
				initUrl = "https://www.ynggzy.com/jyxx/qtjy/cjqr";
				type = "结果公示";
				break;
			default:
				break;
		}
	}


}


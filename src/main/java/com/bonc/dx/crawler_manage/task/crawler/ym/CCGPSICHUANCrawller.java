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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 * @date 2021-4-13 10:46:58
 */
@Component
public class CCGPSICHUANCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(CCGPSICHUANCrawller.class);
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "四川省政府采购网";
	private static final String CITY = "四川省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	public String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=recommendBulletinList&rp=25&page=1&moreType=provincebuyBulletinMore&channelCode=cggg";

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
			for (int i = 0; i < 20; i++) {
				//更新url和type
				setUrl(i);

				driver.get(initUrl);
				Thread.sleep(3000);

				isNext = true;



					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement ul = driver.findElement(By.cssSelector("div.info > ul "));
						try {
							lis = ul.findElements(By.cssSelector("li"));
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

							String title = li.findElement(By.cssSelector("a > div.title")).getText();

							String date = li.findElement(By.cssSelector("div.time.curr")).getText().replace("\n","");
							String date1 = date.substring(0,2);
							String date2 = date.substring(2,9);
							date = date2+"-"+date1;
							System.out.println("date:"+date);
							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(2000);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("#myPrintArea").text();
								if (content == null || content.equals("")) {
									content = doc.select("#lines").text();
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
							try {
								WebElement next = driver.findElement(By.cssSelector("a[name=searchPage_next]"));
								next.click();
								Thread.sleep(1500);
							}catch (Exception e){
								break;
							}
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,CCGPSICHUANCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CCGPSICHUANCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=recommendBulletinList&rp=25&page=1&moreType=provincebuyBulletinMore&channelCode=cggg";
				type = "采购公告";
				break;
			case 1:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39add7c40139ae0b9b43100e&chnlNames=\\u4E2D\\u6807\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "中标公告";
				break;
			case 2:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39add7c40139ae0b9b43166&chnlNames=\\u6210\\u4EA4\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "成交公告";
				break;
			case 3:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39add7c40139ae0c09001012&chnlNames=\\u66F4\\u6B63\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "更正公告";
				break;
			case 4:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=99997ecb39b9902a0139b9a72aed0b57&chnlNames=\\u7ADE\\u4EF7\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "竞价采购公告";
				break;
			case 5:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=88887ecb39b9902a0139b9a72aed0b57&chnlNames=\\u7ADE\\u4EF7\\u6210\\u4EA4\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "竞价成交公告";
				break;
			case 6:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39d832560139d85416d70ad5&chnlNames=\\u8D44\\u683C\\u9884\\u5BA1\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "资格预审公告";
				break;
			case 7:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39d832560139d85a2ea70b06&chnlNames=\\u8BE2\\u4EF7\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "采购公告";
				break;
			case 8:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39d832560139d85b10ca0b0a&chnlNames=\\u5355\\u4E00\\u6765\\u6E90\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "采购公告";
				break;
			case 9:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39d832560139d858abff0afe&chnlNames=\\u516C\\u5F00\\u62DB\\u6807\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "采购公告";
				break;
			case 10:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=402886875355b06e01539dde85093fb9&chnlNames=\\u7ADE\\u4E89\\u6027\\u78CB\\u5546\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "采购公告";
				break;
			case 11:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39d832560139d85973c30b02&chnlNames=\\u7ADE\\u4E89\\u6027\\u8C08\\u5224\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "采购公告";
				break;
			case 12:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39d832560139d85806a70afa&chnlNames=\\u9080\\u8BF7\\u62DB\\u6807\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "采购公告";
				break;
			case 13:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817eb738e5e70c0138e66e141c0ea1&chnlNames=\\u4E2D\\u6807\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "中标公告";
				break;
			case 14:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39add7c40139ae0b9sj3166&chnlNames=\\u6210\\u4EA4\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "成交公告";
				break;
			case 15:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39add7c40139ae0b9sj3167&chnlNames=\\u5E9F\\u6807\\u3001\\u6D41\\u6807\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "废标、流标公告";
				break;
			case 16:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817eb738e5e70c0138e670a0260eb3&chnlNames=\\u66F4\\u6B63\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "更正公告";
				break;
			case 17:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=77777ecb39b9902a0139b9a72aed0b57&chnlNames=\\u7ADE\\u4EF7\\u91C7\\u8D2D\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "竞价采购公告";
				break;
			case 18:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=66667ecb39b9902a0139b9a72aed0b57&chnlNames=\\u7ADE\\u4EF7\\u6210\\u4EA4\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "竞价成交公告";
				break;
			case 19:
				initUrl = "http://www.ccgp-sichuan.gov.cn/CmsNewsController.do?method=search&chnlCodes=8a817ecb39add7c40139ae0b9sqitg2&chnlNames=\\u5176\\u5B83\\u516C\\u544A&years=2018&title=&startTime=&endTime=&distin_like=510000&province=510000&city=&town=&provinceText=\\u56DB\\u5DDD\\u7701&cityText=\\u8BF7\\u9009\\u62E9&townText=\\u8BF7\\u9009\\u62E9&pageSize=10&searchResultForm=search_result_anhui.ftl";
				type = "其它公告";
				break;
			default:
				break;
		}
	}


}


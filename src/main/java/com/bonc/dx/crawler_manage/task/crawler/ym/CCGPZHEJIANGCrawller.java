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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author ym
 * @date 2021-4-1 10:12:43
 */
@Component
public class CCGPZHEJIANGCrawller implements Crawler {

	@Autowired
	ProxyChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;

	private static Logger log = LoggerFactory.getLogger(CCGPZHEJIANGCrawller.class);
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "浙江省政府采购网";
	private static final String CITY = "浙江省";
	private  String begin_time;
	private  String end_time;
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg = "https://zfcg.czt.zj.gov.cn/purchaseNotice/index.html";

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
			driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
			driver2.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
			String table_name = commonUtil.getTableName();
			driver.get(reg);
			Thread.sleep(3000);

			begin_time = days.get("start");
			end_time = days.get("end");

			/*Calendar calendar= Calendar.getInstance();
			calendar.add(Calendar.MONTH, -1);
			calendar.set(Calendar.DAY_OF_MONTH, 1);
			begin_time = simpleDateFormat.format(calendar.getTime());
			calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
			end_time = simpleDateFormat.format(calendar.getTime());*/

			List<WebElement> h3s = driver.findElements(By.cssSelector("div.gpoz-nav > div > h3"));
			List<WebElement> uls = driver.findElements(By.cssSelector("div.gpoz-nav > div > ul"));
			for (int i = 0; i < uls.size(); i++) {
				WebElement h3 = h3s.get(i);
				h3.click();
				Thread.sleep(500);
				WebElement ul = uls.get(i);
				List<WebElement> as = ul.findElements(By.cssSelector("li > a"));
				for (WebElement lf : as) {

					String type = lf.getText();
					if (type.equals("采购意向公开")
							|| type.equals("单一来源采购公示")
							|| type.equals("采购文件需求公示")
							|| type.equals("允许采购进口产品公示")
							|| type.equals("资格预审公告")
							|| type.equals("招标公告")
							|| type.equals("非招标公告（竞争性谈判、竞争性磋商、询价公告）")
							|| type.equals("更正公告")
							|| type.equals("中标（成交）结果公告")
							|| type.equals("废标公告")
							|| type.equals("采购合同公告")
							|| type.equals("公款竞争性存放公告")
							|| type.equals("其他非政府采购公告")
							) {
						lf.click();
					} else {
						continue;
					}
					Thread.sleep(2000);
					isNext = true;


					while (true) {
						List<WebElement> lis = driver.findElements(By.cssSelector("#gpozItems > P"));
//						String type = driver.findElement(By.cssSelector("#gonggao_type")).getText();
//						System.out.println("lis.size:"+lis.size());
//						System.out.println("type:"+type);
						for (WebElement li : lis) {
							String url = li.findElement(By.cssSelector(" a")).getAttribute("href");
							String title = li.findElement(By.cssSelector("a > span.underline")).getText();
//						String city = li.findElement(By.cssSelector("a > span.warning")).getText();
//						String limit = li.findElement(By.cssSelector("a > span.warning > span.limit")).getText();
//						city = city.replace(limit,"").replace("\"","").replace("[","").replace("]","").replace("[","").replace("·","");
							String date = li.findElement(By.cssSelector("span.time")).getText().replace("[", "").replace("]", "");
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (date.equals("") || !date.contains("-") || simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (!date.equals("") && (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time)))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(2000);
								driver2.switchTo().frame(driver2.findElement(By.cssSelector("#detail_frame")));
								Thread.sleep(500);
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("#iframe_box").text();
//							String type = doc.select("div.ewb-loc > a:last-child").text();

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
							WebElement next = driver.findElement(By.cssSelector("li.paginationjs-next"));
							if (next.getAttribute("class").equals("paginationjs-next disabled")) {
								break;
							}
							next.click();
							Thread.sleep(1000);
						} else {
							break;
						}
					}
				}
			}
			commonService.insertLogInfo(SOURCE,CCGPZHEJIANGCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,CCGPZHEJIANGCrawller.class.getName(),"error",e.getMessage());
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


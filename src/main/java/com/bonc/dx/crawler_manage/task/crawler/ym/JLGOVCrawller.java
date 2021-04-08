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
public class JLGOVCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(JLGOVCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "吉林省政府公共资源交易中心";
	private static final String CITY = "吉林省";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	private String type = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "http://www.jl.gov.cn/ggzy/zfcg/cggg/";
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
				if (i==14 || i ==16){
					continue;
				}
				//更新url和type
				setUrl(i);

				driver.get(initUrl);
				Thread.sleep(2000);

				isNext = true;
					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement ul = driver.findElement(By.cssSelector("#demoContent "));
						try {
							lis = ul.findElements(By.cssSelector("li"));
						}catch (Exception ue){
							isNext = false;
						}
						if (lis.size() == 0 ){
							isNext = false;
						}
						for (WebElement li : lis) {
							String url = li.findElement(By.cssSelector("a")).getAttribute("href");
							String title = li.findElement(By.cssSelector("a")).getText();
							String date = li.findElement(By.cssSelector("span.ewb-list-date")).getText();
							date = date.replace(" ","").replace(".","-");
							System.out.println("date:"+date);
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
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
								String content = doc.select(".ewb-article-info").text();
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
							List<WebElement> elements = driver.findElements(By.cssSelector("#pages > ul > li"));
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
							Thread.sleep(4000);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,JLGOVCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,JLGOVCrawller.class.getName(),"error",e.getMessage());
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
				initUrl = "http://www.jl.gov.cn/ggzy/zfcg/cggg/";
				type = "采购公告";
				break;
			case 1:
				initUrl = "http://www.jl.gov.cn/ggzy/zfcg/bggg/";
				type = "变更公告";
				break;
			case 2:
				initUrl = "http://www.jl.gov.cn/ggzy/zfcg/zbgg/";
				type = "中标公告";
				break;
			case 3:
				initUrl = "http://www.jl.gov.cn/ggzy/zfcg/zzfbgg/";
				type = "终止（废标）公告";
				break;
			case 4:
				initUrl = "http://www.jl.gov.cn/ggzy/zfcg/htgs/";
				type = "合同公示";
				break;
			case 5:
				initUrl = "http://www.jl.gov.cn/ggzy/zfcg/dylylzgs/";
				type = "单一来源论证公示";
				break;
			case 6:
				initUrl = "http://www.jl.gov.cn/ggzy/gcjs/zbgg/";
				type = "招标公告";
				break;
			case 7:
				initUrl = "http://www.jl.gov.cn/ggzy/gcjs/bggggc/";
				type = "变更公告";
				break;
			case 8:
				initUrl = "http://www.jl.gov.cn/ggzy/gcjs/zbgggc/";
				type = "中标候选人公示";
				break;
			case 9:
				initUrl = "http://www.jl.gov.cn/ggzy/gcjs/zbjggg/";
				type = "中标结果公告";
				break;
			case 10:
				initUrl = "http://www.jl.gov.cn/ggzy/tdsyq/cjxw/";
				type = "出让公告";
				break;
			case 11:
				initUrl = "http://www.jl.gov.cn/ggzy/tdsyq/cjzd/";
				type = "成交宗地";
				break;
			case 12:
				initUrl = "http://www.jl.gov.cn/ggzy/kyq/crgg/";
				type = "出让公告";
				break;
			case 13:
				initUrl = "http://www.jl.gov.cn/ggzy/kyq/crjg/";
				type = "出让结果";
				break;
			case 14:
				initUrl = "http://www.jl.gov.cn/ggzy/gycq/gppl/";
				type = "挂牌披露";
				break;
			case 15:
				initUrl = "http://www.jl.gov.cn/ggzy/gycq/jyjg/";
				type = "交易结果";
				break;
			case 16:
				initUrl = "http://www.jl.gov.cn/ggzy/gqlcqjy/gpplxx/";
				type = "挂牌披露";
				break;
			case 17:
				initUrl = "http://www.jl.gov.cn/ggzy/gqlcqjy/jyjgxx/";
				type = "交易结果";
				break;
			case 18:
				initUrl = "http://www.jl.gov.cn/ggzy/yaoxiecaig/caigougongg/";
				type = "采购公告";
				break;
			default:
				break;
		}
	}


}


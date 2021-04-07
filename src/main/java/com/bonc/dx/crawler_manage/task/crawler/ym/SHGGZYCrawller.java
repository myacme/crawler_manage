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
 * @date 2021-3-24 16:28:29
 */
@Component
public class SHGGZYCrawller implements Crawler {

	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	CommonService commonService;


	private static Logger log = LoggerFactory.getLogger(SHGGZYCrawller.class);
	private  long ct = 0;
	private  boolean isNext = true;
	//测试用表
	private static final String TABLE_NAME = "data_ccgp_henan_info";
	private static final String SOURCE = "全国公共资源交易平台-上海市";
	private static final String CITY = "上海市";
	private  String begin_time;
	private  String end_time;
	private int key = -1;
	private String initUrl = "";
	public static  final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private static String reg1 = "https://www.shggzy.com/jyxxgc/index.jhtml";
	private static String reg2 = "https://www.shggzy.com/jyxxtd/index.jhtml";
	private static String reg3 = "https://www.shggzy.com/jyxxzc/index.jhtml";
	private static String reg4 = "https://www.shggzy.com/jyxxcq/index.jhtml";
	private static String reg5 = "https://www.shggzy.com/jyxxjd/index.jhtml";
	private static String reg6 = "https://www.shggzy.com/jyxxjs/index.jhtml";
	private static String reg7 = "https://www.shggzy.com/jyxxtpf/index.jhtml";
	private static String reg8 = "https://www.shggzy.com/jyxxncys/index.jhtml";
	private static String reg9 = "https://www.shggzy.com/jyxxnc/index.jhtml";
	private static String reg10 = "https://www.shggzy.com/jyxxpm/index.jhtml";
	private static String reg11 = "https://www.shggzy.com/jyxxyp/index.jhtml";
	private static String reg12 = "https://www.shggzy.com/jyxxwzcg/index.jhtml";
	private static String reg13 = "https://www.shggzy.com/jyxxwtl/index.jhtml";
	private static String reg14 = "https://www.shggzy.com/jyxxtyj/index.jhtml";
	private static String reg15 = "https://www.shggzy.com/jyxxny/index.jhtml";
	private static String reg16 = "https://www.shggzy.com/jyxxzf/index.jhtml";
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
			for (int i = 0; i < 16; i++) {
				setUrl(i);
				driver.get(initUrl);
				Thread.sleep(2000);

				isNext = true;
					while (true) {
						List<WebElement> lis = new ArrayList<>();
						WebElement ul = driver.findElement(By.cssSelector("div.gui-title-bottom > ul "));
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
							String url = li.getAttribute("onclick").replace("window.open('","").replace("')","");

							String title = li.findElement(By.cssSelector("span.cs-span2")).getText().replace("\"","").replace(" ","");
							String date = li.findElement(By.cssSelector("span:last-child")).getText();
							System.out.println("date:"+date);
//							String date = doc.select("span.feed-time").text().replace("发布时间：","")
//									.replace("年","-").replace("月","-").replace("日","");
							if (!date.equals("") && simpleDateFormat.parse(end_time).before(simpleDateFormat.parse(date))) {
								//结束时间在爬取到的时间之前 就下一个
								continue;
							}else if (begin_time == null || !simpleDateFormat.parse(date).before(simpleDateFormat.parse(begin_time))) {
								//begin_time为null代表爬取全量的  或者 开始时间 小于等于 爬取到的时间之前

//							DbUtil.insertdataZGZFCGW(insertMap);
								//详情页面爬取content  单独开窗口
								driver2.get(url);
								Thread.sleep(1000+(int) (Math.random()*3000));
								Document doc = Jsoup.parse(driver2.getPageSource());
								String content = doc.select("div.content").text();
								String type = doc.select("div.crumbs_top > span:nth-child(5) > a").text();

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
							WebElement next = driver.findElement(By.cssSelector("#page > div > a:nth-last-child(2)"));
							if ("layui-laypage-next layui-disabled".equals(next.getAttribute("class"))){
								break;
							}
							next.click();
							Thread.sleep(1000);
						} else {
							break;
						}
					}

			}
			commonService.insertLogInfo(SOURCE,SHGGZYCrawller.class.getName(),"success","");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE,SHGGZYCrawller.class.getName(),"error",e.getMessage());
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
			case 9:
				initUrl = reg10;
				break;
			case 10:
				initUrl = reg11;
				break;
			case 11:
				initUrl = reg12;
				break;
			case 12:
				initUrl = reg13;
				break;
			case 13:
				initUrl = reg14;
				break;
			case 14:
				initUrl = reg15;
				break;
			case 15:
				initUrl = reg16;
				break;
			default:
				break;
		}
	}
}


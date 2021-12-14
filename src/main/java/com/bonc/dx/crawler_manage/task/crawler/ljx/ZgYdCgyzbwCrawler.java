/**
 * FileName: ZgYdCgyzbwCrawler
 * <p>
 * Author:   liujixiang
 * <p>
 * Date:     2021/6/15 11:11
 * <p>
 * Description:
 * <p>
 * History:
 *
 * <author>          <time>          <version>          <desc>
 * <p>
 * 作者姓名           修改时间           版本号              描述
 */


package com.bonc.dx.crawler_manage.task.crawler.ljx;


import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.util.Operating;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 〈中国移动采购与招标网〉<br>
 * 〈〉
 *
 * @author ljx
 * @create 2021/6/15
 * @since 1.0.0
 */
@Component
public class ZgYdCgyzbwCrawler implements Crawler {

	@Autowired
	private CommonService commonService;
	@Autowired
	private CommonUtil commonUtil;
	@Autowired(required = false)
	ChromeDriverPool driverPool;

	private static final String CITY = "其他外网资源";
	private static final String SOURCE = "中国移动采购与招标网";
	private static String table = "data_info_test";
	//开始日期
	private static Date startTime;
	//截止日期
	private static Date endTime;
	private String type = "软件招标";
	private int key = -1;
	//是否爬取下一页
	private boolean isNext = true;
	//页码
	private int pageNum = 2;
	public final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public String initUrl = "https://b2b.10086.cn/b2b/main/listVendorNotice.html?noticeType=2#this";

	@Async("taskpool")
	@Override
	public void run() {
		try {
			commonService.insertLogInfo(SOURCE, ZgYdCgyzbwCrawler.class.getName(), "begin", "");
			Map<String, String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			endTime = simpleDateFormat.parse(days.get("end"));
			startTime = simpleDateFormat.parse(days.get("start"));
			table = commonUtil.getTableName();
			getData();
			commonService.insertLogInfo(SOURCE, ZgYdCgyzbwCrawler.class.getName(), "success", "");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE, ZgYdCgyzbwCrawler.class.getName(), "error", e.getMessage());
		}
	}

	public void getData() throws ParseException {
		WebDriver listDriver = driverPool.get();
		Operating operating = new Operating(listDriver, "https://b2b.10086.cn/b2b/main/preIndex.html");
		operating.setCookie(System.getProperty("zgYdCgyzbwCookies"), "b2b.10086.cn");
		WebDriver pageDriver = driverPool.get();
		listDriver.get(initUrl);
		List<WebElement> li = listDriver.findElements(By.cssSelector("#container > div.bg > table > tbody > tr > td.zb_table_td1 > ul > li"));
		for (int i = 0; i < li.size(); i++) {
			li.get(i).click();
			setType(i);
			isNext = true;
			while (isNext) {
				Document list = Jsoup.parse(listDriver.getPageSource());
				Elements trs = list.select("#searchResult > table > tbody > tr[onclick]");
				for (int j = 0; j < trs.size(); j++) {
					String onclick = trs.get(j).attr("onclick");
					String url = "https://b2b.10086.cn/b2b/main/viewNoticeContent.html?noticeBean.id=" + onclick.substring(onclick.indexOf("('") + 1, onclick.indexOf("')"));
					String date = trs.get(j).select("td:last-child").text();
					String title = trs.get(j).select("a").attr("title");
					if (startTime == null || (!"".equals(date) && !simpleDateFormat.parse(date).after(endTime) && !simpleDateFormat.parse(date).before(startTime))) {
						pageDriver.get(url);
						Document page = Jsoup.parse(pageDriver.getPageSource());
						String content = page.select("#tableWrap").text();
						CrawlerEntity crawlerEntity = new CrawlerEntity();
						crawlerEntity.setUrl(url);
						crawlerEntity.setTitle(title);
						crawlerEntity.setCity(CITY);
						crawlerEntity.setType(type);
						crawlerEntity.setDate(date);
						crawlerEntity.setContent(content);
						crawlerEntity.setSource(SOURCE);
						crawlerEntity.setIsCrawl("1");
						commonService.insertTable(crawlerEntity, table);
					} else if (startTime != null && !"".equals(date) && simpleDateFormat.parse(date).before(startTime)) {
						isNext = false;
					}
				}
				try {
					WebElement next = listDriver.findElement(By.cssSelector("#pageid2 > table > tbody > tr > td:nth-child(4) > a"));
					next.click();
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}

	public void setType(int index) {
		switch (index) {
			case 0:
				type = "采购公告";
				break;
			case 1:
				type = "资格预审公告";
				break;
			case 2:
				type = "候选人公示";
				break;
			case 3:
				type = "中选结果公示";
				break;
			case 4:
				type = "单一来源采购信息公告";
				break;
			default:
				break;
		}
	}
}
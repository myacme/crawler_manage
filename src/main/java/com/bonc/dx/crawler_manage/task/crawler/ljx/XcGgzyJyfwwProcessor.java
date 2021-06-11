/**
 * Copyright (C), 2015-2020, XXX有限公司
 * <p>
 * FileName: JszfProcessor
 * <p>
 * Author:   MyAcme
 * <p>
 * Date:     2020/10/29 9:40
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
import com.bonc.dx.crawler_manage.webMagic.downloader.SeleniumDownloadUseDriverPool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * 〈宣城公共资源交易服务网〉<br>
 * 〈〉
 *
 * @author MyAcme
 * @create 2021/6/8
 * @since 1.0.0
 */
@Component
public class XcGgzyJyfwwProcessor implements PageProcessor, Crawler {

	@Autowired
	private CommonService commonService;
	@Autowired
	private CommonUtil commonUtil;
	@Autowired
	SeleniumDownloadUseDriverPool seleniumDownloadUseDriverPool;

	private static final String CITY = "安徽省";
	private static final String SOURCE = "宣城公共资源交易服务网";
	private static String table = "data_info_test";
	//开始日期
	private static Date startTime;
	//截止日期
	private static Date endTime;
	private String type = "";
	private int key = -1;
	//是否爬取下一页
	private boolean isNext = true;
	//页码
	private int pageNum = 2;
	public final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
	public String initUrl = "http://ggzyjy.xuancheng.gov.cn/xcspfront/%s/%s/%s/projectlist.html";

	@Override
	public void process(Page page) {
		Document doc = Jsoup.parse(page.getRawText());
		Elements select = doc.select("tbody.content-list > tr");
		if (select != null && select.size() != 0) {
			for (Element element : select) {
				String url = element.select("a").attr("href");
				String title = element.select("a").attr("title");
				String date = element.select("td:nth-child(3)").text();
				try {
					if (startTime == null || (!"".equals(date) && !simpleDateFormat.parse(date).after(endTime) && !simpleDateFormat.parse(date).before(startTime))) {
						Request request = new Request(new URL(new URL("http://ggzyjy.xuancheng.gov.cn/"), url).toString());
						request.putExtra("date", date);
						request.putExtra("title", title);
						String projectlist = page.getUrl().toString().replace("projectlist", String.valueOf(pageNum));
						page.addTargetRequest(request);
					} else if (startTime != null && !"".equals(date) && simpleDateFormat.parse(date).before(startTime)) {
						isNext = false;
					}
				} catch (ParseException | MalformedURLException e) {
					e.printStackTrace();
				}
			}
			if (isNext) {
				Request request = new Request(page.getUrl().toString().replace("projectlist", String.valueOf(pageNum)));
				page.addTargetRequest(request);
				pageNum++;
			}
		} else {
			String content = doc.select("div.ewb-list-main").text();
			String title = page.getRequest().getExtra("title").toString();
			String date = page.getRequest().getExtra("date").toString();
			String url = page.getRequest().getUrl();
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
		}
	}

	@Override
	public Site getSite() {
		Site site = Site.me()
				.setRetryTimes(3)         // 立即重试
				.setRetrySleepTime(3000)
				.setCycleRetryTimes(5)    // 添加到任务重试
				.setUserAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
				.setSleepTime(1000)
				.setCharset("utf8").setTimeOut(30000);
		return site;
	}

	@Async("taskpool")
	@Override
	public void run() {
		try {
			commonService.insertLogInfo(SOURCE, XcGgzyJyfwwProcessor.class.getName(), "begin", "");
			Map<String, String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			endTime = simpleDateFormat.parse(days.get("end"));
			startTime = simpleDateFormat.parse(days.get("start"));
			table = commonUtil.getTableName();
			for (TType tType : TType.values()) {
				for (City city : City.values()) {
					for (AType aType : AType.values()) {
						XcGgzyJyfwwProcessor bean = new XcGgzyJyfwwProcessor();
						bean.commonService = commonService;
						bean.type = aType.name;
						Spider spider = new Spider(bean);
						spider.addUrl(String.format(bean.initUrl, tType.abridge, tType.code + city.code, tType.code + city.code + aType.code));
						spider.setDownloader(seleniumDownloadUseDriverPool);
						spider.thread(1);
						spider.run();
					}
				}
			}
			commonService.insertLogInfo(SOURCE, XcGgzyJyfwwProcessor.class.getName(), "success", "");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE, XcGgzyJyfwwProcessor.class.getName(), "error", e.getMessage());
		}
	}


	enum TType {
		/**
		 * 交易类型
		 */
		JSGC("建设工程", "016", "jsgc"),
		ZFCG("政府采购", "017", "zfcg"),
		XEYXXM("限额以下项目", "021", "xeyxxm"),
		SHCGXM("其他交易项目", "022", "shcgxm");
		private String name;
		private String code;
		private String abridge;

		TType(String name, String code, String abridge) {
			this.name = name;
			this.code = code;
			this.abridge = abridge;
		}
	}

	enum City {
		/**
		 * 交易类型
		 */
		SBJ("市本级", "001"),
		XCQ("宣州区", "002"),
		LXX("郎溪县", "003"),
		GDS("广德市", "004"),
		NGS("宁国市", "005"),
		JX("泾县", "006"),
		JXX("绩溪县", "007"),
		JDX("旌德县", "008");
		private String name;
		private String code;

		City(String name, String code) {
			this.name = name;
			this.code = code;
		}
	}

	enum AType {
		/**
		 * 公告类型
		 */
		ZBGG("招标公告", "001"),
		ZHBGG("中标公告", "004");
		private String name;
		private String code;

		AType(String name, String code) {
			this.name = name;
			this.code = code;
		}
	}
}
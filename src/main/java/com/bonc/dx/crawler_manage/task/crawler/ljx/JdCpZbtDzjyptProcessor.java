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
import com.bonc.dx.crawler_manage.webMagic.downloader.HttpClientDownloader;
import com.bonc.dx.crawler_manage.webMagic.downloader.SeleniumDownloadUseDriverPool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.model.HttpRequestBody;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.utils.HttpConstant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 〈机电产品招标投标电子交易平台〉<br>
 * 〈〉
 *
 * @author MyAcme
 * @create 2021/6/9
 * @since 1.0.0
 */
@Component
public class JdCpZbtDzjyptProcessor implements PageProcessor, Crawler {

	@Autowired
	private CommonService commonService;
	@Autowired
	private CommonUtil commonUtil;

	private static final String CITY = "其他外网资源";
	private static final String SOURCE = "机电产品招标投标电子交易平台";
	private static String table = "data_info_test";
	private static WebDriver driver;
	private Param param;
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
	public String initUrl = "https://www.chinabidding.com/search/proj.htm";

	@Override
	public void process(Page page) {
		Document doc = Jsoup.parse(page.getRawText());
		Elements select = doc.select("#lab-show > div.as-floor-normal > div.span-f > div > ul > li");
		if (select != null && select.size() != 0) {
			for (Element element : select) {
				String url = element.select("a").attr("href");
				String title = element.select("span.txt").attr("title");
				String date = element.select("span.time").text();
				date = date.substring(date.lastIndexOf("：") + 1).replaceAll(" ", "");
				try {
					if (startTime == null || (!"".equals(date) && !simpleDateFormat.parse(date).after(endTime) && !simpleDateFormat.parse(date).before(startTime))) {
						Request request = new Request(url);
						request.putExtra("date", date);
						request.putExtra("title", title);
						page.addTargetRequest(request);
					} else if (startTime != null && !"".equals(date) && simpleDateFormat.parse(date).before(startTime)) {
						isNext = false;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if (isNext) {
				HashMap<String, Object> map = new HashMap<>(4);
				map.put("poClass", param.poClass);
				map.put("infoClassCodes", param.infoClassCodes);
				map.put("currentPage", String.valueOf(pageNum));
				Request request = new Request(initUrl);
				request.setMethod(HttpConstant.Method.POST);
				request.setRequestBody(HttpRequestBody.form(map, "utf-8"));
				page.addTargetRequest(request);
				pageNum++;
			}
		} else {
			String content = doc.select("#lab-show > div.as-floor-normal > div.span-f").text();
			String title = page.getRequest().getExtra("title").toString();
			String date = page.getRequest().getExtra("date").toString();
			String url = page.getRequest().getUrl();
			CrawlerEntity crawlerEntity = new CrawlerEntity();
			crawlerEntity.setUrl(url);
			crawlerEntity.setTitle(title);
			crawlerEntity.setCity(CITY);
			crawlerEntity.setType(param.name);
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
			commonService.insertLogInfo(SOURCE, JdCpZbtDzjyptProcessor.class.getName(), "begin", "");
			Map<String, String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			endTime = simpleDateFormat.parse(days.get("end"));
			startTime = simpleDateFormat.parse(days.get("start"));
			table = commonUtil.getTableName();
			for (Param param : Param.values()) {
				HashMap<String, Object> map = new HashMap<>(4);
				map.put("poClass", param.poClass);
				map.put("infoClassCodes", param.infoClassCodes);
				map.put("currentPage", "1");
				JdCpZbtDzjyptProcessor bean = new JdCpZbtDzjyptProcessor();
				bean.commonService = commonService;
				bean.param = param;
				Spider spider = new Spider(bean);
				Request request = new Request(bean.initUrl);
				request.setMethod(HttpConstant.Method.POST);
				request.setRequestBody(HttpRequestBody.form(map, "utf-8"));
				spider.addRequest(request);
				spider.setDownloader(new HttpClientDownloader());
				spider.thread(2);
				spider.run();
			}
			commonService.insertLogInfo(SOURCE, JdCpZbtDzjyptProcessor.class.getName(), "success", "");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE, JdCpZbtDzjyptProcessor.class.getName(), "error", e.getMessage());
		}
	}

	enum Param {
		/**
		 *
		 */
		ZBGG("招标公告", "BidNotice", "0105"),
		BGGG("变更公告", "BidChange", "0106"),
		PSJG("评审结果", "BidResult", "0107"),
		ZBJG("中标公告", "BidResult", "0108");
		private String name;
		private String poClass;
		private String infoClassCodes;

		Param(String name, String poClass, String infoClassCodes) {
			this.name = name;
			this.poClass = poClass;
			this.infoClassCodes = infoClassCodes;
		}
	}
}
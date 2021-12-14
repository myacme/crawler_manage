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
import com.bonc.dx.crawler_manage.service.CommonService;
import com.bonc.dx.crawler_manage.task.crawler.CommonUtil;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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
 * 〈软件招标-软件产品网〉<br>
 * 〈〉
 *
 * @author MyAcme
 * @create 2021/6/14
 * @since 1.0.0
 */
@Component
public class RjZbRjCpwProcessor implements PageProcessor, Crawler {

	@Autowired
	private CommonService commonService;
	@Autowired
	private CommonUtil commonUtil;

	private static final String CITY = "其他外网资源";
	private static final String SOURCE = "软件招标-软件产品网";
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
	public String initUrl = "https://www.soft78.com/businesslist_24_%d.htm";

	@Override
	public void process(Page page) {
		Document doc = Jsoup.parse(page.getRawText());
		Elements select = doc.select("body > div.container > div.right_content_list > div > ul > li");
		if (select != null && select.size() != 0) {
			for (Element element : select) {
				String url = element.select("span.content_title > a").attr("href");
				String title = element.select("span.content_title > a").attr("title");
				String date = element.select("span.content_date").text();
				try {
					if (startTime == null || (!"".equals(date) && !simpleDateFormat.parse(date).after(endTime) && !simpleDateFormat.parse(date).before(startTime))) {
						Request request = new Request(new URL(new URL(initUrl), url).toString());
						request.putExtra("date", date);
						request.putExtra("title", title);
						page.addTargetRequest(request);
					} else if (startTime != null && !"".equals(date) && simpleDateFormat.parse(date).before(startTime)) {
						isNext = false;
					}
				} catch (MalformedURLException | ParseException e) {
					e.printStackTrace();
				}
			}
			if (isNext) {
				Request request = new Request(String.format(initUrl, pageNum));
				page.addTargetRequest(request);
				pageNum++;
			}
		} else {
			String content = doc.select("div.article_content > div.s_w > div.s_w_c").text();
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
				.setCharset("UTF-8").setTimeOut(30000);
		return site;
	}

	@Async("taskpool")
	@Override
	public void run() {
		try {
			commonService.insertLogInfo(SOURCE, RjZbRjCpwProcessor.class.getName(), "begin", "");
			Map<String, String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			endTime = simpleDateFormat.parse(days.get("end"));
			startTime = simpleDateFormat.parse(days.get("start"));
			table = commonUtil.getTableName();
			RjZbRjCpwProcessor bean = new RjZbRjCpwProcessor();
			bean.commonService = commonService;
			Spider spider = new Spider(bean);
			spider.addUrl(String.format(initUrl,1));
			spider.thread(2);
			spider.run();
			commonService.insertLogInfo(SOURCE, RjZbRjCpwProcessor.class.getName(), "success", "");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE, RjZbRjCpwProcessor.class.getName(), "error", e.getMessage());
		}
	}
}
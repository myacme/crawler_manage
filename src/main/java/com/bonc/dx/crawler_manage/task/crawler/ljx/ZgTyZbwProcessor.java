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
 * 〈中国通用招标网〉<br>
 * 〈〉
 *
 * @author MyAcme
 * @create 2021/6/4
 * @since 1.0.0
 */
@Component
public class ZgTyZbwProcessor implements PageProcessor, Crawler {

	@Autowired
	private CommonService commonService;
	@Autowired
	private CommonUtil commonUtil;

	private static final String CITY = "直管政务网站";
	private static final String SOURCE = "中国通用招标网";
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
	public String initUrl = "";
	public static final String URL1 = "http://www.china-tender.com.cn/zbgg/index%s.jhtml";
	public static final String URL2 = "http://www.china-tender.com.cn/zgys/index%s.jhtml";
	public static final String URL3 = "http://www.china-tender.com.cn/bggg/index%s.jhtml";
	public static final String URL4 = "http://www.china-tender.com.cn/jggg/index%s.jhtml";

	@Override
	public void process(Page page) {
		Document doc = Jsoup.parse(page.getRawText());
		Elements select = doc.select("div.List2.Top5 > ul > li");
		if (select != null && select.size() != 0) {
			for (Element element : select) {
				String url = element.select("a").attr("href");
				String title = element.select("a").text();
				String date = element.select(" p > span.Gray.Right").text();
				date = date.substring(date.indexOf("：")+1);
				try {
					if (startTime == null || (!"".equals(date) && !simpleDateFormat.parse(date).after(endTime) && !simpleDateFormat.parse(date).before(startTime))) {
						Request request = new Request(new URL(new URL("http://www.china-tender.com.cn/"),url).toString());
						request.putExtra("date", date);
						request.putExtra("title", title);
						page.addTargetRequest(request);
					} else if (startTime != null && !"".equals(date) && simpleDateFormat.parse(date).before(startTime)) {
						isNext = false;
					}
				} catch (ParseException | MalformedURLException e) {
					e.printStackTrace();
				}
			}
			if (isNext) {
				Request request = new Request(String.format(initUrl, "_" + pageNum));
				page.addTargetRequest(request);
				pageNum++;
			}
		} else {
			if (page.getRequest().getExtra("title") != null) {
				//爬取详情页
				String content = doc.select("div.ConBox1").text();
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

	public void setUrl(int index) {
		key = index;
		switch (index) {
			case 0:
				type = "招标公告";
				initUrl = URL1;
				break;
			case 1:
				type = "资格预审";
				initUrl = URL2;
				break;
			case 2:
				type = "变更公告";
				initUrl = URL3;
				break;
			case 3:
				type = "结果公告";
				initUrl = URL4;
				break;
			default:
				break;
		}
	}

	@Async("taskpool")
	@Override
	public void run() {
		try {
			commonService.insertLogInfo(SOURCE, ZgTyZbwProcessor.class.getName(), "begin", "");
			Map<String, String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			endTime = simpleDateFormat.parse(days.get("end"));
			startTime = simpleDateFormat.parse(days.get("start"));
			table = commonUtil.getTableName();
			for (int i = 0; i < 4; i++) {
				ZgTyZbwProcessor bean = new ZgTyZbwProcessor();
				bean.setUrl(i);
				bean.commonService = commonService;
				Spider spider = new Spider(bean);
				spider.addUrl(String.format(bean.initUrl, ""));
				spider.thread(2);
				spider.run();
			}
			commonService.insertLogInfo(SOURCE, ZgTyZbwProcessor.class.getName(), "success", "");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE, ZgTyZbwProcessor.class.getName(), "error", e.getMessage());
		}
	}
}
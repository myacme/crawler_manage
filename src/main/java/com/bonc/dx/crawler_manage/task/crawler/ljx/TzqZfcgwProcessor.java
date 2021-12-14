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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.pool.driver.DriverPool;
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
import us.codecraft.webmagic.selector.Json;
import us.codecraft.webmagic.utils.HttpConstant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 〈通州区政府采购网〉<br>
 * 〈〉
 *
 * @author MyAcme
 * @create 2021/6/16
 * @since 1.0.0
 */
@Component
public class TzqZfcgwProcessor implements PageProcessor, Crawler {

	@Autowired
	private CommonService commonService;
	@Autowired
	private CommonUtil commonUtil;
	@Autowired
	private ChromeDriverPool chromeDriverPool;

	private static final String CITY = "北京市";
	private static final String SOURCE = "通州区政府采购网";
	private static String table = "data_info_test";
	private static WebDriver driver;
	private Param param;
	HashMap<String, Object> map;
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
	public String initUrl = "http://zfcg.zwyun.bjtzh.gov.cn/zfcg/getMoreBulletin";
	public String pageUrl = "http://zfcg.zwyun.bjtzh.gov.cn/zfcg/cmsInfo/%s";

	@Override
	public void process(Page page) {
		Json json = page.getJson();
		String pageJson = json.jsonPath("$..obj.list.results").toString();
		if (pageJson != null) {
			JSONArray jsonArray = JSONArray.parseArray(pageJson);
			for (int i = 0; i < jsonArray.size(); i++) {
				JSONObject o = jsonArray.getJSONObject(i);
				String id = o.get("id").toString();
				String title = o.get("mainTitle").toString();
				String date = o.get("postDateStr").toString();
				try {
					if (startTime == null || (!"".equals(date) && !simpleDateFormat.parse(date).after(endTime) && !simpleDateFormat.parse(date).before(startTime))) {
						String url = String.format(pageUrl, id);
						driver.get(url);
						Document parse = Jsoup.parse(driver.getPageSource());
						String content = parse.select("body > div.main > div.news").text();
						if ("".equals(content)) {
							content = parse.select("#cmsBulletinDivForm").text();
						}
						if ("".equals(content)) {
							content = parse.select("#abolishBulletinDivForm").text();
						}
						if ("".equals(content)) {
							content = parse.select("#cmsBulletinDivFormNew").text();
						}
						if ("".equals(content)) {
							content = parse.select("#cmsBulletinDivFormAlone").text();
						}
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
					} else if (startTime != null && !"".equals(date) && simpleDateFormat.parse(date).before(startTime)) {
						isNext = false;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			if (isNext) {
				map.put("pageSize", String.valueOf(pageNum));
				Request request = new Request(initUrl);
				request.setMethod(HttpConstant.Method.POST);
				request.setRequestBody(HttpRequestBody.form(map, "utf-8"));
				page.addTargetRequest(request);
				pageNum++;
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

	@Async("taskpool")
	@Override
	public void run() {
		try {
			commonService.insertLogInfo(SOURCE, TzqZfcgwProcessor.class.getName(), "begin", "");
			Map<String, String> days = commonUtil.getDays(Thread.currentThread().getStackTrace()[1].getClassName());
			endTime = simpleDateFormat.parse(days.get("end"));
			startTime = simpleDateFormat.parse(days.get("start"));
			driver = chromeDriverPool.get();
			table = commonUtil.getTableName();
			for (Param param : Param.values()) {
				HashMap<String, Object> map = new HashMap<>(8);
				map.put("type", param.type);
				map.put("orderByColumn", "postDate desc");
				map.put("pageNum", "1");
				map.put("pageSize", "12");
				map.put("menu", "");
				TzqZfcgwProcessor bean = new TzqZfcgwProcessor();
				bean.commonService = commonService;
				bean.param = param;
				bean.map = map;
				Spider spider = new Spider(bean);
				Request request = new Request(bean.initUrl);
				request.setMethod(HttpConstant.Method.POST);
				request.setRequestBody(HttpRequestBody.form(map, "utf-8"));
				spider.addRequest(request);
				spider.thread(2);
				spider.run();
			}
			commonService.insertLogInfo(SOURCE, TzqZfcgwProcessor.class.getName(), "success", "");
		} catch (Exception e) {
			e.printStackTrace();
			commonService.insertLogInfo(SOURCE, TzqZfcgwProcessor.class.getName(), "error", e.getMessage());
		} finally {
			chromeDriverPool.release(driver);
		}
	}

	enum Param {
		/**
		 *
		 */
		TZTG("通知通告", "tztg"),
		ZBGG("招标公告", "zbgg"),
		GZGG("更正公告", "gzgg"),
		DYLY("单一来源公示", "dyly"),
		ZBCJ("中标成交", "zbcj"),
		CGHT("采购合同", "cght"),
		QTGG("其他公告", "qtgg");
		private String name;
		private String type;

		Param(String name, String type) {
			this.name = name;
			this.type = type;
		}
	}
}
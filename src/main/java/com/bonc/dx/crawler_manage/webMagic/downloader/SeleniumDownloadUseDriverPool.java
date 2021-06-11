package com.bonc.dx.crawler_manage.webMagic.downloader;


import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriver.Options;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.Downloader;
import us.codecraft.webmagic.selector.Html;
import us.codecraft.webmagic.selector.PlainText;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * 下载器
 * @author MyAcme
 * @date 2021/6/8
 */
@Component
public class SeleniumDownloadUseDriverPool implements Downloader, Closeable {

	ChromeDriverPool driverPool;
	WebDriver webDriver;
	private Logger logger = Logger.getLogger(this.getClass());
	private int sleepTime = 0;
	private int poolSize = 1;

	public void init() {
		System.getProperties().setProperty("webdriver.chrome.driver", System.getProperty("chromedriver"));
	}

	@Autowired
	public SeleniumDownloadUseDriverPool(ChromeDriverPool driverPool) {
		this.driverPool = driverPool;
	}

	public SeleniumDownloadUseDriverPool setSleepTime(int sleepTime) {
		this.sleepTime = sleepTime;
		return this;
	}

	@Override
	public Page download(Request request, Task task) {
		//linux环境注释init()方法
		init();
		webDriver = driverPool.get();
		this.logger.info("downloading page " + request.getUrl());
		webDriver.get(request.getUrl());
		try {
			Thread.sleep((long) this.sleepTime);
		} catch (InterruptedException var9) {
			var9.printStackTrace();
		}
		Options manage = webDriver.manage();
		Site site = task.getSite();
		if (site.getCookies() != null) {
			Iterator var6 = site.getCookies().entrySet().iterator();
			while (var6.hasNext()) {
				Entry<String, String> cookieEntry = (Entry) var6.next();
				Cookie cookie = new Cookie((String) cookieEntry.getKey(), (String) cookieEntry.getValue());
				manage.addCookie(cookie);
			}
		}
		WebElement webElement = webDriver.findElement(By.xpath("/html"));
		String content = webElement.getAttribute("outerHTML");
		Page page = new Page();
		page.setRawText(content);
		page.setHtml(new Html(content, request.getUrl()));
		page.setUrl(new PlainText(request.getUrl()));
		page.setRequest(request);
		driverPool.release(webDriver);
		return page;
	}


	@Override
	public void setThread(int thread) {
		this.poolSize = thread;
	}

	@Override
	public void close() {
	}
}

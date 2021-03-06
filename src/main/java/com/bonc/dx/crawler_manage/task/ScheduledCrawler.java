package com.bonc.dx.crawler_manage.task;

import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.pool.driver.InitSystemProperty;
import com.bonc.dx.crawler_manage.pool.driver.ProxyChromeDriverPool;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.util.SpringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@Component
public class ScheduledCrawler {
	@Autowired
	ProxyChromeDriverPool proxyDriverPool;

	@Autowired
	ChromeDriverPool driverPool;

	@Autowired
	InitSystemProperty initSystemProperty;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Scheduled(cron = "0 20 0 * * ?")
	public void execute() throws UnsupportedEncodingException {
		System.out.println("====================定时启动=================");
//		proxyDriverPool.init(initSystemProperty);
		driverPool.init(initSystemProperty);
		ApplicationContext applicationContext = SpringUtil.getApplicationContext();
		Map<String, Crawler> crawlers = applicationContext.getBeansOfType(Crawler.class);
		for (Map.Entry entry : crawlers.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
			//需要测试某一个就放开if条件 匹配类的bean
//            if(entry.getKey().equals("GANSUGOVCrawller")){
                crawlers.get(entry.getKey()).run();
//            }
		}
	}
}

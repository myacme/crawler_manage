package com.bonc.dx.crawler_manage.task;

import com.bonc.dx.crawler_manage.pool.driver.ChromeDriverPool;
import com.bonc.dx.crawler_manage.pool.driver.InitSystemProperty;
import com.bonc.dx.crawler_manage.pool.driver.ProxyChromeDriverPool;
import com.bonc.dx.crawler_manage.service.TaskConfService;
import com.bonc.dx.crawler_manage.task.crawler.Crawler;
import com.bonc.dx.crawler_manage.util.SpringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class InitStartCrawler implements CommandLineRunner {
	@Autowired
	TaskConfService taskConfService;
	@Autowired
	ChromeDriverPool driverPool;
	@Autowired
	ProxyChromeDriverPool proxyDriverPool;
	@Autowired
	InitSystemProperty initSystemProperty;

	@Override
	public void run(String... args) {
		Map<String, Crawler> crawlers = SpringUtil.getApplicationContext().getBeansOfType(Crawler.class);
		driverPool.init(initSystemProperty);
		for (Map.Entry entry : crawlers.entrySet()) {
			System.out.println(entry.getKey() + ":" + entry.getValue());
			//需要测试某一个就放开if条件 匹配类的bean
			if ("tzqZfcgwProcessor".equals(entry.getKey())) {
				crawlers.get(entry.getKey()).run();
			}
		}
	}
}

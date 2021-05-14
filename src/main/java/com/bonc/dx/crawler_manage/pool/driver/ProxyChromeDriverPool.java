package com.bonc.dx.crawler_manage.pool.driver;

import com.bonc.dx.crawler_manage.entity.ChromeDriverPro;
import com.bonc.dx.crawler_manage.pool.ip.IpGetAndRelease;
import com.bonc.dx.crawler_manage.pool.ip.ProxyZipUtil;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 阻塞队列实现一个池
 * 存放代理driver
 */
@Component
@DependsOn("initSystemProperty")
public class ProxyChromeDriverPool implements DriverPool {

	private static Logger log = LoggerFactory.getLogger(ProxyChromeDriverPool.class);

	//todo 正式的池大小需扩大
	private LinkedBlockingQueue<ChromeDriverPro> queue = new LinkedBlockingQueue();
//	ProxyChromeDriverPool(InitSystemProperty initSystemProperty) {
//		initSystemProperty.init();
//		ProxyZipUtil proxyZipUtil = new ProxyZipUtil();
//		int size = Integer.parseInt(System.getProperty("proxy_driver_size"));
//		for (int i = 0; i < size; i++) {
//			add(proxyZipUtil, i);
//		}
//		log.info("代理Driver池 初始化完成， 数量 : {}", queue.size());
//	}


	public void init(InitSystemProperty initSystemProperty) {
		//申请ip前释放所有ip和driver
		while (!queue.isEmpty()) {
			//driver出池
			ChromeDriverPro driver = queue.poll();
			driver.quit();
			//释放ip
			IpGetAndRelease.releaseIp(System.getProperty("ipReleaseUrl") + ProxyZipUtil.getIp(driver.getIndex()));
		}
		initSystemProperty.init();
		ProxyZipUtil proxyZipUtil = new ProxyZipUtil();
		int size = Integer.parseInt(System.getProperty("proxy_driver_size"));
		for (int i = 0; i < size; i++) {
			add(proxyZipUtil, i);
		}
		log.info("代理Driver池 定时初始化完成， 数量 : {}", queue.size());
	}

	/**
	 * 阻塞获取，获取不到时，等待
	 *
	 * @return
	 */
	@Override
	public ChromeDriverPro get() {
		ChromeDriverPro driver = null;
		//获取driver前验证ip是否可用
		while (true) {
			try {
				driver = queue.take();
				try {
					//验证ip是否可用
					driver.get("https://www.baidu.com");
					log.info("代理driver池 获取一个资源，现有 : {}", queue.size());
					return driver;
				} catch (Exception e) {
					e.printStackTrace();
					log.info("代理driver池 一个资源失效，现有 : {}", queue.size());
					//重新申请ip
					add(new ProxyZipUtil(), driver.getIndex());
					log.info("代理driver池 新增一个资源，现有 : {}", queue.size());
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}


	private void add(ProxyZipUtil proxyZipUtil, int i) {
		//设置代理
		String path = System.getProperty("proxyDir") + "proxy" + i + ".zip";
		String ip = proxyZipUtil.setProxyIp(i);
		ChromeOptions chromeOptions = new ChromeOptions();
		File file = proxyZipUtil.getZip(path);
		chromeOptions.addExtensions(file);
		//最大化
		chromeOptions.addArguments("start-maximized");
		/*设置新的代理模式*/
		String proxyIpAndPort = ip;
		Proxy proxy = new Proxy();
		proxy.setHttpProxy(ip).setFtpProxy(ip).setSslProxy(ip);
		chromeOptions.setProxy(proxy);
//		DesiredCapabilities cap = new DesiredCapabilities();
//		cap.setCapability(CapabilityType.ForSeleniumServer.AVOIDING_PROXY, true);
//		cap.setCapability(CapabilityType.ForSeleniumServer.ONLY_PROXYING_SELENIUM_TRAFFIC, true);
//		cap.setCapability(ChromeOptions.CAPABILITY, chromeOptions);
//		System.setProperty("http.nonProxyHosts", "localhost");
//		cap.setCapability(CapabilityType.PROXY, proxy);
		ChromeDriverPro driver = new ChromeDriverPro(chromeOptions);
		//设置压缩文件下标
		driver.setIndex(i);
		//设置5分钟超时时间
		driver.manage().timeouts().pageLoadTimeout(300, TimeUnit.SECONDS);
		queue.offer(driver);
	}

	/**
	 * release 释放driver资源
	 *
	 * @param driver
	 */
	@Override
	public void release(ChromeDriverPro driver) {
		queue.offer(driver);
		log.info("代理Driver池 释放一个资源，剩余 : {}", queue.size());
	}
}

/**
 * FileName: WebDriverPro
 * <p>
 * Author:   liujixiang
 * <p>
 * Date:     2021/5/10 16:28
 * <p>
 * Description:
 * <p>
 * History:
 *
 * <author>          <time>          <version>          <desc>
 * <p>
 * 作者姓名           修改时间           版本号              描述
 */


package com.bonc.dx.crawler_manage.entity;


import lombok.Data;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;
import java.util.Set;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author ljx
 * @create 2021/5/10
 * @since 1.0.0
 */

@Data
public class WebDriverPro implements WebDriver {

	private String ip;

	@Override
	public void get(String s) {
	}

	@Override
	public String getCurrentUrl() {
		return null;
	}

	@Override
	public String getTitle() {
		return null;
	}

	@Override
	public List<WebElement> findElements(By by) {
		return null;
	}

	@Override
	public WebElement findElement(By by) {
		return null;
	}

	@Override
	public String getPageSource() {
		return null;
	}

	@Override
	public void close() {
	}

	@Override
	public void quit() {
	}

	@Override
	public Set<String> getWindowHandles() {
		return null;
	}

	@Override
	public String getWindowHandle() {
		return null;
	}

	@Override
	public TargetLocator switchTo() {
		return null;
	}

	@Override
	public Navigation navigate() {
		return null;
	}

	@Override
	public Options manage() {
		return null;
	}
}
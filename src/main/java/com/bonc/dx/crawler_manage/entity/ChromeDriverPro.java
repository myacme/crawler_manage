/**
 * FileName: ChromeDriverPro
 * <p>
 * Author:   liujixiang
 * <p>
 * Date:     2021/5/10 17:03
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


import org.openqa.selenium.Capabilities;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author ljx

 * @create 2021/5/10

 * @since 1.0.0

 */


public class ChromeDriverPro extends ChromeDriver {
	public ChromeDriverPro() {
		super();
	}

	public ChromeDriverPro(ChromeDriverService service) {
		super(service);
	}

	/** @deprecated */
	@Deprecated
	public ChromeDriverPro(Capabilities capabilities) {
		super(capabilities);
	}

	public ChromeDriverPro(ChromeOptions options) {
		super(options);
	}

	public ChromeDriverPro(ChromeDriverService service, ChromeOptions options) {
		this(service, (Capabilities)options);
	}

	/** @deprecated */
	@Deprecated
	public ChromeDriverPro(ChromeDriverService service, Capabilities capabilities) {
		super(service,capabilities);
	}

	private int index;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}
}
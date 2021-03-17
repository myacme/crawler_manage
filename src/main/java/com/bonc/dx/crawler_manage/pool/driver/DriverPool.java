package com.bonc.dx.crawler_manage.pool.driver;

import org.openqa.selenium.WebDriver;

public interface DriverPool {

    WebDriver get();

    void release(WebDriver driver);

}

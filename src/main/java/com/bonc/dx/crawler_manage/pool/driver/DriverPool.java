package com.bonc.dx.crawler_manage.pool.driver;

import com.bonc.dx.crawler_manage.entity.ChromeDriverPro;

public interface DriverPool {

    ChromeDriverPro get();

    void release(ChromeDriverPro driver);

}

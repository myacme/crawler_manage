package com.bonc.dx.crawler_manage.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CrawlerEntity {
    private  String title;
    private String sample;
    private String content;
    private String url;
    private String source;
    private String type;
    private String date;
    private String city;
    private String isCrawl;

}

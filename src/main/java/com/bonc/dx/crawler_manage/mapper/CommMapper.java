package com.bonc.dx.crawler_manage.mapper;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommMapper {

    void insert(CrawlerEntity entity);
}

package com.bonc.dx.crawler_manage.mapper;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CommMapper {

    void insert(CrawlerEntity entity);

    void insertLogInfo(@Param("web_name") String web_name,@Param("program_name") String program_name,@Param("sign") String sign,@Param("message") String message);

}

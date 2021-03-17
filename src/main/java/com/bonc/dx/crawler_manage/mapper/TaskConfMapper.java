package com.bonc.dx.crawler_manage.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface TaskConfMapper {

    String getConf(@Param("key") String key, @Param("className") String className);


}

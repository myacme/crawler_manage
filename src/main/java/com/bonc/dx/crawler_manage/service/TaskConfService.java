package com.bonc.dx.crawler_manage.service;

import com.bonc.dx.crawler_manage.mapper.TaskConfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskConfService {

    private final TaskConfMapper taskConfMapper;

    public String getConfValue(String key, String className){
        return taskConfMapper.getConf(key, className);
    }

    public void insertNameLog(String time, String table_name){
        taskConfMapper.insertNameLog(time, table_name);
    }


}

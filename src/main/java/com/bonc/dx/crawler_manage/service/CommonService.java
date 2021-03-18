package com.bonc.dx.crawler_manage.service;

import com.bonc.dx.crawler_manage.entity.CrawlerEntity;
import com.bonc.dx.crawler_manage.mapper.CommMapper;
import com.bonc.dx.crawler_manage.mapper.TaskConfMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final CommMapper commMapper;

    public void insert(CrawlerEntity crawlerEntity){
        try {
            commMapper.insert(crawlerEntity);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void insertLogInfo(String web_name,String program_name,String sign,String message){
        try {
            commMapper.insertLogInfo(web_name,program_name,sign,message);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}

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


}

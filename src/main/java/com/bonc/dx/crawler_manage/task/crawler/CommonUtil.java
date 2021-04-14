package com.bonc.dx.crawler_manage.task.crawler;



import com.bonc.dx.crawler_manage.mapper.TaskConfMapper;
import com.bonc.dx.crawler_manage.service.TaskConfService;
import com.bonc.dx.crawler_manage.util.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CommonUtil {
    @Autowired
    TaskConfService taskConfService;
    private static final String START_DATE = "2021-01-01";

    public  Map<String,String> getDays(String threadName){
        String start = "";
        String end = "";
        //是否全量,指定天数,默认m-1

        String type = taskConfService.getConfValue("is_all", threadName);
        //type = 1 全量
        if("1".equals(type)){
            start = START_DATE;
            end = taskConfService.getConfValue("end_time",threadName);
        //type = 0,指定日期
        }else if("0".equals(type)){
            start = taskConfService.getConfValue("begin_time",threadName);
            end = taskConfService.getConfValue("end_time",threadName);
        //默认m-1
        } else{
            start = end = DateUtil.getDate("yyyy-MM-dd");
        }

        Map<String,String> days = new HashMap<>(2);
        days.put("start",start);
        days.put("end",end);

        return days;
    }

    //获取当前需要存的表名(启动时间跑前一天的数据，所以表名后缀是昨天的日期)
    public String getTableName(){
        return taskConfService.getConfValue("table_name_prefix","all")+DateUtil.getDate("yyyyMMdd");
        /*String time = DateUtil.getDate("yyyy-MM-dd HH:mm:ss");
        String table_name = taskConfService.getConfValue("table_name_prefix", "all") + DateUtil.getDate("yyyyMMdd");
        taskConfService.insertNameLog(time,table_name);
        return table_name;*/
    }

}

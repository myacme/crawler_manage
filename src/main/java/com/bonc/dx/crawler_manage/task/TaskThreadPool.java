package com.bonc.dx.crawler_manage.task;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class TaskThreadPool {

    /**
     * 线程池1，同时可执行的爬虫
     * @return
     */
    @Bean("taskpool")
    public Executor myExecutor1(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(1024);
        executor.setKeepAliveSeconds(100);
        //线程名字前缀
        executor.setThreadNamePrefix("TaskPool-");
        // setRejectedExecutionHandler：当pool已经达到max size的时候，如何处理新任务
        // CallerRunsPolicy：不在新线程中执行任务，而是由调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new MyRejected());
        executor.initialize();
        return executor;

    }

    /**
     * 线程池2，同时执行的爬虫内部的子任务线程
     * @return
     */
    @Bean("taskpool2")
    public Executor myExecutor2(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(100);
        executor.setThreadNamePrefix("TaskPool2-");
        executor.setRejectedExecutionHandler(new MyRejected());
        executor.initialize();
        return executor;

    }
}


class MyRejected implements RejectedExecutionHandler{
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        System.out.println("线程已满，做些其它操作");
        throw new RejectedExecutionException("Task " + r.toString() +
                " rejected from " +
                executor.toString());
    }
}

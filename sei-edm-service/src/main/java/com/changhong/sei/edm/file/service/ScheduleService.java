package com.changhong.sei.edm.file.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2020-12-08 18:07
 */
@Component
@EnableScheduling
public class ScheduleService {
    private static final Logger LOG = LoggerFactory.getLogger(ScheduleService.class);

    @Autowired
    private FileService fileService;

    /**
     * 0/5 * * * * ?  每隔5秒触发一次
     * 0 0/2 * * * ?  每隔2分钟触发一次
     * 0 15 1 * * ?   每天1:15触发
     */
    @Scheduled(cron = "0 17 2 * * ?")
    public void cron() {
        LOG.info("启动临时文件清理");
        try {
            fileService.removeInvalidDocuments();
            LOG.info("临时文件清理完毕");
        } catch (Exception e) {
            LOG.error("临时文件清理异常", e);
        }
    }
}

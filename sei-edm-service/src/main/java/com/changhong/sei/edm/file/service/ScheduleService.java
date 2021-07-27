package com.changhong.sei.edm.file.service;

import com.changhong.sei.edm.common.FileConstants;
import com.changhong.sei.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

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

    /**
     * 0 0/2 * * * ?  每隔2分钟触发一次
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    public void removeDir() {
        String dirPath = FileConstants.FILE_PATH;
        LOG.info("启动{}磁盘临时文件清理", dirPath);
        try {
            File dir = new File(dirPath);
            if (dir.exists()) {
                File file;
                long currentTime = System.currentTimeMillis();
                List<String> fileList = FileUtils.getAllFile(dirPath, Boolean.TRUE);
                for (String filePath : fileList) {
                    file = new File(filePath);
                    if (FileConstants.check(file, currentTime)) {
                        FileUtils.forceDelete(file);
                    }
                }
            }
            LOG.info("临时文件清理完毕");
        } catch (Exception e) {
            LOG.error("临时文件清理异常", e);
        }
    }
}

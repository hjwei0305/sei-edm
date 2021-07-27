package com.changhong.sei.edm.common;

import com.changhong.sei.core.log.LogUtil;

import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 实现功能：
 *
 * @author 马超(Vision.Mac)
 * @version 1.0.00  2021-07-27 18:12
 */
public class FileConstants {
    private static final long EXPIRED = 3600 * 1000;
    public static final String FILE_PATH;

    private static final ConcurrentMap<String, Long> MAP_;

    static {
        MAP_ = new ConcurrentHashMap<>();
        FILE_PATH = System.getProperty("user.dir").concat(File.separator).concat("file").concat(File.separator);

        LogUtil.bizLog("文件临时目录: {}", FILE_PATH);
    }

    public static void add(File file) {
        if (file != null && file.exists()) {
            MAP_.put(file.getAbsolutePath(), System.currentTimeMillis());
        }
    }

    public static boolean check(File file, long currentTime) {
        if (file != null && file.exists()) {
            if (currentTime <= 0) {
                currentTime = System.currentTimeMillis();
            }

            String fileName = file.getAbsolutePath();
            Long time = MAP_.get(fileName);
            if (time == null) {
                return true;
            } else if (time + EXPIRED < currentTime) {
                MAP_.remove(fileName);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static Set<String> getAll() {
        return MAP_.keySet();
    }
}

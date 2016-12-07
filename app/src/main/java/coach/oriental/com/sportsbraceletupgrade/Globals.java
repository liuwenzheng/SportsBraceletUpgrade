package coach.oriental.com.sportsbraceletupgrade;

import android.app.Application;
import android.os.Environment;

import com.elvishew.xlog.LogConfiguration;
import com.elvishew.xlog.LogLevel;
import com.elvishew.xlog.XLog;
import com.elvishew.xlog.printer.AndroidPrinter;
import com.elvishew.xlog.printer.Printer;
import com.elvishew.xlog.printer.file.FilePrinter;
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator;

import java.io.File;

/**
 * Created by wenzheng.liu on 2016/12/7.
 */

public class Globals extends Application {
    private static final String appFolder = "iFitLog";
    private static String PATH_LOGCAT;


    @Override
    public void onCreate() {
        super.onCreate();
        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {// 优先保存到SD卡中
            PATH_LOGCAT = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + File.separator + appFolder;
        } else {// 如果SD卡不存在，就保存到本应用的目录下
            PATH_LOGCAT = getApplicationContext().getFilesDir().getAbsolutePath()
                    + File.separator + appFolder;
        }
        Printer filePrinter = new FilePrinter.Builder(PATH_LOGCAT)
                .fileNameGenerator(new DateFileNameGenerator())
                .build();
        LogConfiguration config = new LogConfiguration.Builder().tag("Upgrade").build();
        XLog.init(LogLevel.ALL, config, new AndroidPrinter(), filePrinter);
    }
}

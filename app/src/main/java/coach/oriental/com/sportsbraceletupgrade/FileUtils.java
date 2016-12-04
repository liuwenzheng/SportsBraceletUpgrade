package coach.oriental.com.sportsbraceletupgrade;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class FileUtils {
    public static String getPath(Context context, Uri uri) {

        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            } catch (Exception e) {
                // Eat it
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    private static InputStream in;

    public static byte[] readFile(String filePath) {
        File file = new File(filePath);
        try {
            if (in == null) {
                in = new FileInputStream(file);
            }
            if (in.available() > 0) {
                byte b[] = new byte[17];
                in.read(b);
                return b;
            } else {
                in.close();
                in = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getVersion(String filePath) throws Exception {
        File file = new File(filePath);
        try {
            String fileName = file.getName().split(".")[0];

            String[] s = new String[3];
            s[0] = fileName.split("_")[1];
            s[1] = fileName.split("_")[2];
            s[2] = fileName.split("_")[3];
            return s;
        } catch (Exception e) {
            throw e;
        }
    }
}
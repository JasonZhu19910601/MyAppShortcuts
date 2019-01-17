package com.leading.myappshortcuts;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;


/**
 * @author Zj
 * @package com.leading.myappshortcuts
 * @fileName Utils
 * @date 2019/1/16 16:12
 * @describe TODO
 * @org Leading.com(北京理正软件)
 * @email 2856211755@qq.com
 * @computer Administrator
 */
public class Utils {
    public Utils() {
    }

    public static void showToast(Context context, String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }
}

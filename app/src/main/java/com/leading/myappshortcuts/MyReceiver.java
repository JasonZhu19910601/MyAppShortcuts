package com.leading.myappshortcuts;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * @author Zj
 * @package com.leading.myappshortcuts
 * @fileName MyReceiver
 * @date 2019/1/17 10:55
 * @describe TODO
 * @org Leading.com(北京理正软件)
 * @email 2856211755@qq.com
 * @computer Administrator
 */
public class MyReceiver extends BroadcastReceiver {
    private static final String TAG = "MyReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive: " + intent);
        if (intent.getAction().equals(Intent.ACTION_LOCALE_CHANGED)) {
            //刷新所有快捷方式以更新标签。
            //（但现在快捷方式标签不包含本地化字符串。）
            new ShortcutHelper(context).refreshShortcuts(true);
        }
    }
}

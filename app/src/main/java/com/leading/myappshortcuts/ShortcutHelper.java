package com.leading.myappshortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.PersistableBundle;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * @author Zj
 * @package com.leading.myappshortcuts
 * @fileName ShortcutHelper
 * @date 2019/1/16 16:22
 * @describe TODO
 * @org Leading.com(北京理正软件)
 * @email 2856211755@qq.com
 * @computer Administrator
 */
public class ShortcutHelper {
    private static final String TAG = "ShortcutHelper";
    private static final String EXTRA_LAST_REFRESH = "com.leading.myappshortcuts";
    private static final long REFRESH_INTERVAL_MS = 60 * 60 * 1000;
    private final Context mContext;
    private final ShortcutManager mShortcutManager;

    public ShortcutHelper(Context context) {
        this.mContext = context;
        this.mShortcutManager = mContext.getSystemService(ShortcutManager.class);
    }

    public void maybeRestoreAllDynamicShortcuts() {
        if (mShortcutManager.getDynamicShortcuts().size() == 0) {
            // 注意：如果此应用程序始终应该具有动态快捷方式，则在此处发布它们。
            // 注意当应用程序在新设备上“恢复”时，所有动态快捷方式将*不会*被恢复，但固定快捷方式*会*。
        }
    }

    public void reportShortcutUsed(String id) {
        mShortcutManager.reportShortcutUsed(id);
    }

    /**
     * 在与ShortcutManager交互时使用此选项可显示一致的错误消息。
     *
     * @param r
     */
    public void callShortcutManager(BooleanSupplier r) {
        try {
            if (!r.getAsBoolean()) {
                Utils.showToast(mContext, "Call to ShortcutManager is rate-limited");
            }
        } catch (Exception e) {
            Log.e(TAG, "callShortcutManager: ", e);
            Utils.showToast(mContext, "Error while calling ShortcutManager: " + e);
        }
    }

    /**
     * 返回此应用程序自身的所有可变快捷方式。
     *
     * @return
     */
    public List<ShortcutInfo> getShortcuts() {
        //加载可变动态快捷方式和固定快捷方式，并将它们放入单个列表中，删除重复项。
        ArrayList<ShortcutInfo> ret = new ArrayList<>();
        final HashSet<String> seenKeys = new HashSet<>();
        // 检查现有的快捷方式
        for (ShortcutInfo dynamicShortcut : mShortcutManager.getDynamicShortcuts()) {
            Log.e(TAG, "getShortcuts: dynamicShortcut-->" + dynamicShortcut + "，isImmutable-> " + dynamicShortcut.isImmutable());
            if (!dynamicShortcut.isImmutable()) {
                ret.add(dynamicShortcut);
                seenKeys.add(dynamicShortcut.getId());
            }
        }

        for (ShortcutInfo pinnedShortcut : mShortcutManager.getPinnedShortcuts()) {
            Log.e(TAG, "getShortcuts: pinnedShortcut-->" + pinnedShortcut + "，isImmutable-> " + pinnedShortcut.isImmutable());
            if (!pinnedShortcut.isImmutable() && !ret.contains(pinnedShortcut.getId())) {
                ret.add(pinnedShortcut);
                seenKeys.add(pinnedShortcut.getId());
            }
        }

        return ret;
    }

    /**
     * activity 开始时调用。查找已推送的快捷方式并刷新它们（但刷新部分尚未实现…）。
     *
     * @param force
     */
    public void refreshShortcuts(boolean force) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                Log.e(TAG, "doInBackground: refreshingShortcuts...");
                final long now = System.currentTimeMillis();
                final long staleThreshold = force ? now : now - REFRESH_INTERVAL_MS;

                //检查所有现有的动态和固定快捷方式，如果它们的上次刷新时间早于某个阈值，则更新它们。
                List<ShortcutInfo> updateList = new ArrayList<>();
                for (ShortcutInfo shortcut : getShortcuts()) {
                    if (shortcut.isImmutable()) {
                        continue;
                    }

                    final PersistableBundle extras = shortcut.getExtras();
                    if (extras != null && extras.getLong(EXTRA_LAST_REFRESH) >= staleThreshold) {
                        // 快捷方式仍然新鲜。
                        continue;
                    }

                    final ShortcutInfo.Builder b = new ShortcutInfo.Builder(mContext, shortcut.getId());
                    setSiteInformation(b, shortcut.getIntent().getData());
                    setExtras(b);

                    updateList.add(b.build());
                }

                // 调用更新
                if (updateList.size() > 0) {
                    callShortcutManager(() -> mShortcutManager.updateShortcuts(updateList));
                }
                return null;
            }
        }.execute();
    }

    private ShortcutInfo.Builder setExtras(ShortcutInfo.Builder b) {
        PersistableBundle persistableBundle = new PersistableBundle();
        persistableBundle.putLong(EXTRA_LAST_REFRESH, System.currentTimeMillis());
        b.setExtras(persistableBundle);
        return b;
    }

    /**
     * @param b
     * @param uri
     */
    private ShortcutInfo.Builder setSiteInformation(ShortcutInfo.Builder b, Uri uri) {
        //获取实际站点<title>并使用它。
        //将当前区域设置为接受语言以获取本地化标题。
        b.setShortLabel(uri.getHost());
        b.setLongLabel(uri.toString());

        Bitmap bm = fetchFavicon(uri);
        if (bm != null) {
            b.setIcon(Icon.createWithBitmap(bm));
        } else {
            b.setIcon(Icon.createWithResource(mContext, R.drawable.link));
        }
        return b;
    }

    /**
     * @param uri
     * @return
     */
    private Bitmap fetchFavicon(Uri uri) {
        final Uri iconUri = uri.buildUpon().path("favicon").build();
        Log.i(TAG, "Fetching favicon from: " + iconUri);

        InputStream is;
        BufferedInputStream bis;

        try {
            URLConnection conn = new URL(uri.toString()).openConnection();
            conn.connect();
            is = conn.getInputStream();
            bis = new BufferedInputStream(is, 8192);
            return BitmapFactory.decodeStream(bis);
        } catch (IOException e) {
            Log.w(TAG, "Failed to fetch favicon from " + iconUri, e);
            return null;
        }
    }

    private String normalizeUrl(String urlAsString) {
        if (urlAsString.startsWith("http://") || urlAsString.startsWith("https://")) {
            return urlAsString;
        } else {
            return "http://" + urlAsString;
        }
    }

    public void addWebsiteShortcut(String urlAsString) {
        final String urlFinal = urlAsString;
        callShortcutManager(() -> {
            final ShortcutInfo shortcutInfo = createShortcutForUrl(normalizeUrl(urlAsString));
            return mShortcutManager.addDynamicShortcuts(Arrays.asList(shortcutInfo));
        });
    }

    public void removeShortcut(ShortcutInfo shortcut) {
        mShortcutManager.removeDynamicShortcuts(Arrays.asList(shortcut.getId()));
    }

    public void disableShortcut(ShortcutInfo shortcut) {
        mShortcutManager.disableShortcuts(Arrays.asList(shortcut.getId()));
    }

    public void enableShortcut(ShortcutInfo shortcut) {
        mShortcutManager.enableShortcuts(Arrays.asList(shortcut.getId()));
    }


    private ShortcutInfo createShortcutForUrl(String normalizeUrl) {
        Log.i(TAG, "createShortcutForUrl: " + normalizeUrl);

        final ShortcutInfo.Builder builder = new ShortcutInfo.Builder(mContext, normalizeUrl);

        final Uri uri = Uri.parse(normalizeUrl);
        builder.setIntent(new Intent(Intent.ACTION_VIEW, uri));

        setSiteInformation(builder, uri);
        setExtras(builder);

        return builder.build();
    }
}

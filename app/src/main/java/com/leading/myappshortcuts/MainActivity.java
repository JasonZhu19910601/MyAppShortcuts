package com.leading.myappshortcuts;

import android.app.ListActivity;
import android.content.Context;
import android.content.pm.ShortcutInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends ListActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private static final String ID_ADD_WEBSITE = "add_website";
    private static final String ACTION_ADD_WEBSITE = "com.leading.myappshortcuts.ADD_WEBSITE";
    private static final List<ShortcutInfo> EMPTY_LIST = new ArrayList<>();
    private MyAdapter mAdapter;
    private ShortcutHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mHelper = new ShortcutHelper(this);
        mHelper.maybeRestoreAllDynamicShortcuts();

        if (ACTION_ADD_WEBSITE.equals(getIntent().getAction())) {
            //通过清单快捷方式调用。
            addWebSite();
        }

        mAdapter = new MyAdapter(getApplicationContext());
        setListAdapter(mAdapter);
    }

    private void addWebSite() {
        Log.i(TAG, "addWebSite: ");
        // 这个很重要。 这允许启动器构建预测模型。
        mHelper.reportShortcutUsed(ID_ADD_WEBSITE);

        final EditText etUri = new EditText(this);
        etUri.setHint("http://www.android.com");
        etUri.setInputType(EditorInfo.TYPE_TEXT_VARIATION_URI);

        new AlertDialog.Builder(this)
                .setTitle("添加新网站")
                .setMessage("输入网站的URL")
                .setView(etUri)
                .setPositiveButton("添加", (dialog, whichButton) -> {
                    final String url = etUri.getText().toString().trim();
                    if (url.length() > 0) {
                        addUriAsync(url);
                    }
                }).show();
    }

    /**
     * @param url
     */
    private void addUriAsync(String url) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                mHelper.addWebsiteShortcut(url);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                refreshList();
            }
        }.execute();
    }

    private void refreshList() {
        mAdapter.setShortcuts(mHelper.getShortcuts());
    }

    @Override
    public void onClick(View v) {
        final ShortcutInfo shortcut = (ShortcutInfo) ((View) v.getParent()).getTag();

        switch (v.getId()) {
            case R.id.disable:
                if (shortcut.isEnabled()) {
                    mHelper.disableShortcut(shortcut);
                } else {
                    mHelper.enableShortcut(shortcut);
                }
                refreshList();
                break;
            case R.id.remove:
                mHelper.removeShortcut(shortcut);
                refreshList();
                break;
            default:
                break;
        }
    }

    private String getType(ShortcutInfo shortcutInfo) {
        final StringBuilder sb = new StringBuilder();
        String sep = "";
        if (shortcutInfo.isDynamic()) {
            sb.append(sep).append("动态");
            sep = "，";
        }

        if (shortcutInfo.isPinned()) {
            sb.append(sep).append("固定");
            sep = "，";
        }

        if (!shortcutInfo.isEnabled()) {
            sb.append(sep).append("禁用");
            sep = "，";
        }
        return sb.toString();
    }

    /**
     * 处理添加按钮
     *
     * @param v
     */
    public void onAddPressed(View v) {
        addWebSite();
    }

    private class MyAdapter extends BaseAdapter {
        private final Context mContext;
        private final LayoutInflater mInflater;
        private List<ShortcutInfo> mList = EMPTY_LIST;

        public MyAdapter(Context context) {
            this.mContext = context;
            this.mInflater = getLayoutInflater();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        public void setShortcuts(List<ShortcutInfo> list) {
            this.mList = list;
            notifyDataSetChanged();
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView != null) {
                view = convertView;
            } else {
                view = mInflater.inflate(R.layout.list_item, null);
            }
            bindView(view, position, mList.get(position));
            return view;
        }

        private void bindView(View view, int position, ShortcutInfo shortcutInfo) {
            view.setTag(shortcutInfo);

            TextView line1 = view.findViewById(R.id.line1);
            TextView line2 = view.findViewById(R.id.line2);
            line1.setText(shortcutInfo.getLongLabel());
            line2.setText(getType(shortcutInfo));

            Button remove = view.findViewById(R.id.remove);
            Button disable = view.findViewById(R.id.disable);

            disable.setText(shortcutInfo.isEnabled() ? R.string.disable_shortcut : R.string.enable_shortcut);

            remove.setOnClickListener(MainActivity.this);
            disable.setOnClickListener(MainActivity.this);
        }
    }
}

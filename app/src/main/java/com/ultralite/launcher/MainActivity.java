package com.ultralite.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int BG = Color.rgb(18, 20, 26);
    private static final int PANEL = Color.rgb(31, 34, 43);
    private static final int TEXT = Color.rgb(245, 246, 250);
    private static final int MUTED = Color.rgb(170, 176, 190);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<AppEntry> allApps = new ArrayList<>();
    private final List<AppEntry> shownApps = new ArrayList<>();
    private LinearLayout root;
    private LinearLayout homePanel;
    private LinearLayout drawerPanel;
    private GridView appGrid;
    private AppAdapter adapter;
    private EditText searchBox;
    private TextView clockView;
    private TextView dateView;
    private boolean drawerOpen = false;
    private float touchDownY;
    private SharedPreferences prefs;

    private final Runnable clockUpdater = new Runnable() {
        @Override public void run() {
            if (clockView != null && dateView != null) {
                Date now = new Date();
                clockView.setText(DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault()).format(now));
                dateView.setText(DateFormat.getDateInstance(DateFormat.FULL, Locale.getDefault()).format(now));
            }
            handler.postDelayed(this, 30_000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("launcher_settings", MODE_PRIVATE);
        buildInterface();
        loadApps();
        showHome();
        handler.post(clockUpdater);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (root != null) loadApps();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(clockUpdater);
        super.onDestroy();
    }

    private void buildInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        setContentView(root);

        homePanel = new LinearLayout(this);
        homePanel.setOrientation(LinearLayout.VERTICAL);
        homePanel.setGravity(Gravity.CENTER_HORIZONTAL);
        homePanel.setLayoutParams(new LinearLayout.LayoutParams(-1, -1));

        LinearLayout clockBlock = new LinearLayout(this);
        clockBlock.setOrientation(LinearLayout.VERTICAL);
        clockBlock.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(-1, 0, 1.1f);
        homePanel.addView(clockBlock, clockParams);

        clockView = new TextView(this);
        clockView.setTextColor(TEXT);
        clockView.setTextSize(58);
        clockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clockView.setGravity(Gravity.CENTER);
        clockBlock.addView(clockView, new LinearLayout.LayoutParams(-1, -2));

        dateView = new TextView(this);
        dateView.setTextColor(MUTED);
        dateView.setTextSize(14);
        dateView.setGravity(Gravity.CENTER);
        dateView.setPadding(0, dp(6), 0, dp(8));
        clockBlock.addView(dateView, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = new TextView(this);
        hint.setText("Swipe up to open apps");
        hint.setTextColor(MUTED);
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        clockBlock.addView(hint);

        TextView drawerButton = makeButton("All apps  ↑");
        drawerButton.setOnClickListener(v -> showDrawer(""));
        homePanel.addView(drawerButton, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView favoritesTitle = new TextView(this);
        favoritesTitle.setText("FAVORITES");
        favoritesTitle.setTextColor(MUTED);
        favoritesTitle.setTextSize(11);
        favoritesTitle.setLetterSpacing(0.12f);
        favoritesTitle.setPadding(dp(4), dp(20), 0, dp(8));
        homePanel.addView(favoritesTitle);

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        homePanel.addView(dock, new LinearLayout.LayoutParams(-1, dp(92)));
        homePanel.setTag(dock);

        drawerPanel = new LinearLayout(this);
        drawerPanel.setOrientation(LinearLayout.VERTICAL);
        drawerPanel.setVisibility(View.GONE);

        TextView drawerHeading = new TextView(this);
        drawerHeading.setText("Applications");
        drawerHeading.setTextColor(TEXT);
        drawerHeading.setTextSize(24);
        drawerHeading.setTypeface(null, Typeface.BOLD);
        drawerHeading.setPadding(0, dp(4), 0, dp(14));
        drawerPanel.addView(drawerHeading);

        searchBox = new EditText(this);
        searchBox.setSingleLine(true);
        searchBox.setTextSize(16);
        searchBox.setTextColor(TEXT);
        searchBox.setHintTextColor(MUTED);
        searchBox.setHint("Search apps");
        searchBox.setPadding(dp(14), 0, dp(12), 0);
        searchBox.setBackground(roundRect(PANEL, dp(14)));
        drawerPanel.addView(searchBox, new LinearLayout.LayoutParams(-1, dp(50)));

        TextView backButton = makeButton("←  Home");
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(-1, dp(42));
        backParams.topMargin = dp(8);
        drawerPanel.addView(backButton, backParams);
        backButton.setOnClickListener(v -> showHome());

        appGrid = new GridView(this);
        appGrid.setNumColumns(4);
        appGrid.setVerticalSpacing(dp(8));
        appGrid.setHorizontalSpacing(dp(4));
        appGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setPadding(0, dp(8), 0, 0);
        drawerPanel.addView(appGrid, new LinearLayout.LayoutParams(-1, 0, 1f));

        adapter = new AppAdapter();
        appGrid.setAdapter(adapter);
        appGrid.setOnItemClickListener((parent, view, position, id) -> launchApp(shownApps.get(position)));

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterApps(s.toString()); }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        root.addView(homePanel, new LinearLayout.LayoutParams(-1, -1));
        root.addView(drawerPanel, new LinearLayout.LayoutParams(-1, -1));

        View.OnTouchListener swipeListener = (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                touchDownY = event.getY();
                return true;
            }
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float delta = event.getY() - touchDownY;
                if (!drawerOpen && delta < -dp(55)) showDrawer("");
                else if (drawerOpen && delta > dp(70)) showHome();
                return true;
            }
            return true;
        };
        homePanel.setOnTouchListener(swipeListener);
        clockBlock.setOnTouchListener(swipeListener);
        hint.setOnTouchListener(swipeListener);
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> results = pm.queryIntentActivities(query, 0);
        allApps.clear();
        for (ResolveInfo ri : results) {
            if (ri.activityInfo == null) continue;
            String label = ri.loadLabel(pm).toString();
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);
            launch.setClassName(ri.activityInfo.packageName, ri.activityInfo.name);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            allApps.add(new AppEntry(label, ri.activityInfo.packageName, ri.activityInfo.name, launch, ri.loadIcon(pm)));
        }
        Collections.sort(allApps, Comparator.comparing(a -> a.label.toLowerCase(Locale.getDefault())));
        filterApps(searchBox == null ? "" : searchBox.getText().toString());
        updateDock();
    }

    private void filterApps(String query) {
        shownApps.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        for (AppEntry app : allApps) {
            if (q.isEmpty() || app.label.toLowerCase(Locale.getDefault()).contains(q)) shownApps.add(app);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void updateDock() {
        if (homePanel == null) return;
        LinearLayout dock = (LinearLayout) homePanel.getTag();
        if (dock == null) return;
        dock.removeAllViews();
        int count = Math.min(5, allApps.size());
        for (int i = 0; i < count; i++) {
            AppEntry app = allApps.get(i);
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            ImageView icon = new ImageView(this);
            icon.setImageDrawable(app.icon);
            item.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));
            TextView label = new TextView(this);
            label.setText(app.label);
            label.setTextColor(TEXT);
            label.setTextSize(10);
            label.setGravity(Gravity.CENTER);
            label.setMaxLines(1);
            item.addView(label, new LinearLayout.LayoutParams(-1, -2));
            item.setOnClickListener(v -> launchApp(app));
            dock.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private void showDrawer(String query) {
        drawerOpen = true;
        homePanel.setVisibility(View.GONE);
        drawerPanel.setVisibility(View.VISIBLE);
        if (searchBox != null) {
            searchBox.setText(query);
            searchBox.clearFocus();
        }
        filterApps(query);
    }

    private void showHome() {
        drawerOpen = false;
        if (drawerPanel != null) drawerPanel.setVisibility(View.GONE);
        if (homePanel != null) homePanel.setVisibility(View.VISIBLE);
        if (searchBox != null) {
            searchBox.setText("");
            searchBox.clearFocus();
        }
        View focused = getCurrentFocus();
        if (focused != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }

    private void launchApp(AppEntry app) {
        if (app == null) return;
        try {
            startActivity(app.launchIntent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "Could not open " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerOpen) showHome();
        else super.onBackPressed();
    }

    private TextView makeButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(TEXT);
        button.setTextSize(15);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(PANEL, dp(14)));
        return button;
    }

    private GradientDrawable roundRect(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class AppEntry {
        final String label;
        final String packageName;
        final String activityName;
        final Intent launchIntent;
        final android.graphics.drawable.Drawable icon;
        AppEntry(String label, String packageName, String activityName, Intent launchIntent,
                 android.graphics.drawable.Drawable icon) {
            this.label = label;
            this.packageName = packageName;
            this.activityName = activityName;
            this.launchIntent = launchIntent;
            this.icon = icon;
        }
    }

    private class AppAdapter extends BaseAdapter {
        @Override public int getCount() { return shownApps.size(); }
        @Override public Object getItem(int position) { return shownApps.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView label;
            if (convertView instanceof LinearLayout) {
                cell = (LinearLayout) convertView;
                icon = (ImageView) cell.getChildAt(0);
                label = (TextView) cell.getChildAt(1);
            } else {
                cell = new LinearLayout(MainActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(8), dp(2), dp(8));
                icon = new ImageView(MainActivity.this);
                cell.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
                label = new TextView(MainActivity.this);
                label.setTextColor(TEXT);
                label.setTextSize(11);
                label.setGravity(Gravity.CENTER);
                label.setMaxLines(2);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(34));
                lp.topMargin = dp(4);
                cell.addView(label, lp);
            }
            AppEntry app = shownApps.get(position);
            icon.setImageDrawable(app.icon);
            label.setText(app.label);
            return cell;
        }
    }
}

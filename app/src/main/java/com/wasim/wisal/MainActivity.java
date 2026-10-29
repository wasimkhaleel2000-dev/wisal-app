package com.wasim.wisal;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.webkit.*;
import android.view.*;
import android.widget.*;
import android.content.SharedPreferences;

public class MainActivity extends Activity {
    WebView web;
    SharedPreferences prefs;
    String siteUrl;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("wisal", MODE_PRIVATE);
        siteUrl = prefs.getString("url", "");
        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setMediaPlaybackRequiresUserGesture(false);
        web.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        request.grant(request.getResources());
                    }
                });
            }
        });
        web.setWebViewClient(new WebViewClient());
        setContentView(web);

        if (siteUrl.isEmpty()) askUrl();
        else web.loadUrl(siteUrl);
    }

    private void askUrl() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://رابط-Cloudflare");
        input.setText(siteUrl);
        new AlertDialog.Builder(this)
            .setTitle("وصال")
            .setMessage("أدخل رابط وصال الذي يعمل عبر Cloudflare")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("فتح", (d, w) -> {
                String u = input.getText().toString().trim();
                if (!u.startsWith("https://")) {
                    new AlertDialog.Builder(this)
                        .setMessage("استخدم رابط HTTPS صحيحًا")
                        .setPositiveButton("حسنًا", (a, c) -> askUrl())
                        .show();
                    return;
                }
                siteUrl = u;
                prefs.edit().putString("url", siteUrl).apply();
                web.loadUrl(siteUrl);
            }).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("تغيير رابط وصال").setOnMenuItemClickListener(item -> {
            askUrl();
            return true;
        });
        return true;
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
}

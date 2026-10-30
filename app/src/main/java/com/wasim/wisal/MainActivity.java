
package com.wasim.wisal;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.webkit.*;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView web;
    private SharedPreferences prefs;
    private String siteUrl = "";
    private PermissionRequest pendingRequest;

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
                runOnUiThread(() -> handlePermission(request));
            }
        });

        web.setWebViewClient(new WebViewClient());
        setContentView(web);

        if (siteUrl.isEmpty()) {
            askUrl();
        } else {
            web.loadUrl(siteUrl);
        }
    }

    private void handlePermission(PermissionRequest request) {
        Uri origin = request.getOrigin();
        Uri saved = Uri.parse(siteUrl);

        if (!"https".equalsIgnoreCase(origin.getScheme())
                || !origin.getHost().equalsIgnoreCase(saved.getHost())) {
            request.deny();
            return;
        }

        if (pendingRequest != null) {
            request.deny();
            return;
        }

        ArrayList<String> needed = new ArrayList<>();

        for (String resource : request.getResources()) {
            if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                    && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.RECORD_AUDIO);
            }

            if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                    && checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.CAMERA);
            }
        }

        if (needed.isEmpty()) {
            request.grant(request.getResources());
        } else {
            pendingRequest = request;
            requestPermissions(needed.toArray(new String[0]), 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if (requestCode == 100 && pendingRequest != null) {
            PermissionRequest request = pendingRequest;
            pendingRequest = null;

            ArrayList<String> allowed = new ArrayList<>();

            for (String resource : request.getResources()) {
                if (resource.equals(PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                        && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED) {
                    allowed.add(resource);
                }

                if (resource.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                        && checkSelfPermission(Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    allowed.add(resource);
                }
            }

            if (!allowed.isEmpty()) {
                request.grant(allowed.toArray(new String[0]));
            } else {
                request.deny();
            }
        }
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
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }
}

package com.esc1.app;

import android.app.Activity;
import android.app.AlertDialog;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> chooser;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        web = new WebView(this);
        setContentView(web);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        web.addJavascriptInterface(new Bridge(), "AndroidSave");
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb, FileChooserParams p) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = cb;
                try {
                    startActivityForResult(p.createIntent(), 1);
                } catch (Exception e) {
                    chooser = null;
                    return false;
                }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
        checkUpdate();
    }

    private static final String BASE = "https://github.com/lemrabottmoustapha-maker/ESC1/releases/download/latest/";

    private void checkUpdate() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(BASE + "version.txt").openConnection();
                    c.setConnectTimeout(6000);
                    c.setReadTimeout(6000);
                    c.setInstanceFollowRedirects(true);
                    BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
                    final int latest = Integer.parseInt(r.readLine().trim());
                    r.close();
                    int cur = (int) getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
                    if (latest > cur) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                new AlertDialog.Builder(MainActivity.this)
                                    .setTitle("Mise à jour disponible")
                                    .setMessage("Une version plus récente est disponible. Vos données ne seront pas supprimées.")
                                    .setPositiveButton("Mettre à jour", new android.content.DialogInterface.OnClickListener() {
                                        public void onClick(android.content.DialogInterface d, int w) {
                                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(BASE + "ESC1.apk")));
                                        }
                                    })
                                    .setNegativeButton("Plus tard", null)
                                    .show();
                            }
                        });
                    }
                } catch (Exception e) {
                    // بدون إنترنت أو تعذّر الفحص: تجاهل بصمت
                }
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == 1 && chooser != null) {
            chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res, data));
            chooser = null;
        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    class Bridge {
        @JavascriptInterface
        public void save(String name, String mime, String b64) {
            try {
                byte[] d = Base64.decode(b64, Base64.DEFAULT);
                ContentValues v = new ContentValues();
                v.put(MediaStore.Downloads.DISPLAY_NAME, name);
                v.put(MediaStore.Downloads.MIME_TYPE, mime);
                v.put(MediaStore.Downloads.RELATIVE_PATH, "Download");
                Uri u = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                OutputStream o = getContentResolver().openOutputStream(u);
                o.write(d);
                o.close();
                toast("Fichier enregistré dans Téléchargements : " + name);
            } catch (Exception e) {
                toast("Échec de l’enregistrement du fichier");
            }
        }
    }

    private void toast(final String t) {
        runOnUiThread(new Runnable() {
            public void run() { Toast.makeText(MainActivity.this, t, Toast.LENGTH_LONG).show(); }
        });
    }
}

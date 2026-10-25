package fr.confidant.app;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public final class MainActivity extends Activity {
    private static final String APP_HOST = "mediagrabofficial.github.io";
    private static final String APP_PATH = "/confidant";
    private static final String NOTIFICATION_CHANNEL = "confidant-messages";
    private static final int REQ_MEDIA = 2101;
    private static final int REQ_FILES = 2102;
    private static final int REQ_NOTIFICATIONS = 2103;

    private WebView webView;
    private PermissionRequest pendingWebPermission;
    private String[] pendingWebResources;
    private ValueCallback<Uri[]> pendingFileChooser;
    private int nextNotificationId = 1;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(234, 231, 223));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(234, 231, 223));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                    View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (isInternalAppUrl(uri)) return false;
                return openExternal(uri);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> handleWebPermission(request));
            }

            @Override
            public void onPermissionRequestCanceled(PermissionRequest request) {
                if (pendingWebPermission == request) {
                    pendingWebPermission = null;
                    pendingWebResources = null;
                }
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (pendingFileChooser != null) pendingFileChooser.onReceiveValue(null);
                pendingFileChooser = callback;
                Intent pick;
                try {
                    pick = params.createIntent();
                } catch (RuntimeException e) {
                    pick = new Intent(Intent.ACTION_GET_CONTENT);
                    pick.addCategory(Intent.CATEGORY_OPENABLE);
                    pick.setType("*/*");
                }
                try {
                    startActivityForResult(pick, REQ_FILES);
                    return true;
                } catch (ActivityNotFoundException e) {
                    pendingFileChooser = null;
                    callback.onReceiveValue(null);
                    Toast.makeText(MainActivity.this, "Aucun sélecteur de fichiers disponible.", Toast.LENGTH_SHORT).show();
                    return true;
                }
            }
        });

        webView.addJavascriptInterface(new ConfidantBridge(), "ConfidantAndroid");
        setContentView(webView);
        if (state != null) {
            webView.restoreState(state);
        } else {
            String startUrl = internalAppUrl(getIntent() == null ? null : getIntent().getData());
            webView.loadUrl(startUrl == null ? "https://" + APP_HOST + APP_PATH + "/" : startUrl);
        }
    }

    private boolean isInternalAppUrl(Uri uri) {
        return internalAppUrl(uri) != null;
    }

    private String internalAppUrl(Uri uri) {
        if (uri == null) return null;
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        String host = uri.getHost() == null ? "" : uri.getHost();
        String path = uri.getPath() == null ? "" : uri.getPath();
        boolean allowed = "https".equalsIgnoreCase(scheme)
                && APP_HOST.equalsIgnoreCase(host)
                && (path.equals(APP_PATH) || path.startsWith(APP_PATH + "/"));
        return allowed ? uri.toString() : null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String inviteUrl = internalAppUrl(intent == null ? null : intent.getData());
        if (webView != null && inviteUrl != null) webView.loadUrl(inviteUrl);
    }

    private void handleWebPermission(PermissionRequest request) {
        Uri origin = request.getOrigin();
        if (origin == null || !"https".equalsIgnoreCase(origin.getScheme())
                || !APP_HOST.equalsIgnoreCase(origin.getHost())) {
            request.deny();
            return;
        }

        Set<String> androidPermissions = new LinkedHashSet<>();
        String[] resources = request.getResources();
        for (String resource : resources) {
            if (PermissionRequest.RESOURCE_AUDIO_CAPTURE.equals(resource)) {
                androidPermissions.add(Manifest.permission.RECORD_AUDIO);
            } else if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                androidPermissions.add(Manifest.permission.CAMERA);
            } else {
                request.deny();
                return;
            }
        }
        if (resources.length == 0 || androidPermissions.isEmpty()) {
            request.deny();
            return;
        }

        ArrayList<String> missing = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 23) {
            for (String permission : androidPermissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
            }
        }
        if (missing.isEmpty()) {
            request.grant(resources);
            return;
        }

        if (pendingWebPermission != null) pendingWebPermission.deny();
        pendingWebPermission = request;
        pendingWebResources = resources.clone();
        String[] permissionsToRequest = missing.toArray(new String[0]);
        requestPermissions(permissionsToRequest, REQ_MEDIA);
    }

    public final class ConfidantBridge {
        @JavascriptInterface
        public void requestNotifications() {
            runOnUiThread(() -> {
                if (Build.VERSION.SDK_INT >= 33
                        && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
                } else {
                    Toast.makeText(MainActivity.this, "Notifications activées.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void notifyMessage(String title, String body) {
            final String safeTitle = safeText(title, "Confidant", 64);
            final String safeBody = safeText(body, "Nouveau message", 180);
            runOnUiThread(() -> postMessageNotification(safeTitle, safeBody));
        }
    }

    private String safeText(String value, String fallback, int maxLength) {
        if (value == null || value.trim().isEmpty()) return fallback;
        String compact = value.replace('\n', ' ').replace('\r', ' ').trim();
        return compact.length() > maxLength ? compact.substring(0, maxLength) : compact;
    }

    private void postMessageNotification(String title, String body) {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL, "Messages Confidant", NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Notifications de nouveaux messages Confidant");
            manager.createNotificationChannel(channel);
        }
        Intent launch = new Intent(this, MainActivity.class);
        launch.setAction(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        launch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(this, 0, launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, NOTIFICATION_CHANNEL)
                : new Notification.Builder(this);
        Notification notification = builder
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build();
        manager.notify(nextNotificationId++, notification);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_MEDIA && pendingWebPermission != null) {
            PermissionRequest request = pendingWebPermission;
            String[] resources = pendingWebResources;
            pendingWebPermission = null;
            pendingWebResources = null;
            boolean granted = results.length > 0;
            for (int result : results) granted &= result == PackageManager.PERMISSION_GRANTED;
            if (granted && resources != null) {
                request.grant(resources);
            } else {
                request.deny();
                Toast.makeText(this, "L’accès caméra ou microphone n’a pas été autorisé.", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQ_NOTIFICATIONS) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications activées.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Les notifications restent désactivées.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILES && pendingFileChooser != null) {
            Uri[] selected = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) selected = new Uri[]{data.getData()};
                else if (Build.VERSION.SDK_INT >= 16 && data.getClipData() != null &&
                        data.getClipData().getItemCount() > 0) {
                    selected = new Uri[]{data.getClipData().getItemAt(0).getUri()};
                }
            }
            pendingFileChooser.onReceiveValue(selected);
            pendingFileChooser = null;
        }
    }

    private boolean openExternal(Uri uri) {
        if (uri == null) return true;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "Impossible d’ouvrir ce lien sur cet appareil.", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if (pendingWebPermission != null) pendingWebPermission.deny();
        if (pendingFileChooser != null) pendingFileChooser.onReceiveValue(null);
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.removeJavascriptInterface("ConfidantAndroid");
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
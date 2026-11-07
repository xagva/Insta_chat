package com.instachat.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.regex.Pattern;

/**
 * A WebView locked to Instagram Direct Messages. Login and 2FA pages are allowed.
 * Every other Instagram page (feed, reels, explore, profiles, stories) redirects to the inbox.
 */
public class MainActivity extends Activity {

    private static final String INBOX = "https://www.instagram.com/direct/inbox/";

    /** Paths on www.instagram.com that may be shown. */
    private static final Pattern ALLOWED_PATH = Pattern.compile(
            "^/(direct/|accounts/|challenge/|two_factor|auth_platform/|oauth/).*");

    /** Hides navigation links to non-chat sections. Redirects catch anything that slips through. */
    private static final String HIDE_NAV_JS =
            "(function(){if(document.getElementById('ic-hide'))return;" +
            "var s=document.createElement('style');s.id='ic-hide';" +
            "s.textContent='a[href=\"/\"],a[href^=\"/explore\"],a[href^=\"/reels\"],a[href^=\"/stories\"]," +
            "a[href^=\"/threads\"],a[href*=\"threads.net\"],a[href*=\"threads.com\"],a[href=\"/accounts/activity/\"]," +
            "a[href^=\"/meta-ai\"],a[href*=\"play.google.com\"],a[href*=\"apps.apple.com\"]{display:none!important}';" +
            "(document.head||document.documentElement).appendChild(s);})();";

    private static final int FILE_REQUEST = 1;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        // Instagram serves a degraded page to WebViews; present as regular mobile Chrome instead.
        s.setUserAgentString(s.getUserAgentString().replace("; wv", ""));

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        cookies.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new LockedClient());
        web.setWebChromeClient(new WebChromeClient() {
            // Lets you attach photos/videos in chats.
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_REQUEST);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState);
        } else {
            web.loadUrl(INBOX);
        }
    }

    /** True if this URL is on Instagram's main site. */
    private static boolean isMainSite(Uri uri) {
        String host = uri.getHost();
        return "www.instagram.com".equals(host) || "instagram.com".equals(host);
    }

    private static boolean isAllowed(Uri uri) {
        String path = uri.getPath() == null ? "/" : uri.getPath();
        return ALLOWED_PATH.matcher(path).matches();
    }

    private class LockedClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            String host = uri.getHost() == null ? "" : uri.getHost();

            if (isMainSite(uri)) {
                if (isAllowed(uri)) return false;
                view.loadUrl(INBOX);
                return true;
            }
            // Other Instagram/Facebook hosts appear during login, 2FA and account checks.
            if (host.endsWith(".instagram.com") || host.endsWith(".facebook.com")
                    || host.equals("facebook.com")) {
                return false;
            }
            // Any other link (shared websites) opens in the regular browser, outside this app.
            if ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) { }
            }
            return true;
        }

        // Instagram changes pages without reloading. This catches those in-app navigations.
        @Override
        public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
            Uri uri = Uri.parse(url);
            if (isMainSite(uri) && !isAllowed(uri)) {
                view.loadUrl(INBOX);
            }
            view.evaluateJavascript(HIDE_NAV_JS, null);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            Uri uri = Uri.parse(url);
            if (isMainSite(uri) && !isAllowed(uri)) {
                view.stopLoading();
                view.loadUrl(INBOX);
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            view.evaluateJavascript(HIDE_NAV_JS, null);
            CookieManager.getInstance().flush(); // keep you logged in
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_REQUEST && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }
}

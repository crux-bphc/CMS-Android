package crux.bphc.cms;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.CookieSyncManager;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import app.Constants;
import helper.UserAccount;

public class WebSiteActivity extends AppCompatActivity {

    private static final String PARAM1 = "title";
    private static final String PARAM2 = "loginURL";
    private static final String TAG = "WebsiteActivity.class";
    private static final int MAX_LOGIN_ATTEMPTS = 2;
    WebView webview;
    String title, loginURL;
    int loginAttempts = 0;
    UserAccount userAccount;
    SwipeRefreshLayout swipeRefreshLayout;
    private String toLoadURL;

    public static Intent getIntent(Context context, String title, String url) {
        Intent intent = new Intent(context, WebSiteActivity.class);
        intent.putExtra(PARAM1, title);
        intent.putExtra(PARAM2, url);
        return intent;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_site);

        setData();


        userAccount = new UserAccount(this);

        webview = (WebView) findViewById(R.id.webview);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webview.loadUrl(webview.getUrl());
            }
        });


        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setDomStorageEnabled(true);
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                setTitle(title);
            }
        });


        webview.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                swipeRefreshLayout.setRefreshing(true);
                return true;
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(request.getUrl().toString());
                swipeRefreshLayout.setRefreshing(true);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d(TAG, "Page load finished");
                swipeRefreshLayout.setRefreshing(false);
                if (url.equalsIgnoreCase(loginURL))
                    if (loginAttempts < MAX_LOGIN_ATTEMPTS) {
                        loginAttempts++;
                        tryAutoLogin();
                    }

            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                Toast.makeText(WebSiteActivity.this, "Your Internet Connection May not be active Or " + error, Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
            }

        });

        webview.loadUrl(toLoadURL);
        swipeRefreshLayout.setRefreshing(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.action_open_in_browser) {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webview.getUrl()));
            startActivity(browserIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    void tryAutoLogin() {
        Toast.makeText(this, "Attempting Auto Login",
                Toast.LENGTH_SHORT).show();
        String uname = userAccount.getUsername();
        String password = userAccount.getPassword();
        webview.loadUrl("javascript: {" +
                "document.getElementById('username').value = '" + uname + "';" +
                "document.getElementById('password').value = '" + password + "';" +
                "document.forms[0].submit(); };");
        swipeRefreshLayout.setRefreshing(true);
        Log.d(TAG, "Login attempted");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.website_menu, menu);
        return true;
    }

    private void setData() {
        title = getIntent().getStringExtra(PARAM1);
        loginURL = Constants.LOGIN_URL;
        toLoadURL = getIntent().getStringExtra(PARAM2);
    }
}

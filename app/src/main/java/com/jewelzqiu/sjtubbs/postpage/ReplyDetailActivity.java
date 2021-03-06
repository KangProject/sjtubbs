package com.jewelzqiu.sjtubbs.postpage;

import com.jewelzqiu.sjtubbs.R;
import com.jewelzqiu.sjtubbs.main.BBSApplication;
import com.jewelzqiu.sjtubbs.newpost.NewPostActivity;
import com.jewelzqiu.sjtubbs.support.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.HashSet;

public class ReplyDetailActivity extends ActionBarActivity {

    public static final String REPLY_USER = "reply_user";

    public static final String REPLY_TIME = "reply_time";

    public static final String REPLY_CONTENT = "reply_content";

    public static final String REPLY_TITLE = "reply_title";

    public static final String REPLY_BOARD = "reply_board";

    public static final String REPLY_URL = "reply_url";

    private static final String IMG_AUTO_ZOOM = "javascript:"
            + "var elements = document.getElementsByTag('img').onload = "
            + "function(){"
            + "if(this.width > screen.width) {"
            + "this.width = screen.width"
            + "}"
            + "}";

    private WebView mWebView;

    private HashSet<String> imgFormatSet = new HashSet<String>();

    private String mUrl, mReplyTo, mBoardName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_page);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.setWebViewClient(new MyWebViewClient());
        mWebView.setClipToPadding(false);

        BBSApplication.imgUrlMap.clear();
        BBSApplication.imgUrlList.clear();
        new PrepareContentTask(mWebView).execute(getIntent().getStringExtra(REPLY_CONTENT));

        setTitle(getIntent().getStringExtra(REPLY_TITLE));

//        SystemBarTintManager tintManager = new SystemBarTintManager(this);
//        tintManager.setStatusBarTintEnabled(true);
//        tintManager.setNavigationBarTintEnabled(true);
//        tintManager.setTintColor(getResources().getColor(android.R.color.holo_blue_dark));
//        tintManager.setTintAlpha(0.69f);
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            View layout = findViewById(R.id.parent_layout);
//            SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
//            layout.setPadding(0, config.getPixelInsetTop(true), 0, config.getPixelInsetBottom());
//        }

        imgFormatSet.clear();
        imgFormatSet.add("jpg");
        imgFormatSet.add("jpeg");
        imgFormatSet.add("gif");
        imgFormatSet.add("png");
        imgFormatSet.add("bmp");

        mUrl = getIntent().getStringExtra(REPLY_URL);
        mReplyTo = getIntent().getStringExtra(REPLY_USER);
        mBoardName = getIntent().getStringExtra(REPLY_BOARD);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (BBSApplication.imgUrlMap.isEmpty()) {
            getMenuInflater().inflate(R.menu.reply_detail_no_pic, menu);
        } else {
            getMenuInflater().inflate(R.menu.reply_detail, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_pic:
                if (BBSApplication.imgUrlMap.isEmpty()) {
                    Toast.makeText(this, "此贴没有图片！", Toast.LENGTH_SHORT).show();
                    return true;
                }
                startActivity(new Intent(this, PicViewPagerActivity.class));
                return true;
            case R.id.action_reply:
                Intent intent = new Intent(ReplyDetailActivity.this, NewPostActivity.class);
                intent.putExtra(NewPostActivity.FLAG_IS_REPLY, true);
                intent.putExtra(NewPostActivity.REPLY_URL, mUrl);
                intent.putExtra(NewPostActivity.REPLY_TO, mReplyTo);
                intent.putExtra(NewPostActivity.BOARD_NAME, mBoardName);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private class MyWebViewClient extends WebViewClient {

//        @Override
//        public void onPageFinished(WebView view, String url) {
//            view.loadUrl(IMG_AUTO_ZOOM);
//        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (BBSApplication.imgUrlMap.containsKey(url)) {
                Intent intent = new Intent(ReplyDetailActivity.this,
                        PicViewPagerActivity.class);
                intent.putExtra(PicViewPagerActivity.PHOTO_POSITION,
                        BBSApplication.imgUrlMap.get(url));
                startActivity(intent);
            } else if (imgFormatSet
                    .contains(url.substring(url.lastIndexOf('.') + 1).toLowerCase())) {
                Intent intent = new Intent(ReplyDetailActivity.this, SinglePicActivity.class);
                intent.putExtra(SinglePicActivity.PIC_URL, url);
                startActivity(intent);
            } else {
                // TODO parse post data
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            }
            return true;
        }
    }

    private class PrepareContentTask extends AsyncTask<String, Void, Boolean> {

        private String html;

        private WebView mWebView;

        private String content;

        public PrepareContentTask(WebView webView) {
            mWebView = webView;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            html = params[0];
            boolean success = true;

            Document doc = Jsoup.parse(html);
            Elements imgs = doc.select("img");
            for (Element img : imgs) {
                img.attr("style", "max-width:100%; height:auto");
                img.wrap("<a href='" + img.attr("src") + "'></a>");
                int pos = BBSApplication.imgUrlMap.size();
                String url = img.attr("src");
                BBSApplication.imgUrlMap.put(url, pos);
                BBSApplication.imgUrlList.add(url);
            }
            content = doc.outerHtml();

            return success;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mWebView.loadDataWithBaseURL(Utils.BBS_BASE_URL, content, "text/html", "UTF-8",
                        null);
            } else {
                mWebView.loadUrl(null);
            }
            invalidateOptionsMenu();
        }
    }
}

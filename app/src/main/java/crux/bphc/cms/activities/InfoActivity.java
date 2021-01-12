package crux.bphc.cms.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import crux.bphc.cms.R;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.app.Urls;
import crux.bphc.cms.models.UserAccount;
import crux.bphc.cms.widgets.HtmlTextView;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (UserAccount.INSTANCE.isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        ImageView imageView = findViewById(R.id.image);
        imageView.setOnClickListener(view -> {
            Intent viewIntent = new Intent("android.intent.action.VIEW", Urls.WEBSITE_URL);
            startActivity(viewIntent);
        });

        findViewById(R.id.crux).setOnClickListener(view -> {
            Intent viewIntent = new Intent("android.intent.action.VIEW", Urls.WEBSITE_URL);
            startActivity(viewIntent);
        });
        setTitle("About us");

        /* Set the description text */
        HtmlTextView desc = findViewById(R.id.description);
        desc.setText(HtmlTextView.parseHtml(this.getApplicationContext().getResources().getString(R.string.app_info)));

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

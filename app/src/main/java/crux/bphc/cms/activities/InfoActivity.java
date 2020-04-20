package crux.bphc.cms.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import app.Constants;
import app.MyApplication;
import crux.bphc.cms.R;
import helper.HtmlTextView;

public class InfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (MyApplication.getInstance().isDarkModeEnabled()) {
            setTheme(R.style.AppTheme_Dark);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        ImageView imageView = findViewById(R.id.image);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse(Constants.WEBSITE_URL));
                startActivity(viewIntent);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_clear_white);
        findViewById(R.id.crux).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent viewIntent =
                        new Intent("android.intent.action.VIEW",
                                Uri.parse(Constants.WEBSITE_URL));
                startActivity(viewIntent);
            }
        });
        setTitle("About us");

        /* Set the description text */
        HtmlTextView desc = findViewById(R.id.description);
        desc.setText(HtmlTextView.parseHtml(this.getApplicationContext().getResources().getString(R.string.app_info)));
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

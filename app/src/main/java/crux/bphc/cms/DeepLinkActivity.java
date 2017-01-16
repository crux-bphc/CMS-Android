package crux.bphc.cms;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.util.List;

/**
 * Created by harsu on 15-01-2017.
 */
public class DeepLinkActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        Uri data = intent.getData();
        Intent intent1=new Intent(this,LoginActivity.class);
        intent1.putExtra("path",data);
        startActivity(intent1);
        finish();
    }
}

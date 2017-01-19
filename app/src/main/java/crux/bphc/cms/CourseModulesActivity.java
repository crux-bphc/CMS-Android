package crux.bphc.cms;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import app.MyApplication;
import helper.ClickListener;
import helper.ModulesAdapter;
import helper.MyFileManager;
import io.realm.Realm;
import io.realm.RealmResults;
import set.Content;
import set.CourseSection;
import set.Module;

import static helper.MyFileManager.DATA_DOWNLOADED;

public class CourseModulesActivity extends AppCompatActivity {


    List<Module> modules;


    ModulesAdapter myAdapter;
    Realm realm;
    MyFileManager mFileManager;
    boolean newFileDownloaded = false;


    private void setDownloaded(String fileName) {
        myAdapter.notifyDataSetChanged();
        newFileDownloaded = true;

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int sectionID = getIntent().getIntExtra("id", -1);
        if (sectionID == -1) {
            finish();
            return;
        }

        mFileManager = new MyFileManager(this);
        realm = MyApplication.getInstance().getRealmInstance();
        setContentView(R.layout.activity_course_modules);


        RealmResults<CourseSection> sections = realm.where(CourseSection.class).equalTo("id", sectionID).findAll();

        setTitle(sections.first().getName());

        modules = sections.first().getModules();
        if (modules.size() == 0) {
            findViewById(R.id.empty).setVisibility(View.VISIBLE);
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        myAdapter = new ModulesAdapter(this, mFileManager);
        myAdapter.setModules(modules);
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        mFileManager.registerDownloadReceiver();
        myAdapter.setClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                if (object instanceof Module) {
                    Module module = (Module) object;
                    if (module.getModType() == Module.Type.URL) {
                        if (module.getContents().size() > 0 && !module.getContents().get(0).getFileurl().isEmpty()) {
                            MyFileManager.showInWebsite(CourseModulesActivity.this, module.getContents().get(0).getFileurl());
                        }
                    }
                    //todo update
                    else if (module.getContents() == null || module.getContents().size() == 0) {
                        if (module.getModType() == Module.Type.FORUM || module.getModType() == Module.Type.LABEL) {
                            if (module.getDescription() == null || module.getDescription().length() == 0) {
                                return false;
                            }
                            AlertDialog.Builder alertDialog = new AlertDialog.Builder(CourseModulesActivity.this);
                            alertDialog.setMessage(Html.fromHtml(module.getDescription()));
                            alertDialog.setNegativeButton("Close", null);
                            alertDialog.show();
                        } else

                            MyFileManager.showInWebsite(CourseModulesActivity.this, module.getUrl());

                    } else {
                        for (Content content : module.getContents()) {
                            if (!mFileManager.searchFile(content.getFilename(), false)) {
                                mFileManager.downloadFile(content, module);
                            } else {
                                mFileManager.openFile(content.getFilename());
                            }
                        }
                    }
                    return true;
                }
                return false;
            }
        });

        mFileManager.setCallback(new MyFileManager.Callback() {
            @Override
            public void onDownloadCompleted(String fileName) {
                setDownloaded(fileName);
                mFileManager.openFile(fileName);
            }
        });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mFileManager.unregisterDownloadReceiver();

    }


    @Override
    public void onBackPressed() {
        if (newFileDownloaded)
            setResult(DATA_DOWNLOADED);
        else
            setResult(1);
        finish();
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }


}
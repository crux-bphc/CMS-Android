package crux.bphc.cms;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import helper.DividerItemDecoration;
import io.realm.Realm;
import io.realm.RealmResults;
import set.CourseSection;
import set.Module;

public class CourseModulesActivity extends AppCompatActivity {
    Realm realm;
    List<Module> modules;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int sectionID = getIntent().getIntExtra("id", -1);
        if (sectionID == -1) {
            finish();
            return;
        }
        setContentView(R.layout.activity_course_modules);
        realm = Realm.getDefaultInstance();
        RealmResults<CourseSection> sections = realm.where(CourseSection.class).equalTo("id", sectionID).findAll();


        modules = sections.first().getModules();

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        MyAdapter myAdapter = new MyAdapter(this);
        myAdapter.setModules(modules);
        recyclerView.setAdapter(myAdapter);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left,android.R.anim.slide_out_right);
    }

    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        Context context;
        LayoutInflater inflater;

        List<Module> modules;


        MyAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            modules = new ArrayList<>();

        }

        @Override
        public int getItemViewType(int position) {
            return modules.get(position).getModType();
        }

        void setModules(List<Module> modules) {
            this.modules = modules;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            return new ViewHolderResource(inflater.inflate(R.layout.row_course_module_resource, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            ((ViewHolderResource) holder).bind(modules.get(position));
        }

        @Override
        public int getItemCount() {
            return modules.size();
        }



        class ViewHolderResource extends RecyclerView.ViewHolder {
            TextView name;
            ImageView modIcon, download;


            ViewHolderResource(View itemView) {
                super(itemView);

                name = (TextView) itemView.findViewById(R.id.fileName);
                modIcon = (ImageView) itemView.findViewById(R.id.fileIcon);
                download = (ImageView) itemView.findViewById(R.id.download);
            }

            void bind(Module module) {
                name.setText(module.getName());
                modIcon.setVisibility(View.VISIBLE);
                if (module.getContents() == null || module.getContents().size()==0) {
                    download.setVisibility(View.GONE);
                } else
                    download.setVisibility(View.VISIBLE);

                switch (module.getModType()) {
                    case 0:
                    case 100:
                        Picasso.with(context).load(module.getModicon()).into(modIcon);
                        break;
                    case 3:
                        modIcon.setImageResource(R.drawable.book);
                        break;
                    case 4:
                        modIcon.setImageResource(R.drawable.folder);
                        break;
                    case 2:
                        modIcon.setVisibility(View.GONE);
                        break;
                    case 1:
                        modIcon.setImageResource(R.drawable.forum);
                        break;

                }
                //todo download or open icon
            }
        }

    }
}

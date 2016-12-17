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

    private class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        Context context;
        LayoutInflater inflater;

        List<Module> modules;


        public MyAdapter(Context context) {
            this.context = context;
            inflater = LayoutInflater.from(context);
            modules = new ArrayList<>();

        }

        @Override
        public int getItemViewType(int position) {
            return modules.get(position).getModType();
        }

        public void setModules(List<Module> modules) {
            this.modules = modules;
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            switch (viewType) {
                case 0:
                default:
                case 3:
                    return new ViewHolderResource(inflater.inflate(R.layout.row_course_module_resource, parent, false));

                case 2:
                    return new ViewHolderLabel(inflater.inflate(R.layout.row_course_module_label, parent, false));
                case 1:
                    return new ViewHolderForum(inflater.inflate(R.layout.row_course_module_label, parent, false));

            }

        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ViewHolderResource)
                ((ViewHolderResource) holder).bind(modules.get(position));
            else if (holder instanceof ViewHolderLabel)
                ((ViewHolderLabel) holder).bind(modules.get(position));
            else if (holder instanceof ViewHolderForum)
                ((ViewHolderForum) holder).bind(modules.get(position));
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
                switch (module.getModType()) {
                    case 0:
                    case 100:
                        Picasso.with(context).load(module.getModicon()).into(modIcon);
                        break;
                    case 3:
                        modIcon.setImageResource(R.drawable.book);
                }
                //todo download or open icon
            }
        }

        class ViewHolderLabel extends RecyclerView.ViewHolder {
            TextView name, description;


            ViewHolderLabel(View itemView) {
                super(itemView);

                name = (TextView) itemView.findViewById(R.id.fileName);
                description = (TextView) itemView.findViewById(R.id.description);
                itemView.findViewById(R.id.fileIcon).setVisibility(View.GONE);

            }

            void bind(Module module) {
                name.setText(module.getName());
                description.setText(Html.fromHtml(module.getDescription()));
            }
        }

        class ViewHolderForum extends RecyclerView.ViewHolder {
            TextView name, description;
            ImageView modIcon;

            ViewHolderForum(View itemView) {
                super(itemView);
                modIcon = (ImageView) itemView.findViewById(R.id.fileIcon);
                name = (TextView) itemView.findViewById(R.id.fileName);
                description = (TextView) itemView.findViewById(R.id.description);
                modIcon.setImageResource(R.drawable.forum);
            }

            void bind(Module module) {

                name.setText(module.getName());
                description.setText(Html.fromHtml(module.getDescription()));
            }
        }
    }
}

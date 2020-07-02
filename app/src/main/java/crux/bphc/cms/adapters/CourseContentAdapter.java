package crux.bphc.cms.adapters;

import android.app.Activity;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.hannesdorfmann.adapterdelegates4.AdapterDelegatesManager;

import java.util.List;

import crux.bphc.cms.adapters.delegates.CourseModuleDelegate;
import crux.bphc.cms.adapters.delegates.CourseSectionDelegate;
import crux.bphc.cms.helper.ClickListener;
import crux.bphc.cms.helper.FileManager;
import crux.bphc.cms.interfaces.CourseContent;


/**
 Adapter for Course Content. Utilizes the AdapterDelegates dependency to handle
 multiple view types efficiently.

 @see CourseModuleDelegate
 @see CourseSectionDelegate

 @author Abhijeet Viswa
 */

@SuppressWarnings("rawtypes")
public class CourseContentAdapter extends RecyclerView.Adapter {

    private final AdapterDelegatesManager<List<CourseContent>> delegatesManager;
    private List<CourseContent> contents;

    public CourseContentAdapter(Activity activity, List<CourseContent> contents, FileManager fileManager,
                                      ClickListener clickWrapperListener, ClickListener moreOptionsClickListener) {
        this.contents = contents;
        delegatesManager = new AdapterDelegatesManager<>();

        delegatesManager.addDelegate(new CourseSectionDelegate(activity))
                        .addDelegate(new CourseModuleDelegate(activity, fileManager, clickWrapperListener,
                            moreOptionsClickListener));
    }

    public void setCourseContents(List<CourseContent> contents) {
        this.contents = contents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return delegatesManager.onCreateViewHolder(parent, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        return delegatesManager.getItemViewType(contents, position);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        delegatesManager.onBindViewHolder(contents, position, holder);
    }

    @Override
    public int getItemCount() {
        return contents.size();
    }
}

package crux.bphc.cms.adapters.delegates;

import android.app.Activity;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hannesdorfmann.adapterdelegates4.AdapterDelegate;

import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.interfaces.CourseContent;
import crux.bphc.cms.models.CourseSection;
import crux.bphc.cms.widgets.CollapsibleTextView;

/**
 * Adapter delegate for course sections.
 *
 * @see crux.bphc.cms.adapters.CourseContentAdapter
 *
 * @author Abhijeet Viswa
 */
public class CourseSectionDelegate extends AdapterDelegate<List<CourseContent>> {

    private final LayoutInflater inflater;

    public CourseSectionDelegate(Activity activity) {
        inflater = activity.getLayoutInflater();
    }

    @Override
    protected boolean isForViewType(@NonNull List<CourseContent> items, int position) {
        return items.get(position) instanceof CourseSection;
    }

    @NonNull
    @Override
    protected RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent) {
        return new CourseSectionViewHolder(inflater.inflate(R.layout.row_course_section, parent, false));
    }

    @Override
    protected void onBindViewHolder(@NonNull List<CourseContent> items, int position, @NonNull RecyclerView.ViewHolder
            holder, @NonNull List<Object> payloads) {

        CourseSectionViewHolder vh = (CourseSectionViewHolder) holder;
        CourseSection section = (CourseSection) items.get(position);

        vh.sectionName.setText(section.getName());
        if (!section.getSummary().isEmpty()) {
            vh.sectionDescription.setVisibility(View.VISIBLE);
            vh.sectionDescription.setFullText(HtmlCompat.fromHtml(section.getSummary(),
                    HtmlCompat.FROM_HTML_MODE_COMPACT));
            vh.sectionDescription.setMovementMethod(LinkMovementMethod.getInstance());
        } else {
            vh.sectionDescription.setVisibility(View.GONE);
        }
    }


    static class CourseSectionViewHolder extends RecyclerView.ViewHolder {
        public final TextView sectionName;
        public final CollapsibleTextView sectionDescription;

        public CourseSectionViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionName = itemView.findViewById(R.id.section_name);
            sectionDescription = itemView.findViewById(R.id.description);
        }
    }
}

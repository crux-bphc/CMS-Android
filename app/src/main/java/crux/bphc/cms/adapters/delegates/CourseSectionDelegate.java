package crux.bphc.cms.adapters.delegates;

import android.app.Activity;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.hannesdorfmann.adapterdelegates4.AdapterDelegate;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.helper.HtmlTextView;
import crux.bphc.cms.helper.Util;
import crux.bphc.cms.interfaces.CourseContent;
import crux.bphc.cms.models.CourseSection;

/**
 * Adapter delegate for course sections.
 * @author Abhijeet Viswa
 */
public class CourseSectionDelegate extends AdapterDelegate<List<CourseContent>> {

    private LayoutInflater inflater;

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
            /* TODO Move this and other collapsible text view code fragments to
             TextUtils, or create custom widget that encapsulates this functionality
             */
            vh.sectionDescription.setVisibility(View.VISIBLE);
            String descriptionWithOutExtraSpace = section.getSummary().trim();
            // The first object in the array is the full html description
            // The second represents the number of characters to be shown before the text is truncated.
            // The value is calculated and assigned when the text is set in makeTextViewResizable
            vh.sectionDescription.setTag(new Object[]{descriptionWithOutExtraSpace, -1});
            makeTextViewResizable(vh.sectionDescription, MAX_DESC_LINES, "Show More", true);
        } else {
            vh.sectionDescription.setVisibility(View.GONE);
        }
    }

    /**
     * Sets the text associated with the Textview and appends @param{}expandText to the text
     * @param description the Textview who's text is to be modified
     * @param maxLine the maximum number of lines to be shown inside the Textview
     * @param expandText the String that should be append to the end of the text. This text will be converted to a clickable span.
     * @param viewMore whether the clickable span will expand or limit text
     */
    public void makeTextViewResizable(final TextView description, final int maxLine, final String expandText, final boolean viewMore) {

        ViewTreeObserver vto = description.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            @Override
            public void onGlobalLayout() {
                String displayText = ""; /* The final text that is to be displayed  */
                String descriptionHtml = (String) ((Object[]) description.getTag())[0]; /* The description of the course section (with HTML tags) */
                int lineEndIndex = (int) ((Object[]) description.getTag())[1]; /* The number of characters that are shown in the description textview */

                String descriptionText = HtmlCompat.fromHtml(descriptionHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
                        .toString(); /* The description of the course section (with the HTML tags removed) */

                ViewTreeObserver obs = description.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
                if (viewMore) {
                    if (maxLine == 0) {
                        displayText = expandText;
                    } else if (description.getLineCount() > maxLine) {
                        if (lineEndIndex == -1) {
                            // We do it this way because the layout may change after the activity has
                            // been created and the text in description changes meaning the number
                            // of characters also changes. Ideally, the layout should be static.
                            // row_course_section->description has initial visibility GONE, which might be the culprit
                            lineEndIndex = description.getLayout().getLineEnd(maxLine - 1);
                            ((Object[]) description.getTag())[1] = lineEndIndex;
                        }

                        //int lineEndIndex = description.getLayout().getLineEnd(maxLine - 1); /* The maximum number of characters that the TextView can show at a time */
                        /* Find out how much of the final text would actually be visible and grab the HTML description till that part */
                        if (descriptionText.length() > lineEndIndex) {
                            /* Start from lineEndIndex and find out the next complete word */
                            String nearestWord = Util.nextNearestWord(descriptionText, lineEndIndex);
                            if (!nearestWord.isEmpty()) { /* if empty -> lineEndIndex is probably at the end of the string */
                                int occurrence = Util.countOccurrencesOfWord(descriptionText.substring(0, lineEndIndex + 1), nearestWord) + 1; /* Find the number of occurrences of the word in the truncated string, there will be at least one occurrence */
                                displayText = descriptionHtml.subSequence(0, Util.indexOfOccurrence(descriptionHtml, nearestWord, occurrence)).toString() + "\n" + expandText;
                            }
                            else {
                                displayText = descriptionHtml;
                            }

                        } else {
                            displayText = descriptionHtml;
                        }
                    } else if (description.getLineCount() <= maxLine) {
                        displayText = descriptionHtml;
                    }
                } else {
                    displayText = descriptionHtml + "\n" + expandText;
                }

                SpannableStringBuilder spanBuilder = new SpannableStringBuilder();
                spanBuilder.append(HtmlTextView.parseHtml(displayText));

                addClickablePartTextViewResizable(spanBuilder, description, expandText, viewMore);
                description.setMovementMethod(LinkMovementMethod.getInstance());
                description.setText(spanBuilder, TextView.BufferType.SPANNABLE);
            }
        });

        description.requestLayout(); /* Causes the view to be re-measured and re-drawn, which calls the GlobalLayoutListener we have defined above */
    }

    /**
     * Associates a <code>ClickableSpan</code> with <code>spannableText</code>
     * @param spannableStringBuilder The SpannableStringBuilder to which the clickable span is to be added
     * @param description The TextView to which the SpannableString will be set
     * @param spannableText The text to which the span should be assigned to
     * @param viewMore Whether clicking the span results in more text being visible or not
     * @return
     */
    private SpannableStringBuilder addClickablePartTextViewResizable(final SpannableStringBuilder spannableStringBuilder,
                                                                     final TextView description,
                                                                     final String spannableText,
                                                                     final boolean viewMore) {

        if (spannableStringBuilder.toString().contains(spannableText)) {
            spannableStringBuilder.setSpan(new ClickableSpan() {

                @Override
                public void onClick(@NotNull View widget) {
                    if (viewMore) {
                        makeTextViewResizable(description, -1, "Show Less", false);
                    } else {
                        makeTextViewResizable(description, MAX_DESC_LINES, "Show More", true);
                    }
                }

                @Override
                public void updateDrawState(@NotNull TextPaint textpaint) {
                    super.updateDrawState(textpaint);
                    TypedValue value = new TypedValue();
                    activity.getTheme().resolveAttribute(R.attr.colorAccent,value,true);
                    textpaint.setColor(value.data);
                    textpaint.setUnderlineText(false);
                    textpaint.setFakeBoldText(true);
                }
            }, spannableStringBuilder.toString().indexOf(spannableText), spannableStringBuilder.toString().indexOf(spannableText) + spannableText.length(), 0);

        }
        description.setHighlightColor(Color.TRANSPARENT);
        return spannableStringBuilder;

    }

    static class CourseSectionViewHolder extends RecyclerView.ViewHolder {
        public TextView sectionName;
        public TextView sectionDescription;

        public CourseSectionViewHolder(@NonNull View itemView) {
            super(itemView);
            sectionName = itemView.findViewById(R.id.section_name);
            sectionDescription = itemView.findViewById(R.id.description);
        }
    }
}

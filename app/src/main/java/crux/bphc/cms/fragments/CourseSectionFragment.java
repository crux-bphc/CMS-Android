package crux.bphc.cms.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.util.ArrayList;
import java.util.List;

import crux.bphc.cms.app.Constants;
import crux.bphc.cms.R;
import crux.bphc.cms.helper.ClickListener;
import crux.bphc.cms.helper.CourseDataHandler;
import crux.bphc.cms.helper.CourseRequestHandler;
import crux.bphc.cms.helper.HtmlTextView;
import crux.bphc.cms.helper.ModulesAdapter;
import crux.bphc.cms.helper.MyFileManager;
import crux.bphc.cms.helper.Util;
import models.CourseSection;
import models.Module;
import models.forum.Discussion;

import static crux.bphc.cms.helper.MyFileManager.DATA_DOWNLOADED;

/**
 * Created by SKrPl on 12/21/16.
 */

public class CourseSectionFragment extends Fragment {

    public static final int COURSE_DELETED = 102;
    private static final String TOKEN_KEY = "token";
    private static final String COURSE_ID_KEY = "id";
    private static final int MODULE_ACTIVITY = 101;

    View empty;
    MyFileManager mFileManager;
    List<CourseSection> courseSections;
    CourseDataHandler courseDataHandler;
    private String TOKEN;
    private int courseId;
    private LinearLayout linearLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private String courseName;
    private int maxDescriptionLines = 5;

    MoreOptionsFragment.OptionsViewModel moreOptionsViewModel;

    public static CourseSectionFragment newInstance(String token, int courseId) {
        CourseSectionFragment courseSectionFragment = new CourseSectionFragment();
        Bundle args = new Bundle();
        args.putString(TOKEN_KEY, token);
        args.putInt(COURSE_ID_KEY, courseId);
        courseSectionFragment.setArguments(args);
        return courseSectionFragment;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MODULE_ACTIVITY && resultCode == DATA_DOWNLOADED) {

            reloadSections();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            TOKEN = args.getString(TOKEN_KEY);
            courseId = args.getInt(COURSE_ID_KEY);
        }
        courseDataHandler = new CourseDataHandler(getActivity());
        mFileManager = new MyFileManager(getActivity(), CourseDataHandler.getCourseName(courseId));
        mFileManager.registerDownloadReceiver();
        courseSections = new ArrayList<>();
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        String title = courseDataHandler.getActionBarTitle(courseId);
        if(getActivity() != null) {
            getActivity().setTitle(title);
        }
        super.onStart();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_course_section, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        moreOptionsViewModel = new ViewModelProvider(requireActivity()).get(MoreOptionsFragment.OptionsViewModel.class);

        courseName = CourseDataHandler.getCourseName(courseId);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        linearLayout = view.findViewById(R.id.linearLayout);
        empty = view.findViewById(R.id.empty);
        courseSections = courseDataHandler.getCourseData(courseId);

        if (courseSections.isEmpty()) {
            mSwipeRefreshLayout.setRefreshing(true);
            sendRequest(courseId);
        }
        for (CourseSection section : courseSections) {
            addSection(section);
        }

//        sendRequest(courseId);
        mFileManager.setCallback(new MyFileManager.Callback() {
            @Override
            public void onDownloadCompleted(String fileName) {
                reloadSections();
            }
        });

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.setRefreshing(true);
                sendRequest(courseId);
            }
        });

        checkEmpty();
    }

    private boolean checkEmpty() {
        for (CourseSection courseSection : courseSections) {
            if (!courseSection.getModules().isEmpty()) {
                empty.setVisibility(View.GONE);
                return false;
            }
        }
        empty.setVisibility(View.VISIBLE);
        ((TextView) empty).setText("No Course Data to display.\nTap to Reload");
        return true;
    }

    private void reloadSections() {
        mFileManager.reloadFileList();
        linearLayout.removeAllViews();
        for (CourseSection section : courseSections) {
            addSection(section);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mFileManager.unregisterDownloadReceiver();
    }

    private void sendRequest(final int courseId) {

        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        courseRequestHandler.getCourseData(courseId, new CourseRequestHandler.CallBack<List<CourseSection>>() {
            @Override
            public void onResponse(List<CourseSection> sectionList) {
                empty.setVisibility(View.GONE);

                if (sectionList == null) {
                    //todo not registered, ask to register, change UI, show enroll button
                    return;
                }
                for (CourseSection courseSection : sectionList) {
                    List<Module> modules = courseSection.getModules();
                    for (Module module : modules) {
                        if (module.getModType() == Module.Type.FORUM) {
                            courseRequestHandler.getForumDiscussions(module.getInstance(), new CourseRequestHandler.CallBack<List<Discussion>>() {
                                @Override
                                public void onResponse(List<Discussion> responseObject) {
                                    for (Discussion d : responseObject) {
                                        d.setForumId(module.getInstance());
                                    }
                                    List<Discussion> newDiscussions = courseDataHandler.setForumDiscussions(module.getInstance(), responseObject);
                                    if (newDiscussions.size() > 0) courseDataHandler.markAsReadandUnread(module.getId(), true);
                                }

                                @Override
                                public void onFailure(String message, Throwable t) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        }
                    }
                }
                linearLayout.removeAllViews();
                courseSections.clear();
                courseDataHandler.setCourseData(courseId, sectionList);

                for (CourseSection section : sectionList) {
                    courseSections.add(section);
                    addSection(section);
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(String message, Throwable t) {
                if (t instanceof IllegalStateException) {
                    //course unenrolled. delete course details, open enroll screen
                    courseDataHandler.deleteCourse(courseId);
                    Toast.makeText(getActivity(), "you have un-enrolled from the course", Toast.LENGTH_SHORT).show();
                    getActivity().setResult(COURSE_DELETED);
                    getActivity().finish();
                    return;
                }
                if (courseSections.isEmpty()) {
                    ((TextView) empty).setText("No internet connection.\nTap to retry");
                    empty.setVisibility(View.VISIBLE);
                    empty.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mSwipeRefreshLayout.setRefreshing(true);
                            sendRequest(courseId);
                            linearLayout.setOnClickListener(null);
                        }
                    });
                }
                Toast.makeText(getActivity(), "Unable to connect to server!", Toast.LENGTH_SHORT).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void addSection(final CourseSection section) {
        if (linearLayout == null || getActivity() == null)
            return;

        if ((section.getModules() == null || section.getModules().isEmpty())
                && (section.getSummary() == null || section.getSummary().isEmpty())
                    && (section.getName().matches("Topic \\d+"))) {
            return;
        }

        View v = LayoutInflater.from(getActivity()).inflate(R.layout.row_course_section, linearLayout, false);

        ((TextView) v.findViewById(R.id.sectionName)).setText(section.getName());
        /*v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), CourseModulesActivity.class);
                intent.putExtra("id", section.getId());
                startActivityForResult(intent, MODULE_ACTIVITY);
                getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });*/
        if (!section.getSummary().isEmpty()) {
            v.findViewById(R.id.description).setVisibility(View.VISIBLE);
            String descriptionWithOutExtraSpace = addToken(section.getSummary().trim());
            TextView textView = v.findViewById(R.id.description);

            // The first object in the array is the full html description
            // The second represents the number of characters to be shown before the text is truncated.
            // The value is calculated and assigned when the text is set in makeTextViewResizable
            textView.setTag(new Object[] {descriptionWithOutExtraSpace, -1});

            makeTextViewResizable(textView, maxDescriptionLines, "Show More", true);
        }else{
            v.findViewById(R.id.description).setVisibility(View.GONE);
        }

        RecyclerView recyclerView = v.findViewById(R.id.recyclerView);

        final ModulesAdapter myAdapter = new ModulesAdapter(getContext(), mFileManager, courseName,
                courseId, moreOptionsViewModel);
        myAdapter.setModules(section.getModules());
        recyclerView.setAdapter(myAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setNestedScrollingEnabled(false);

//        recyclerView.addItemDecoration(new DividerItemDecoration(getContext(), RecyclerView.VERTICAL));

        myAdapter.setClickListener(new ClickListener() {
            @Override
            public boolean onClick(Object object, int position) {
                if (object instanceof Module) {
                    return mFileManager.onClickAction((Module) object, courseName);
                }
                return false;
            }
        });
        linearLayout.addView(v);
    }

    private String addToken(String descriptionWithOutExtraSpace) {
        int start = 0;
        while (start != -1 && start < descriptionWithOutExtraSpace.length()) {
            start = descriptionWithOutExtraSpace.indexOf("<a href=\"", start);
            if (start == -1) {
                break;
            }
            start += "<a href=\"".length();
            int end = descriptionWithOutExtraSpace.indexOf("\"", start);
            String oldhref = descriptionWithOutExtraSpace.substring(start, end);
            String href = oldhref;
            if (href.contains("?")) {
                href = href.concat("&token=" + TOKEN);
            } else {
                href = href.concat("?token=" + TOKEN);
            }
            descriptionWithOutExtraSpace = descriptionWithOutExtraSpace.replace(oldhref, href);
            end = descriptionWithOutExtraSpace.indexOf("\"", start);
            start = end + 1;
        }
        return descriptionWithOutExtraSpace;

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

                String descriptionText = Html.fromHtml(descriptionHtml).toString(); /* The description of the course section (with the HTML tags removed) */

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
    private SpannableStringBuilder addClickablePartTextViewResizable(
            final SpannableStringBuilder spannableStringBuilder, final TextView description, final String spannableText, final boolean viewMore) {

        if (spannableStringBuilder.toString().contains(spannableText)) {
            spannableStringBuilder.setSpan(new ClickableSpan() {

                @Override
                public void onClick(View widget) {
                    if (viewMore) {
                        makeTextViewResizable(description, -1, "Show Less", false);
                    } else {
                        makeTextViewResizable(description, maxDescriptionLines, "Show More", true);
                    }
                }

                @Override
                public void updateDrawState(TextPaint textpaint) {
                    super.updateDrawState(textpaint);
                    TypedValue value = new TypedValue();
                    getContext().getTheme().resolveAttribute(R.attr.colorAccent,value,true);
                    textpaint.setColor(value.data);
                    textpaint.setUnderlineText(false);
                    textpaint.setFakeBoldText(true);
                }
            }, spannableStringBuilder.toString().indexOf(spannableText), spannableStringBuilder.toString().indexOf(spannableText) + spannableText.length(), 0);

        }
        description.setHighlightColor(Color.TRANSPARENT);
        return spannableStringBuilder;

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.course_details_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mark_all_as_read) {
            courseDataHandler.markAllAsRead(courseSections);
            courseSections = courseDataHandler.getCourseData(courseId);
            reloadSections();
            Toast.makeText(getActivity(), "Marked all as read", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (item.getItemId() == R.id.action_open_in_browser) {
            MyFileManager.showInWebsite(getActivity(), Constants.getCourseURL(courseId));
        }
        return super.onOptionsItemSelected(item);
    }
}

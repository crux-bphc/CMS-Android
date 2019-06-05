package crux.bphc.cms.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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

import java.util.ArrayList;
import java.util.List;

import app.Constants;
import crux.bphc.cms.R;
import helper.ClickListener;
import helper.CourseDataHandler;
import helper.CourseRequestHandler;
import helper.ModulesAdapter;
import helper.MyFileManager;
import set.CourseSection;
import set.Module;
import set.forum.Discussion;

import static helper.MyFileManager.DATA_DOWNLOADED;

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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_course_section, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

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
                && (section.getSummary() == null || section.getSummary().isEmpty())) {
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
            Spanned htmlDescription = Html.fromHtml(addToken(section.getSummary().trim()));
            String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
            TextView textView = v.findViewById(R.id.description);
            textView.setText(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
            textView.setMovementMethod(LinkMovementMethod.getInstance());
            textView.setLinksClickable(true);
            textView.setTag(textView.getText());
            makeTextViewResizable(textView, maxDescriptionLines, "Show More", true);
        }
        RecyclerView recyclerView = v.findViewById(R.id.recyclerView);

        final ModulesAdapter myAdapter = new ModulesAdapter(getContext(), mFileManager, courseName);
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

    public void makeTextViewResizable(final TextView description, final int maxLine, final String expandText, final boolean viewMore) {

        if (description.getTag() == null) {
            description.setTag(description.getText());
        }
        ViewTreeObserver vto = description.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {


            @Override
            public void onGlobalLayout() {
                String text;
                int lineEndIndex;
                ViewTreeObserver obs = description.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);
                if (maxLine == 0) {
                    text = expandText;
                } else if (maxLine > 0 && description.getLineCount() > maxLine) {
                    lineEndIndex = description.getLayout().getLineEnd(maxLine - 1);
                    text = description.getText().subSequence(0, lineEndIndex) + "\n" + expandText;
                } else if (description.getLineCount() <= maxLine) {
                    text = description.getText().toString();
                } else {
                    lineEndIndex = description.getLayout().getLineEnd(description.getLayout().getLineCount() - 1);
                    text = description.getText().subSequence(0, lineEndIndex) + "\n" + expandText;
                }
                description.setText(text);
                description.setMovementMethod(LinkMovementMethod.getInstance());
                description.setText(
                        addClickablePartTextViewResizable(description.getText().toString(), description, expandText,
                                viewMore), TextView.BufferType.SPANNABLE);
            }
        });

    }

    private SpannableStringBuilder addClickablePartTextViewResizable(
            final String spannedString, final TextView description, final String spanableText, final boolean viewMore) {

        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(spannedString);

        if (spannedString.contains(spanableText)) {
            spannableStringBuilder.setSpan(new ClickableSpan() {

                @Override
                public void onClick(View widget) {

                    description.setLayoutParams(description.getLayoutParams());
                    description.setText(description.getTag().toString(), TextView.BufferType.SPANNABLE);
                    description.invalidate();
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
            }, spannedString.indexOf(spanableText), spannedString.indexOf(spanableText) + spanableText.length(), 0);

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
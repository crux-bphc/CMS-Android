package crux.bphc.cms.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import crux.bphc.cms.R;
import crux.bphc.cms.adapters.CourseContentAdapter;
import crux.bphc.cms.app.Constants;
import crux.bphc.cms.app.MyApplication;
import crux.bphc.cms.interfaces.ClickListener;
import crux.bphc.cms.helper.CourseDataHandler;
import crux.bphc.cms.helper.CourseRequestHandler;
import crux.bphc.cms.io.FileManager;
import crux.bphc.cms.widgets.PropertiesAlertDialog;
import crux.bphc.cms.utils.Utils;
import crux.bphc.cms.interfaces.CourseContent;
import crux.bphc.cms.models.course.Content;
import crux.bphc.cms.models.course.CourseSection;
import crux.bphc.cms.models.course.Module;
import crux.bphc.cms.models.forum.Discussion;
import io.realm.Realm;

import static crux.bphc.cms.io.FileManager.DATA_DOWNLOADED;
import static crux.bphc.cms.models.course.Module.Type.FORUM;

/**
 * @author Siddhant Kumar Patel, Abhijeet Viswa
 */

public class CourseContentFragment extends Fragment {

    public static final int COURSE_DELETED = 102;
    private static final String TOKEN_KEY = "token";
    private static final String COURSE_ID_KEY = "id";
    private static final int MODULE_ACTIVITY = 101;

    private FileManager fileManager;
    private CourseDataHandler courseDataHandler;
    private Realm realm;

    private int courseId;
    private String courseName;

    List<CourseSection> courseSections;

    View empty;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView recyclerView;
    private CourseContentAdapter adapter;

    MoreOptionsFragment.OptionsViewModel moreOptionsViewModel;
    private final ClickListener  moduleClickWrapperClickListener= createModuleClickWrapperClickListener();
    private final ClickListener moduleMoreOptionsClickListener = createModuleMoreOptionsClickListener();

    public static CourseContentFragment newInstance(String token, int courseId) {
        CourseContentFragment courseSectionFragment = new CourseContentFragment();
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
            setCourseContentsOnAdapter();
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            courseId = args.getInt(COURSE_ID_KEY);
        }
        // Initialize realm here instead of onCreateView so that other classes can be initialized
        realm = Realm.getDefaultInstance();
        courseDataHandler = new CourseDataHandler(requireActivity(), realm);
        courseName = courseDataHandler.getCourseName(courseId);

        fileManager = new FileManager(requireActivity(), courseName);
        fileManager.registerDownloadReceiver();
        courseSections = new ArrayList<>();
        setHasOptionsMenu(true);
    }

    @Override
    public void onStart() {
        String title = courseDataHandler.getCourseNameForActionBarTitle(courseId);
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
    public void onViewCreated(@NotNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        moreOptionsViewModel = new ViewModelProvider(requireActivity()).get(MoreOptionsFragment.OptionsViewModel.class);

        empty = view.findViewById(R.id.empty);
        mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        recyclerView = view.findViewById(R.id.recycler_view);

        courseSections = courseDataHandler.getCourseData(courseId);
        if (courseSections.isEmpty()) {
            mSwipeRefreshLayout.setRefreshing(true);
            sendRequest(courseId);
        }

        adapter = new CourseContentAdapter(requireActivity(), getCourseContents(), fileManager,
                moduleClickWrapperClickListener, moduleMoreOptionsClickListener);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setItemViewCacheSize(5);

        fileManager.setCallback(fileName -> setCourseContentsOnAdapter());

        mSwipeRefreshLayout.setOnRefreshListener(() -> {
            mSwipeRefreshLayout.setRefreshing(true);
            sendRequest(courseId);
        });

        empty.setOnClickListener(v -> {
            mSwipeRefreshLayout.setRefreshing(true);
            sendRequest(courseId);
        });

        showSectionsOrEmpty();
    }

    private void showSectionsOrEmpty() {
        if (courseSections.stream().anyMatch(section -> !section.getModules().isEmpty())) {
            empty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            return;
        }
        empty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    private ClickListener createModuleMoreOptionsClickListener() {
        return (object, position) -> {
            final MoreOptionsFragment.OptionsViewModel moreOptionsViewModel =
                    CourseContentFragment.this.moreOptionsViewModel;
            Observer<MoreOptionsFragment.Option> observer;  // to handle the selection

            Module module = (Module) object;

            Content content = module.getContents().first();
            boolean downloaded = content != null && fileManager.isModuleContentDownloaded(content);

            /* Set up our options and their handlers */
            ArrayList<MoreOptionsFragment.Option> options = new ArrayList<>();
            if (downloaded) {
                options.addAll(Arrays.asList(
                        new MoreOptionsFragment.Option(0, "View", R.drawable.eye),
                        new MoreOptionsFragment.Option(1, "Re-Download", R.drawable.download),
                        new MoreOptionsFragment.Option(2, "Share", R.drawable.ic_share),
                        new MoreOptionsFragment.Option(3, "Mark as Unread", R.drawable.eye_off)
                ));
                if (module.getModType() == Module.Type.RESOURCE) {
                    options.add(new MoreOptionsFragment.Option(
                            4, "Properties", R.drawable.ic_info));
                }
                observer = option -> {
                    if (option == null)
                        return;
                    switch (option.getId()) {
                        case 0:
                            fileManager.openModuleContent(content);
                            break;
                        case 1:
                            if (!module.isDownloadable()) {
                                return;
                            }
                                Toast.makeText(getActivity(), "Downloading file - " + content.getFileName(),
                                        Toast.LENGTH_SHORT).show();
                                fileManager.downloadModuleContent(content, module);
                            break;
                        case 2:
                            fileManager.shareModuleContent(content);
                            break;
                        case 3:
                            courseDataHandler.markModuleAsReadOrUnread(module, true);
                            adapter.notifyItemChanged(position);
                            break;
                        case 4:
                            new PropertiesAlertDialog(getActivity(), content).show();
                            break;
                    }
                    if (getActivity() != null) {
                        moreOptionsViewModel.getSelection().removeObservers(getActivity());
                    }
                    moreOptionsViewModel.clearSelection();
                };
            } else {
                options.addAll(Arrays.asList(
                        new MoreOptionsFragment.Option(0, "Download", R.drawable.download),
                        new MoreOptionsFragment.Option(1, "Share", R.drawable.ic_share),
                        new MoreOptionsFragment.Option(2, "Mark as Unread", R.drawable.eye_off)
                ));
                if (module.getModType() == Module.Type.RESOURCE) {
                    options.add(new MoreOptionsFragment.Option(
                            3, "Properties", R.drawable.ic_info));
                }
                observer = option -> {
                    if (option == null)
                        return;
                    FragmentActivity activity = getActivity();
                    switch (option.getId()) {
                        case 0:
                            if (content != null) {
                                fileManager.downloadModuleContent(content, module);
                            }
                            break;
                        case 1:
                            shareModuleLinks(module);
                            break;
                        case 2:
                            courseDataHandler.markModuleAsReadOrUnread(module, true);
                            break;
                        case 3:
                            if (content != null && activity != null) {
                                new PropertiesAlertDialog(activity, content).show();
                            }
                            break;
                    }
                    if (activity != null) {
                        moreOptionsViewModel.getSelection().removeObservers(activity);
                    }
                    moreOptionsViewModel.clearSelection();
                };
            }

            /* Show the fragment and register the observer */
            FragmentActivity activity = getActivity();
            if (activity != null) {
                MoreOptionsFragment moreOptionsFragment = MoreOptionsFragment.newInstance(module.getName(), options);
                moreOptionsFragment.show(activity.getSupportFragmentManager(), moreOptionsFragment.getTag());
                moreOptionsViewModel.getSelection().observe(activity, observer);
                courseDataHandler.markModuleAsReadOrUnread(module, false);
                adapter.notifyItemChanged(position);
            }
            return true;
        };
    }

    private ClickListener createModuleClickWrapperClickListener() {
        return (object, position) -> {
            Module module = (Module) object;
            FragmentActivity activity = getActivity();
            Content content = !module.getContents().isEmpty() ? module.getContents().first() : null;
            switch (module.getModType()) {
                case URL:
                    if (activity != null && content != null) {
                        String url = content.getFileUrl();
                        if (url != null && !url.isEmpty()) {
                            Utils.openURLInBrowser(activity, url);
                        }
                    }
                    break;
                case PAGE:
                    String url = module.getUrl();
                    if (activity != null && url != null) {
                        Utils.openURLInBrowser(activity, module.getUrl());
                    }
                    break;
                case FORUM:
                case FOLDER:
                    if (activity != null) {
                        Fragment fragment = module.getModType() == FORUM
                                ? ForumFragment.newInstance(module.getInstance(), courseName)
                                : FolderModuleFragment.newInstance(module.getInstance(), courseName);
                        activity.getSupportFragmentManager()
                                .beginTransaction()
                                .addToBackStack(null)
                                .replace(R.id.course_section_enrol_container, fragment, "Announcements")
                                .commit();
                    }
                    break;
                case LABEL:
                    String desc = module.getDescription();
                    if (activity != null && desc != null && !desc.isEmpty()) {

                        AlertDialog.Builder alertDialog;
                        if (MyApplication.getInstance().isDarkModeEnabled()) {
                            alertDialog = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_Dialog_Alert);
                        } else {
                            alertDialog = new AlertDialog.Builder(activity, R.style.Theme_AppCompat_Light_Dialog_Alert);
                        }

                        Spanned htmlDescription = HtmlCompat.fromHtml(module.getDescription(),
                                HtmlCompat.FROM_HTML_MODE_COMPACT);
                        String descriptionWithOutExtraSpace = htmlDescription.toString().trim();
                        alertDialog.setMessage(htmlDescription.subSequence(0, descriptionWithOutExtraSpace.length()));
                        alertDialog.setNegativeButton("Close", null);
                        alertDialog.show();
                    }
                    break;
                case RESOURCE:
                    if (content != null) {
                        if (fileManager.isModuleContentDownloaded(content)) {
                            fileManager.openModuleContent(content);
                        } else {
                            Toast.makeText(getActivity(), "Downloading file - " + content.getFileName(),
                                    Toast.LENGTH_SHORT).show();
                            fileManager.downloadModuleContent(content, module);
                        }
                    }
                        break;
            }
            courseDataHandler.markModuleAsReadOrUnread(module, false);
            return true;
        };
    }

    private void shareModuleLinks(Module module) {
        Content content = !module.getContents().isEmpty() ? module.getContents().first() : null;
        if (content == null)
                return;

        String toShare = content.getFileUrl().replace("/moodle", "/fileShare/moodle") +
                        "&courseName=" + courseName.replace(" ", "%20") + "&courseId=" + courseId;
        Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
        sharingIntent.setType("text/plain");
        sharingIntent.putExtra(Intent.EXTRA_TEXT, toShare);
        if (getContext() != null)
            getContext().startActivity(Intent.createChooser(sharingIntent, null));
    }

    private void sendRequest(final int courseId) {

        CourseRequestHandler courseRequestHandler = new CourseRequestHandler(getActivity());
        courseRequestHandler.getCourseData(courseId, new CourseRequestHandler.CallBack<List<CourseSection>>() {
            @Override
            public void onResponse(List<CourseSection> sectionList) {
                empty.setVisibility(View.GONE);
                recyclerView.setVisibility(View.VISIBLE);

                if (sectionList == null) {
                    //todo not registered, ask to register, change UI, show enroll button
                    return;
                }
                for (CourseSection courseSection : sectionList) {
                    List<Module> modules = courseSection.getModules();
                    for (Module module : modules) {
                        if (module.getModType() == FORUM) {
                            courseRequestHandler.getForumDiscussions(module.getInstance(), new CourseRequestHandler.CallBack<List<Discussion>>() {
                                @Override
                                public void onResponse(List<Discussion> responseObject) {
                                    for (Discussion d : responseObject) {
                                        d.setForumId(module.getInstance());
                                    }
                                    List<Discussion> newDiscussions = courseDataHandler.setForumDiscussions(module.getInstance(), responseObject);
                                    if (newDiscussions.size() > 0) courseDataHandler.markModuleAsReadOrUnread(module, true);
                                }

                                @Override
                                public void onFailure(String message, Throwable t) {
                                    mSwipeRefreshLayout.setRefreshing(false);
                                }
                            });
                        }
                    }
                }
                courseSections = sectionList;
                courseDataHandler.replaceCourseData(courseId, sectionList);
                setCourseContentsOnAdapter();
                mSwipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(String message, Throwable t) {
                if (t instanceof IllegalStateException) {
                    //course unenrolled. delete course details, open enroll screen
                    courseDataHandler.deleteCourse(courseId);
                    Toast.makeText(getActivity(), "you have un-enrolled from the course", Toast.LENGTH_SHORT).show();
                    requireActivity().setResult(COURSE_DELETED);
                    requireActivity().finish();
                    return;
                }
                if (courseSections.isEmpty()) {
                    ((TextView) empty).setText("No internet connection.\nTap to retry");
                    empty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
                Toast.makeText(getActivity(), "Unable to connect to server!", Toast.LENGTH_SHORT).show();
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void setCourseContentsOnAdapter() {
        fileManager.reloadFileList();
        adapter.setCourseContents(getCourseContents());
    }

    private List<CourseContent> getCourseContents() {
        ArrayList<CourseContent> contents = new ArrayList<>();
        courseSections.stream().filter(courseSection -> !(courseSection.getModules().isEmpty()
                && (courseSection.getSummary() == null || courseSection.getSummary().isEmpty())
                && (courseSection.getName().matches("Topic \\d"))
        )).forEach(courseSection -> {
            contents.add(courseSection);
            contents.addAll(courseSection.getModules());
        });
        return contents;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.mark_all_as_read) {
            courseDataHandler.markAllAsRead(courseSections);
            courseSections = courseDataHandler.getCourseData(courseId);
            setCourseContentsOnAdapter();
            Toast.makeText(getActivity(), "Marked all as read", Toast.LENGTH_SHORT).show();
            return true;
        }
        if (item.getItemId() == R.id.action_open_in_browser) {
            Utils.openURLInBrowser(requireActivity(), Constants.getCourseURL(courseId));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(@NotNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.course_details_menu, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fileManager.unregisterDownloadReceiver();
        realm.close();
    }
}

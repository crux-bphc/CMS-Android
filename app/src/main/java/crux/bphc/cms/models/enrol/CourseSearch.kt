package crux.bphc.cms.models.enrol

import com.google.gson.annotations.SerializedName

/**
 * Model class to represent response from [crux.bphc.cms.network.MoodleServices.searchCourses].
 * @author Siddhant Kumar Patel (17-Dec-2016)
 * @author Abhijeet Viswa
 */
data class CourseSearch(
        @SerializedName("total") val total: Int = 0,
        @SerializedName("courses") val courses: List<SearchedCourseDetail> = emptyList(),
)
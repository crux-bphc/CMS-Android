# Changelog

All notable changes to the project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) standards.

## Unreleased
### Added
* 'Mark as unread' option for modules in a course
* Notification Channel support for Oreo and above.
* Support for Android Pie (API 28)
* 'Announcements' and other forum modules can now be viewed in-app.
* Custom `TextView` to display HTML content, rather than parsing each time.
* Support for multiple files within a course to have the same file name.

### Changed
* Swipe to refresh on 'My Courses' syncs changes in all courses. Only refreshed list of courses earlier.
* API served URL changed from "https://id.bits-hyderabad.ac.in/moodle/" to "https://td.bits-hyderabad.ac.in/moodle/"
* Notifications are 'bundled' with a group summary for Android Nougat and above. For lower APIs, each notification is shown separately.
* Refactored `SiteNewsFragment` to `ForumFragment` that supports any forum based on the `forumid` parameter. The older `ForumFragment` is now `DiscussionFragment`.

### Removed
* All usages of `WebView` in the app. All redirects now use the default browser.
* Notification during content sync. `NotificationService` is now used as a proper background service, not as a forground service.

### Fixed
* Notifications not opening the app on being clicked.
* Improper condition while checking if a course is newly added.
* Repeated crashes while syncing updates.
* Bug in SiteNews pagination where only the latest page is displayed and cached.
* Files list for course was being generated using entire CMS directory.
* Added a missing Toast when starting file download from a forum attachment.
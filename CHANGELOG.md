# Changelog

All notable changes to the project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) standards.
Dates are in `yyyy-mm-dd`.

## Unreleased

*NOTE: The minimum supported version is now Android 7.0 (API Level 24)*

### Added
* Add APK version and commit hash to navigation drawer
* Ability to mark courses as favorite for easier access

### Changed
* Launcher icon

### Fixed

* Full Course Names not being displayed (regression in **v1.7.0-beta1**)
* Module Read/Unread Visual Bug (regression in **v1.7.0-beta1**)
## Version 1.7.0-beta.1 (verCode 1070001), 2020-05-09
### Added
* Dialogbox on invalid token after Google login
* Discussion notifications now open up the discussion
* Each screen from the main activity will set its own title

### Changed
* Massive UI changes
* Replaced custom Settings activity with Android's Preference Fragment
* Re-download option rewrites the file
* Replace dialog boxes with bottom sheets
* Show only Course and Section number as title of Course fragment

### Fixed
* HTML encoded entities in course names
* Unparsed HTML in notification summary
* Empty ghost site notifications

## Version 1.6.0 (verCode 1060000), 2019-11-10
### Added
* Google Login
* Ability to share modules without having to download them
* Fragment for Folder Modules
* Notifications for Site News
* File properties in the `More Options` ellipsis

### Changed
* Module names are clickable
* Discussion Attachment names are clickable

### Fixed
* Crash when refreshing discussions and switching to another fragment/activity

### Removed
* Token based login

## Version 1.5.1 (verCode 1050100), 2019-08-15
### Added
* Dark theme for the app, with a toggle in the settings. System preview window will be dark by default.
* Splashscreen for the app
* Ellipsis in course discussions attachments to view, re-download and share downloaded attachment

### Changed
* Change the text of the information in the "About us" section.
* Replace Octocat with the logo of Crux in the "About us" section.

### Fixed
* Repeated notifications of very old forum discussions
* Hyperlinks not clickable in forums posts and course section descriptions #92
* Navigation Bar not updating when back button is pressed
* Unintuitive back button behaviour in the main activity

## Version 1.4.1 (verCode 8), 2018-09-08
### Added
* Notifications for forum posts.
* "Help" button with instructions on logging in using a Token.

### Changed
* `API_URL` from `http` to `https`. This fixes the Cleartext traffic exceptions on Android 9.

### Fixed
* Course names weren't sanitized to proper directory names before passing to `searchFile`, causing a bug where downloaded files couldn't be found.

## Version 1.4.0 (verCode 7), 2018-09-05
### Added
* 'Mark as unread' option for modules in a course
* Notification Channel support for Oreo and above.
* Support for Android Pie (API 28)
* 'Announcements' and other forum modules can now be viewed in-app.
* Custom `TextView` to display HTML content, rather than parsing each time.
* Support for multiple files within a course to have the same file name.
* "Show More/Less" option for longer descriptions.

### Changed
* Swipe to refresh on 'My Courses' syncs changes in all courses. Only refreshed list of courses earlier.
* API served URL changed from "http://id.bits-hyderabad.ac.in/moodle/" to "http://td.bits-hyderabad.ac.in/moodle/"
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

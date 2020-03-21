# CMS-Android
[![Build Status](https://travis-ci.org/crux-bphc/CMS-Android.svg?branch=development)](https://travis-ci.org/crux-bphc/CMS-Android)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

<a href ="https://play.google.com/store/apps/details?id=crux.bphc.cms" target="_blank"><img src="google_play_badge.svg" alt="Get it on Google Play!" height="80"/></a>

This is the Android version of the CMS BPHC app. The app uses standard Moodle endpoints from the server hosted at the [CMS website](https://td.bits-hyderabad.ac.in/moodle/).

## Features
1. Token based login
2. View list of enrolled courses
3. Search and register new courses
4. Download course content once; access locally any time.
5. Notifications for new content

## Versioning
Changelogs are necessary so that the end user knows what changes were brought in each new release. This project maintains changelogs using the [Keep a Changelog](https://keepachangelog.com/en/1.0.0/) guidelines.

Version numbers are important in helping to differentiate releases of a product. This project follows [Semantic Versioning](https://semver.org/) to keep track of versions.

Android uses a unique integer for each release to the Google Version Code. To ensure proper version code semantics and proper correlation with semantic versioning, the following standard is used:

1. The Major version will be multipled by 1,000,000.
2. The Minor version will be multiplied by 10,000.
3. The Patch version will be multipled by 100.
4. The above values will be added together to get the version code a release.
5. Pre-releases, alpha releases etc shoud be signified by appropriate values in the least two significant digits.
6. The changelog should state the semantic version number as well as version code (if a Playstore rollout has been done) with each release.

## Contributing
You can contribute by submitting an issue, or by picking an existing one from the list and sending a PR. Ensure that the branch underlying the PR is up-to-date with latest changes. Contributors must ensure they add any significant changes to the [changelog](CHANGELOG.md) in the same PR itself.

All changes must be made on top of a new branch forked off the `development` branch. The `master` branch is only updated when a new release is made.

Commit messages should follow common guidelines, such as the ones mentioned [here](https://chris.beams.io/posts/git-commit/), whenever possible.

Mention keywords such as "Fixes" or "Closes" in commit messages, followed by the issue number, to automatically close corresponding issues. [(List of keywords)](https://help.github.com/articles/closing-issues-using-keywords/)

The Moodle endpoints used by this app have been collected in a [Postman](https://getpostman.com") [Here](https://www.getpostman.com/collections/e2c0439f144f7d3f60ed). You may import this collection into Postman directly using the link. 

## License
This app is under the MIT License.

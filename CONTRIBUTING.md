# Contributing to CMS-Android

Thank you for taking time to contribute (or at least considering to). CMS-Android is a community project as much as
it is Crux's.

Contribution does not necessarily mean you have to modify source code and fix a bug or add a new functionality.
Reporting a bug using the [Issues][issues] tab is also highly appreciated  and a good way to start contributing to
the project.

It is essential that you know how to use Git as well as GitHub. While the maintainers will be more than happy helping
you make your first PR, prerequisite know-how makes it easier for everyone involved. Check this
[guide][first-contributions-guide]  if you've never contributed to a project on GitHub before.

Please note we have a [Code of Conduct][code-of-conduct]. Please follow it in all your interactions with the project.

When you submit code changes, your submissions are understood to be under the same [MIT License][license] that covers
the  project. Feel free to contact the maintainers if that's a concern.

[issues]: https://github.com/crux-bphc/CMS-Android/issues
[first-contributions-guide]: https://github.com/firstcontributions/first-contributions


## Moodle Endpoints Used By The App

The Moodle endpoints used by this app have been collected in a [Postman][postman] workspace
[here][postman-collection]. You may import this collection into Postman directly
using the link.

[postman]: https://getpostman.com
[postman-collection]: https://www.getpostman.com/collections/e2c0439f144f7d3f60ed

## Contributors Pull Request Checklist

1. Fork the repository and branch from `development`.

2. Work on your changes. Make sure you commit as frequently as required. Once you're done making the changes, push the
   commits to your fork. Checkout the [Commit Messages](#commit-messages) section to find guidelines relating to commit
   messages.

3. If the changes you make fixes a bug in `master` or adds a new feature, note it down in the [changelog][changelog].

4. Head over to the [pull requests][pull-requests] page and create a new pull request.
   Make sure that the PR is from your feature branch into this repo's `development` branch. If you are making changes
   that affect the UI in any way, attach an image in your PR description.

5. Wait for a maintainer to review your commit. If they find any issue (including redundant code, unnecessary changes,
   potential bugs), they will point it out to you. Make any necessary changes and push them to your fork. Ideally, try
   not to force push as it becomes harder for the maintainers to keep track of changes. Mark the review as resolved.

6. If you are a maintainer yourself, try to get a secondary opinion from other maintainers or contributors.

7. Checkout the [Multi Commit PRs][#multi-commit-prs] section for guidelines on how to deal with PRs that have multiple
   commits.

8. If everything looks okay, the maintainer(s) will go ahead and merge your changes into the repo.

**Note for maintainers**: When merging a PR in, *rebase the changes instead of creating a merge commit*. This ensures a
clean commit history. Squash and rebase if required. You may also ask the contributor to do this if you are unsure how
best to squash the commits. Changes introduced by a commit must be granular so that diagnosing and fixing problems later
on is as simple as rolling back a specific commit.


[pull-requests]: https://github.com/crux-bphc/CMS-Android/pulls

## Commit Messages

Proper commit messages (subject + body) is a very important aspect of any project. A good commit message communicates
the exact details of the commit to other people without them having to go through the changes made. Good commit messages
will be a joy to read in `git log`.

The subject of a commit message should be:

- capitalized
- written in the imperative (e.g., "Fix ...", "Add ...")
- kept short, while concisely explaining what the commit does.
- clear about what part of the code is affected
- a complete sentence

Consider the following commit subjects:

- `Course enrollment was broken`:  does not explain how it was broken (and isn't in the imperative)
- `Fixing exception when given bad input`: impossible to tell from the summary what part of the code is
   affected

The body of a commit must explain the _what_ and _why_ of the commit instead of the _how_. For example, in a commit
that fixes a certain bug, you must explain _what_ was causing the bug and (possibly) _why_ it was fixed in a particular
way. The _how_ aspect of your commit/changes should be self-explanatory i.e you shouldn't need to explain how your changes
fix the bug. If you feel that your commit needs to include a _how_ as well, that indicates a refactoring of your
commit is in order.

Make use of [closing keywords][closing-keywords] in your commit if your PR closes a particular issue. Alternatively, link
the PR with the relevant issue.  This allows an issue to be automatically closed once the relevant PR is merged.

Also checkout [this blogpost][commit-message-guidelines] for further reading regarding commit messages.

[commit-message-guidelines]: https://chris.beams.io/posts/git-commit/
[closing-keywords]: https://help.github.com/en/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue


## Multi Commit PRs

**Note**: This section has concepts that may be too advanced for new Git users. It is recommended that such contributors
ask for help from those more experienced.

Sometimes a PR will require or endup with more than one commit. There are two possibilities of this happening:

* Additional granularity that each commit provides to a feature branch. An example of this is when you make big
  changes  to the UI to follow a new set of guidelines or standard. In such a case, it may be beneficial to have separate
  commits for each fragment/activity that is changed.

* New commits pushed after a review from the maintainer(s). New commits are recommended over amending existing commit(s)
  and force pushing since the latter would make your changes a lot harder to review.

Note that in both cases, only a single PR is required. It is the PR that will have multiple commits. In both cases, our
concern is the additional commits that are added to a PR to fix problems found during code review.

Consider the case of a PR with two commits, `A` and `B`. After a code review, let two additional commits `C` and `D` be
created to fix problems found in `A` and `B` respectively. It might be beneficial to have both `A` and `B` as individual
commits after merging them into `development`, instead of squashing all of them into a single commit. However, a simple
rebase of the PR would include `A`, `B`, `C`, and `D` in `development`. This reduces the readability of the `development`
branch's history.

A possibility would be to makes the necessary changes, stage them, and then do `git commit --amend` to amend `B`. We can
then force push the changes. However, this is not ideal for two reasons: `B` now has changes that were supposed to fix
`A`, and we force pushed.

Ideally, we would like to squash `C` and `D` into `A` and `B` respectively. This can be accomplished by doing an
interactive rebase using `git rebase -i HEAD~4`, reordering the commits in the todo list, and then squashing them
together. However, this process is tedious.

Git allows us to automatically squash or fixup such commits. Git does this by editing the todo list for us when we
perform an interactive rebase. Assume we want to automatically squash `C` into `A`, and `D` into `B`. This can be
accomplished as follows:

1. Stage the changes to be committed. These changes will form commit `C`.
2. Perform `git commit --fixup ...`. Here `...` is either the commit hash, or commit subject of `A`. This will create
   commit `C` with the subject as `!fixup ...`. This annotation tells Git which commit `C` will _fixup_.
3. Repeat the above steps to create `D`.
4. Perform `git rebase -i --autosquash HEAD~4`. Use the annotations in the commit subjects, Git will generate a todo list
   such that `C` and `D` will fixup `A` and `B` respectively.
5. Continue with the rebase and fix any merge problems you encounter.

Note that `fixup` can be replaced with `squash` in the above steps as well. However, the option `--autosquash` for
`git rebase` will remain the same for both cases.

For the sake of completeness, the difference between fixup and squash is as follows:

 > When `C` fixes up `A`, the changes of `C` is applied to `A` and the commit message of `C` is discarded and only that of
 > `A` is used in the new commit.

 > When `C` is squashed with `A`, the changes of `C` is applied to `A` and the user is prompted to provide a new commit
 > message for the squashed commit.

For further reading, [here][auto-squash-doc] is the documentation for `--autosquash`.

[auto-squash-doc]: https://git-scm.com/docs/git-rebase#Documentation/git-rebase.txt---autosquash


## Maintainers' Guide to Versioning

Changelogs are necessary so that the end user knows what changes were brought in each new release. This project maintains
changelogs using the [Keep a Changelog][keep-a-changelog] guidelines.

Version numbers are important in helping to differentiate releases of a product. This project follows
[Semantic Versioning][sem-ver] to keep track of versions.

Android uses a unique integer for each release called the Version Code. To ensure proper version code sequences and
correlation with Semantic  Versioning, the following steps are used to arrive at the version code:

1. The Major version shall be multiplied by 1,000,000.

2. The Minor version shall be multiplied by 10,000.

3. The Patch version shall be multiplied by 100.

4. The above values shall be added together to get the version code a release.

5. Pre-releases, alpha releases etc should be signified by appropriate values in the least two significant digits.

6. The [changelog](changelog) should state the semantic version number as well as version code (if a Playstore
   rollout has been done) with each release


[keep-a-changelog]: https://keepachangelog.com/en/1.0.0/
[sem-ver]: https://semver.org/

## Maintainers' Release Checklist

There is no release timeline for this project. Once the maintainer(s) decide that sufficient changes have been made and
any and all release blocking bugs have been quashed, a maintainer (identified as the release manager henceforth) will
take  charge of creating a release. The following checklist is to  be followed, and all work is to be done on
`development` unless otherwise mentioned:


1. Ensure that all relevant changes have been noted in the [changelog](changelog).

2. Group the **Unreleased** changelogs under a new version number.

3. Update the `versionName` as well as `versionCode` in the app's [`build.gradle`](app/build.gradle) file.

4. Ensure the above change does not result in the the debug and release build to fail.

5. Create a commit with the aforementioned changes. Tag this commit with the version number using `git tag`.
   Fast-forward `master` to the latest `development`.

6.  Push both `master` and `development` directly to the repo. While doing so, the release manager should exercise
    utmost caution.  Use a PR if you are unsure and are doing this process for the first time. Make sure you push
    the tag that you just created as well using `git push --tags <name-of-remote>`.

7. Draft any notes or posts that are to be put on any forum to notify users about the release.

8. Compile a signed release apk using the apps signing key.

9. Head over to the Google Play Store console and create a new release using the release apk.

10. Add stub changelog notes for the next series of unreleased changes. Once again, create a commit. However, this time,
    push only to `development`.

**Note Regarding `master`**: `master` should always point to the latest release commit i.e a commit that updates the
version number in the changelog and  any other files. The commit that master points to should also be tagged with the
version number of that release.

[//]: # (Global links i.e links that appear in more than one section)
[changelog]: CHANGELOG.md
[code-of-conduct]: code-of-conduct.md
[license]: LICENSE

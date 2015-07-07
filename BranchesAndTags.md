
# Introduction #

Most development happens on the main trunk, and the [code checkout instructions](http://code.google.com/p/flightmap/source/checkout) tell you how to check out that code.

This page will tell you how to switch between the `trunk` and other branches. It also tells you how to work with tags.

# One Working Directory #

Once you've done the EclipseSetup, you don't want to repeat that for each branch you work on. The recommended way of development is to have one working directory for all your development, and use the `svn switch` command to switch between them. That way you can keep Eclipse pointed at the one working directory.

These instructions will assume your working directory is `~/src/flightmap`. Adjust appropriately if you've chosen a different working directory.

# Branches #

We create branches for each release. These are typically short-lived, just for release-related details that don't belong on the main trunk (such as setting the version number, or making a quick release-specific fix).

# Tags #
Tags are made for each release. These are kept forever.

# Version Naming Scheme #
We use the common 3-group decimal scheme to name versions. This is coresponds to the `versionName` in the AndroidManifest.xml file.

The format is `major.minor.revision`.
  * The major number is incremented by 1 for each big jump in functionality.
    * Prior to public release, the major number will be 0.
  * The minor number changes with each significant release of the code.
    * Odd minor version numbers (`0.5`, `0.5.1`, `0.7.0`, `0.11.2`, ...) represent experimental releases, even numbers represent public releases.
  * The revision number starts at 0 and increments by 1 for each small bug fix release.
    * The revision may also be a text string such as `trunk` to indicate an un-numbered top-of-trunk build.

# Naming Scheme for release Branches and Tags #
The integer `versionCode` in android/AndroidManifest.xml is an important number. It must be incremented for the Android system to detect your code has been updated. The `versionName` in that file is what we typically talk about (version 0.5.1, 1.0.0, etc.). It's shown to the user, but has no significance to the updating system.

Given how important the `versionCode` is, our naming scheme for release branches and tags emphasizes that by putting the `versionCode` first.

```
<4-digit-versionCode>-release-<versionName>
```
Example:
The release named "0.5" had `versionCode` 5, and the release named "0.5.1" had `versionCode` 6. Here's the branch/tag names for those releases
```
0005-release-0.5
0006-release-0.5.1
```

# How To... #

All the examples below use `https` which provides read-write access to subversion. If you're not on the commiters list, and therefore only have read-only access, change the `https` to `http`.

## Check out the main trunk ##

See the [code checkout instructions](http://code.google.com/p/flightmap/source/checkout).

## Switch your working directory to trunk ##
```
cd ~/src/flightmap
svn switch https://flightmap.googlecode.com/svn/trunk/
```

## Switch your working directory to a branch ##
```
cd ~/src/flightmap
svn switch https://flightmap.googlecode.com/svn/branches/the-branch-name
```

## Determine if your working directory points to trunk or a branch ##
```
cd ~/src/flightmap
svn info | grep URL:
```

## Create a release branch ##
```
svn cp -r333 https://flightmap.googlecode.com/svn/trunk https://flightmap.googlecode.com/svn/branches/0007-release-0.7.0
```

## Merge a change from trunk to a branch ##
Typically this done to cherry pick one specific revision's change down to a release branch.

  1. Ensure you have no pending changes. `svn status` reports nothing pending.
  1. **switch your working dir to the branch** as described above.
  1. Now let's say [r42424242](https://code.google.com/p/flightmap/source/detail?r=42424242) was a change on the trunk that you want to merge to the branch. Do the following:
```
svn merge -c42424242 https://flightmap.googlecode.com/svn/trunk
```

Now the changes from [r42424242](https://code.google.com/p/flightmap/source/detail?r=42424242) are pending changes in your working directory. You can edit them as needed. When you're ready to commit them to the branch, then submit with a comment that identifies the source of the change. For example, [r258](https://code.google.com/p/flightmap/source/detail?r=258) was a merge from trunk to a release branch. Here's the command used to submit it.
```
svn ci -m"Merged fix for issue 62 to release branch (from trunk r257)"
```

## Merge a change from a branch to the trunk ##
**This is not a normal operation for a release branch.** For a release branch, it's much better to make the change on the trunk then merge it down.

The steps are basically the same as merging the other way. Substitute as needed in the merge trunk to branch instructions above.

## Tag a release ##
This is normally done by copying a release branch to the tags directory at a specific revision.

Example:
```
svn cp -r268 https://flightmap.googlecode.com/svn/branches/0123-release-0.0123 https://flightmap.googlecode.com/svn/tags/0123-release-0.0123
```
# Changelog

## 2.000:
    https://imageware.atlassian.net/browse/MOBK-399 - Adapted sample app to exercise new SDK interface which now matches iOS

## 1.12:
    https://imageware.atlassian.net/browse/MOBK-370 - GMI SDK updated which now returns null IMSPerson object instead of IMSServerException when getPerson 404s
    Updated GMI SDK to 8.0.1

## 1.11:
    https://imageware.atlassian.net/browse/MOBK-357 - Adapted Github Actions workflow files to no longer rely on the deprecated / disabled "set-env" command

## 1.10:
    https://imageware.atlassian.net/browse/MOBK-368 - New GMI SDK fixes nested device initialization issue as well as yesno biometric crash
    Updated GMI SDK to 7.8.10

## 1.9:
    https://imageware.atlassian.net/browse/MOBK-365 - New GMI SDK fixes nested template capture sequence
    https://imageware.atlassian.net/browse/MOBK-366 - New GMI SDK fixes nested device template crash
    Updated GMI SDK to 7.8.9

## 1.8:
    https://imageware.atlassian.net/browse/MOBK-362 - Updated GMI SDK which added missing headers to enroll and verify calls to enable NA1 / production operation (these headers are apparently not required on eks-dev)
    Updated GMI SDK to 7.8.8

## 1.7:
    https://imageware.atlassian.net/browse/MOBK-219 - Updated GMI SDK and its required dependencies
    Updated GMI SDK to 7.8.7
    Updated: Kotlin compiler, Android Gradle Plugin, Gradle Build System, Android OS Build Target to Android 11

## 1.6:
    https://imageware.atlassian.net/browse/MOBK-326 - Updated SDK libraries to IWS AWS S3 repo dependencies

## 1.5:
    No code changes, redeploy

## 1.4:
    Updated SDK library read token

## 1.3:
    https://imageware.atlassian.net/browse/MOBK-27 - Updated SDK libraries to Github Package dependencies

## 1.2:
    https://imageware.atlassian.net/browse/MOBK-26 - Migrated CICD build from Jenkins to Github Actions (no code changes)

## 1.1:
    Updated GMI SDK to 7.7.1

## 1.0:
    Initial release, using GMI SDK 7.6.18
    
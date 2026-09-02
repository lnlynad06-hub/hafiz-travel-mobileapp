# Fix Java Compilation Errors in GuidelineActivity

The project fails to build because `GuidelineActivity.java` has the wrong package name, causing it to fail to find `R` and `LocaleHelper`. This prevents the Layout Editor from rendering `activity_guideline.xml`.

## Proposed Changes

### app

#### [MODIFY] [GuidelineActivity.java](file:///C:/Users/User/StudioProjects/hafiz-travel-mobileapp/app/src/main/java/com/example/app/GuidelineActivity.java)
- Correct the package name from `com.example.hafiztraveltours` to `com.hafiztraveltours.app`.

## Verification Plan

### Automated Tests
- Run `gradlew app:assembleDebug` to ensure the project compiles successfully.

### Manual Verification
- Verify that the render issue in the Layout Editor is resolved (though I can't directly see the Layout Editor, a successful build is the prerequisite).

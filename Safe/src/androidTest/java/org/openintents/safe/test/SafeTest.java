/*
 * This allows for the testing of OI Safe.   Both Safe and SafeTest should
 * be imported.
 * 
 * It is assumed that a master password has already been set.   Update the
 * variable below accordingly.
 * 
 * The test also assumes English is being used.
 * 
 * On the SafeTest project, select Run As --> Run As Android JUnit Test
 * 
 * @author Randy McEoin
 * 
 */

package org.openintents.safe.test;

import android.content.pm.ActivityInfo;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.Smoke;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.safe.CategoryEdit;
import org.openintents.safe.CategoryList;
import org.openintents.safe.PassEdit;
import org.openintents.safe.PassList;
import org.openintents.safe.PassView;
import org.openintents.safe.R;
import org.openintents.safe.Search;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.startsWith;

@RunWith(AndroidJUnit4.class)
public class SafeTest {


    @Rule
    private ActivityTestRule rule = new ActivityTestRule(CategoryList.class);

    private final String TAG = "SafeTest";
    private final String masterPassword = "1234";

    private void unlockIfNeeded() throws Exception {

        try {
            onView(withText(R.string.first_time)).check(matches(isDisplayed()));
            //view is displayed logic

            onView(withId(R.id.password)).perform(typeText(masterPassword));
            onView(withId(R.id.pass_confirm)).perform(typeText(masterPassword));
            onView(withId(R.id.continue_button)).perform(click());
            onView(withText(android.R.string.ok)).perform(click());
        } catch (NoMatchingViewException e) {
            //view not displayed logic
        }

        try {
            onView(withText(R.string.continue_text)).check(matches(isDisplayed()));
            //view is displayed logic
            onView(withId(R.id.password)).perform(typeText(masterPassword));
            onView(withText(R.string.continue_text)).perform(click());

        } catch (NoMatchingViewException e) {
            //view not displayed logic
        }
    }

    private String getAppString(int resId) {
        return rule.getActivity().getString(resId);
    }

    @Test
    @Smoke
    public void test000Eula() {
        String accept = getAppString(org.openintents.distribution.R.string.oi_distribution_eula_accept);
        String cancel = getAppString(org.openintents.distribution.R.string.oi_distribution_eula_refuse);

        try {
            onView(withText(accept)).check(matches(isDisplayed()));
            onView(withText(cancel)).check(matches(isDisplayed()));

            onView(withText(accept)).perform(click());
        } catch (NoMatchingViewException e) {
            //view not displayed logic
        }
    }

    @Test
    @Smoke
    public void test001RecentChanges() {
        String recentChanges = getAppString(org.openintents.distribution.R.string.oi_distribution_newversion_recent_changes);
        String cont = getAppString(org.openintents.distribution.R.string.oi_distribution_newversion_continue);

        try {
            onView(withText(recentChanges)).check(matches(isDisplayed()));
            onView(withText(cont)).check(matches(isDisplayed()));

            onView(withText(cont)).perform(click());
        } catch (NoMatchingViewException e) {
            //view not displayed logic
        }
    }

    @Test
    @Smoke
    public void testAAAAUnlock() throws Exception {
        unlockIfNeeded();

        onView(withId(android.R.id.list)).check(matches(isDisplayed()));
    }

    @Test
    @Smoke
    public void testCategoryAdd() throws Exception {
//		unlockIfNeeded();

        openActionBarOverflowOrOptionsMenu(rule.getActivity());
        onView(withText(R.string.password_add)).perform(click());

        intended(hasComponent(CategoryEdit.class.getName()));

        onView(withId(R.id.name)).perform(typeText("Category 1"));
        onView(withId(R.id.save_category)).perform(click());

        openActionBarOverflowOrOptionsMenu(rule.getActivity());
        onView(withText(R.string.password_add)).perform(click());

        intended(hasComponent(CategoryEdit.class.getName()));

        onView(withId(R.id.name)).perform(typeText("Category 2"));
        onView(withId(R.id.save_category)).perform(click());

        onView(withText("Category 1")).check(matches(isDisplayed()));
        onView(withText("Category 2")).check(matches(isDisplayed()));
    }

    @Test
    @Smoke
    public void testCategoryEdit() throws Exception {
//		unlockIfNeeded();
        onView(withText("Category 1")).perform(longClick());
        onView(withText("Edit")).perform(click());
        onView(withId(R.id.name)).perform(typeText("test"));
        onView(withId(R.id.save_category)).perform(click());

        onView(withText("Category 1 test")).check(matches(isDisplayed()));
    }

    @Test
    @Smoke
    public void test_CategoryRemove() throws Exception {
//		unlockIfNeeded();
        onView(withText(startsWith("Category 1"))).perform(longClick());
        onView(withText(R.string.password_delete)).perform(click());

        onView(withText(startsWith("Category 1"))).check(doesNotExist());


        onView(withText(startsWith("Category 2"))).perform(longClick());
        onView(withText(R.string.password_delete)).perform(click());

        onView(withText(startsWith("Category 2"))).check(doesNotExist());
    }

    @Test
    @Smoke
    public void testPasswordAdd() throws Exception {
//		unlockIfNeeded();

        openActionBarOverflowOrOptionsMenu(rule.getActivity());
        onView(withText(R.string.password_add)).perform(click());

        intended(hasComponent(CategoryEdit.class.getName()));

        onView(withId(R.id.name)).perform(typeText("Category for Passwords"));
        onView(withText(R.string.save)).perform(click());

        onView(withText("Category for Passwords")).check(matches(isDisplayed()));

        onView(withText("Category for Passwords")).perform(click());

        intended(hasComponent(PassList.class.getName()));

        for (int i = 1; i < 4; i++) {
            openActionBarOverflowOrOptionsMenu(rule.getActivity());
            onView(withText(R.string.password_add)).perform(click());

            intended(hasComponent(CategoryEdit.class.getName()));

            String entry = "ptest" + i;
            String entryDescription = entry + " description";
            onView(withId(R.id.description)).perform(typeText(entryDescription));
            onView(withId(R.id.website)).perform(typeText("http://www.google.com/"));
            onView(withId(R.id.username)).perform(typeText(entry + " user"));
            onView(withId(R.id.password)).perform(typeText(entry + " password"));
            onView(withId(R.id.note)).perform(typeText(entry + " note"));

            pressBack();

            intended(hasComponent(PassList.class.getName()));

            onView(withText(entryDescription)).check(matches(isDisplayed()));
        }
    }

    @Test
    @Smoke
    public void testPasswordEdit() throws Exception {
        onView(withText("Category for Passwords")).perform(click());

        intended(hasComponent(PassList.class.getName()));

        onView(withText("ptest1")).perform(longClick());
        onView(withText(R.string.password_edit)).perform(click());

        intended(hasComponent(PassEdit.class.getName()));

        onView(withId(R.id.name)).perform(typeText(" modified"));

        pressBack();

        intended(hasComponent(PassList.class.getName()));

        onView(withText("ptest1 description modified")).check(matches(isDisplayed()));

        onView(withText("ptest2")).perform(click());
        intended(hasComponent(PassView.class.getName()));

        openActionBarOverflowOrOptionsMenu(rule.getActivity());
        onView(withText(R.string.password_edit)).perform(click());
        onView(withText(R.string.password_edit)).perform(click());

        intended(hasComponent(PassEdit.class.getName()));

        onView(withId(R.id.name)).perform(typeText(" modified"));
        pressBack();
        pressBack();
        intended(hasComponent(PassList.class.getName()));

        onView(withText("ptest2 description modified")).check(matches(isDisplayed()));
    }

    @Test
    @Smoke
    public void testSearch() throws Exception {
        //	unlockIfNeeded();
        openActionBarOverflowOrOptionsMenu(rule.getActivity());
        onView(withText(R.string.search));

        onView(withId(R.id.search_criteria)).perform(typeText("ptest3"));
        onView(withId(R.id.button1)).perform(click());
        intended(hasComponent(Search.class.getName()));

        onData(withId(android.R.id.list)).atPosition(0).perform(click());
        intended(hasComponent(PassView.class.getName()));

        onView(withText("ptest3 description")).check(matches(isDisplayed()));

        pressBack();

        intended(hasComponent(Search.class.getName()));

        rule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        onData(withId(android.R.id.list)).atPosition(0).perform(click());
        intended(hasComponent(PassView.class.getName()));

        onView(withText("ptest3 description")).check(matches(isDisplayed()));

        rule.getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Remove all passwords present in test category
     *
     * @throws Exception
     */
    @Test
    @Smoke
    public void test_PasswordRemove() throws Exception {
        onView(withText("Category for Passwords")).perform(click());

        intended(hasComponent(PassList.class.getName()));

        for (int i = 1; i < 4; i++) {
            onView(withText(startsWith("ptest"))).perform(longClick());
            onView(withText(R.string.password_delete)).perform(click());
            onView(withText(R.string.yes)).perform(click());
        }

        onView(withText(R.string.empty_safe)).check(matches(isDisplayed()));
    }
}

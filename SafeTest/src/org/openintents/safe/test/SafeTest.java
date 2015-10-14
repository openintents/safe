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

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.Smoke;
import android.util.Log;

import com.jayway.android.robotium.solo.Solo;

import org.openintents.safe.CategoryList;
import org.openintents.safe.R;

public class SafeTest extends ActivityInstrumentationTestCase2<CategoryList> {

    private final String TAG = "SafeTest";
    private final String masterPassword = "1234";

    private Solo solo;

    public SafeTest() {
        super("org.openintents.safe", CategoryList.class);
    }

    public void setUp() throws Exception {
        solo = new Solo(getInstrumentation(), getActivity());
    }

    private void unlockIfNeeded() throws Exception {

//		String confirm = getActivity().getString(R.string.oi_distribution_eula_accept);
//		if (solo.searchButton(confirm)){
//			solo.clickOnButton(confirm);
//		}
//		
//		String cont = getActivity().getString(R.string.oi_distribution_newversion_continue);
//		if (solo.searchButton(cont)){
//			solo.clickOnButton(cont);
//		}

        String continueText = getActivity().getString(R.string.continue_text);
        String restore = getActivity().getString(R.string.restore);
        String firstTime = getActivity().getString(R.string.first_time);
        if (solo.searchText(firstTime)) {
            Log.d(TAG, "Creating new master password");
            solo.enterText(0, masterPassword);
            solo.enterText(1, masterPassword);
            solo.clickOnButton(continueText);

            solo.clickOnButton(getActivity().getString(android.R.string.ok));

        }

        if (solo.searchButton(continueText)) {
            Log.d(TAG, "unlocking");
            solo.enterText(0, masterPassword);
            solo.clickOnButton(continueText);
        }

    }

    private String getAppString(int resId) {
        return getActivity().getString(resId);
    }

    @Smoke
    public void test000Eula() {
        String accept = getAppString(org.openintents.distribution.R.string.oi_distribution_eula_accept);
        String cancel = getAppString(org.openintents.distribution.R.string.oi_distribution_eula_refuse);
        boolean existsAccept = solo.searchButton(accept);
        boolean existsCancel = solo.searchButton(cancel);

        if (existsAccept && existsCancel) {
            solo.clickOnButton(accept);
        }
    }

    @Smoke
    public void test001RecentChanges() {
        String recentChanges = getAppString(org.openintents.distribution.R.string.oi_distribution_newversion_recent_changes);
        String cont = getAppString(org.openintents.distribution.R.string.oi_distribution_newversion_continue);
        while (solo.scrollUp()) {
            ;
        }
        boolean existsRecentChanges = solo.searchText(recentChanges);
        boolean existsCont = solo.searchButton(cont);

        if (existsRecentChanges && existsCont) {
            solo.clickOnButton(cont);
        }
    }

    @Smoke
    public void testAAAAUnlock() throws Exception {
        unlockIfNeeded();

        // This test fails in Honeycomb:
        //solo.assertCurrentActivity("Expected CategoryList", CategoryList.class);
    }

    @Smoke
    public void testCategoryAdd() throws Exception {
//		unlockIfNeeded();

        solo.assertCurrentActivity("Expected CategoryList activity", "CategoryList");

        solo.clickOnMenuItem("Add");
        solo.assertCurrentActivity("Expected CategoryEdit activity", "CategoryEdit");
        solo.enterText(0, "Category 1");
        solo.clickOnButton(0);

        solo.clickOnMenuItem("Add");
        solo.assertCurrentActivity("Expected CategoryEdit activity", "CategoryEdit");
        solo.enterText(0, "Category 2");
        solo.clickOnButton(0);

        boolean expected = true;
        boolean actual = solo.searchText("Category 1") && solo.searchText("Category 2");
        assertEquals("Category 1 and/or Category 2 are not found", expected, actual);
    }

    @Smoke
    public void testCategoryEdit() throws Exception {
//		unlockIfNeeded();
        solo.clickLongOnText("Category 1");

//		solo.setActivityOrientation(Solo.LANDSCAPE); // Change orientation of activity
        solo.clickOnText("Edit"); // Change title
        solo.enterText(0, " test"); //In first text field (0), add test.
        solo.clickOnButton(0);

        boolean expected = true;
        boolean actual = solo.searchText("Category 1 test"); // (Regexp) case insensitive
        assertEquals("Note 1 test is not found", expected, actual);
    }

    @Smoke
    public void test_CategoryRemove() throws Exception {
//		unlockIfNeeded();
        solo.clickLongOnText("Category 1.*");
        solo.clickOnText("Delete");
        boolean expected = false;
        boolean actual = solo.searchText("Category 1 test");
        assertEquals("Category 1 Test is found", expected, actual);
        solo.clickLongOnText("Category 2");
        solo.clickOnText("Delete");
        actual = solo.searchText("Category 2");
        assertEquals("Category 2 is found", expected, actual);
    }

    @Smoke
    public void testPasswordAdd() throws Exception {
//		unlockIfNeeded();

        solo.clickOnMenuItem("Add");
        solo.assertCurrentActivity("Expected CategoryEdit activity", "CategoryEdit");
        solo.enterText(0, "Category for Passwords");
        solo.clickOnButton(0);

        boolean expected = true;
        boolean actual = solo.searchText("Category for Passwords");
        assertEquals("Category for Passwords are not found", expected, actual);

        solo.clickOnText("Category for Passwords");
        solo.assertCurrentActivity("Expected PassList activity", "PassList");

        for (int i = 1; i < 4; i++) {
            solo.clickOnMenuItem("Add");
            solo.assertCurrentActivity("Expected PassEdit activity", "PassEdit");

            String entry = "ptest" + i;
            String entryDescription = entry + " description";
            solo.enterText(0, entryDescription);
            solo.enterText(1, "http://www.google.com/");
            solo.enterText(2, entry + " user");
            solo.enterText(3, entry + " password");
            solo.enterText(4, entry + " note");
            solo.goBack();
            solo.assertCurrentActivity("Expected PassList activity", "PassList");

            expected = true;
            actual = solo.searchText(entryDescription);
            assertEquals(entryDescription + "is not found", expected, actual);
        }
    }

    @Smoke
    public void testPasswordEdit() throws Exception {
        solo.clickOnText("Category for Passwords");
        solo.assertCurrentActivity("Expected PassList activity", "PassList");

        solo.clickLongOnText("ptest1");
        solo.clickOnText("Edit");
        solo.assertCurrentActivity("Expected PassEdit activity", "PassEdit");

        solo.enterText(0, " modified");
        solo.goBack();
        solo.assertCurrentActivity("Expected PassList activity", "PassList");

        boolean expected = true;
        boolean actual = solo.searchText("ptest1 description modified");
        assertEquals("edited password not found", expected, actual);

//		solo.clickLongOnText("ptest2");
        solo.clickInList(2);
        solo.assertCurrentActivity("Expected PassView activity", "PassView");

        solo.clickOnMenuItem("Edit");
        solo.clickOnText("Edit");
        solo.assertCurrentActivity("Expected PassEdit activity", "PassEdit");

        solo.enterText(0, " modified");
        solo.goBack();
        solo.goBack();
        solo.assertCurrentActivity("Expected PassList activity", "PassList");

        expected = true;
        actual = solo.searchText("ptest2 description modified");
        assertEquals("edited password2 not found", expected, actual);

    }

    @Smoke
    public void testSearch() throws Exception {
        //	unlockIfNeeded();
        solo.clickOnMenuItem("Search");

        solo.enterText(0, "ptest3");
        solo.clickOnButton("Search");
        solo.assertCurrentActivity("Expected Search activity", "Search");

        solo.clickInList(1);
        solo.assertCurrentActivity("Expected PassView activity", "PassView");

        boolean expected = true;
        boolean actual = solo.searchText("ptest3 description");
        assertEquals("description not found", expected, actual);

        solo.goBack();
        solo.assertCurrentActivity("Expected Search activity", "Search");

        solo.setActivityOrientation(Solo.LANDSCAPE);
        solo.clickInList(1);
        solo.assertCurrentActivity("Expected PassView activity", "PassView");

        expected = true;
        actual = solo.searchText("ptest3 description");
        assertEquals("description not found", expected, actual);

        solo.setActivityOrientation(Solo.PORTRAIT);
    }

    /**
     * Remove all passwords present in test category
     *
     * @throws Exception
     */
    @Smoke
    public void test_PasswordRemove() throws Exception {
        solo.clickOnText("Category for Passwords");
        solo.assertCurrentActivity("Expected PassList activity", "PassList");

        while (solo.searchText("ptest")) {
            solo.clickLongOnTextAndPress("ptest", 2);
            solo.clickOnButton("Yes");
        }
        boolean expected = true;
        boolean actual = solo.searchText("No Passwords");
        assertEquals("Passwords still found", expected, actual);
    }

    @Override
    public void tearDown() throws Exception {
        try {
            solo.finishOpenedActivities();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        getActivity().finish();
        super.tearDown();
    }
}

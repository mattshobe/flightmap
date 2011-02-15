package com.google.flightmap.android.test;

import com.google.flightmap.android.SearchActivity;

import android.database.sqlite.SQLiteDatabase;
import android.test.ActivityInstrumentationTestCase2;

public class SearchActivityTest extends ActivityInstrumentationTestCase2<SearchActivity>{

  private SearchActivity mActivity;
  
  public SearchActivityTest() {
    super("com.google.flightmap.android", SearchActivity.class);
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    setActivityInitialTouchMode(false);

    mActivity = getActivity();

  } // end of setUp() method definition  
  
  public void testQuery() {
    mActivity.runOnUiThread(
        new Runnable() {
          public void run() {
            //TODO: Should compare this against the db results directly.
            assertEquals(sendQuery("SFO"), 7);
            assertEquals(sendQuery("RHV"), 1);
            assertEquals(sendQuery("xyzzy"), 0);
          }
        }
      );
  }
  
  public int sendQuery(String query) {
    sendKeys(query);
    return mActivity.searchResults.size();
  }
}

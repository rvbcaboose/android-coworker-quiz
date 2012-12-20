package com.appirio.android.samples;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;
import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Gravity;

import android.widget.Toast;
import android.widget.TextView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TableRow.LayoutParams;

import android.content.Intent;

import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.rest.ClientManager.RestClientCallback;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

/**
 * Leaderboard activity
 *
 * @author appirio-appdev
 */
public class LeaderboardActivity extends Activity {
    // Logging tag
    private static final String TAG = "LeaderboardActivity";

    private RestClient oClient = null;
    private String     sApiVersion;
    private boolean    bDrawn = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "::onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.leaderboard);
        sApiVersion = getString(R.string.api_version);
    }

	@Override
	public void onResume() {
        // The activity has become visible (it is now "resumed").
        Log.d(TAG, "::onResume");
		super.onResume();

        // Login options
        String accountType = getString(R.string.account_type);
        LoginOptions loginOptions = new LoginOptions(
                                            null, // gets overridden by LoginActivity based on server picked by uuser 
                                            ForceApp.APP.getPasscodeHash(),
                                            getString(R.string.oauth_callback_url),
                                            getString(R.string.oauth_client_id),
                                            new String[] {"api"});
        
        new ClientManager(this, accountType, loginOptions).getRestClient(this, new RestClientCallback() {
                @Override
                public void authenticatedRestClient(RestClient client) {
                    if (client == null) {
                        ForceApp.APP.logout(LeaderboardActivity.this);
                        return;
                    }

                    // reset leaderboard to empty if its already been drawn and
                    // activity has not been re-created 
                    // (ie: pause, stop, start, resume AND NOT 
                    //      pause, stop, destroy, create, start, resume)
                    if(bDrawn) {
                        setContentView(R.layout.leaderboard);
                    }
                        
                    LeaderboardActivity.this.oClient = client;
                    fetchAndDrawLeaderboard();
                    bDrawn = true;
                }
            });
	}

    @Override
    protected void onStart() {
        Log.d(TAG, "::onStart");
        super.onStart();
        // The activity is about to become visible.
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "::onPause");
        super.onPause();
        // Another activity is taking focus (this activity is about to be "paused").
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "::onStop");
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "::onDestroy");
        bDrawn = false;
        super.onDestroy();
        // The activity is about to be destroyed.
    }

	@Override
	public void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putBoolean("leaderboard_drawn", bDrawn);
    }

    @Override
    public void onRestoreInstanceState(Bundle state) {
        super.onRestoreInstanceState(state);
        bDrawn = state.getBoolean("leaderboard_drawn");
    }
    
    private void fetchAndDrawLeaderboard() {
        /*
          Make SF REST call to fetch leaderboard.

        return new Score[] {
            new Score("Lori Williams", 110, 1),
            new Score("Svatka Simpson", 76, 0),
            new Score("Mike Epner", 74, 1)
        };
        */

        try {
            String soql = "select User_Id__c, Name, Low_Score__c, High_Score__c from CQ_LeaderBoard__c order by High_Score__c desc";
            RestRequest request = RestRequest.getRequestForQuery(sApiVersion, soql);
            
            oClient.sendAsync(request, new AsyncRequestCallback() {
                    @Override
                    public void onSuccess(RestResponse response) {
                        Score[] scores = null;
                        try {
                            if(null != response && null != response.asJSONObject()) {
                                JSONArray records = response.asJSONObject().getJSONArray("records");
                                if (records.length() > 0) {
                                    scores = new Score[records.length()];
                                    for(int i=0; i<records.length(); i++) {
                                        JSONObject record = (JSONObject)records.get(i);
                                        scores[i] = new Score(record.getString("Name"),
                                                              record.getInt("High_Score__c"),
                                                              record.getInt("Low_Score__c"));
                                    }
                                    drawLeaderboard(scores);
                                }
                            }
                        }
                        catch(Exception e) {
                            e.printStackTrace();
                            displayError(e.getMessage());
                        }
                    }
                    
                    @Override
                    public void onError(Exception exp) {
                        exp.printStackTrace();
                        displayError(exp.getMessage());
                    }
                });
        }
        catch(Exception e) {
            e.printStackTrace();
            displayError(e.getMessage());
        }
    }

    /**
     * Draws leaderboard-data. Expects a non-null array of scores.
     */
    private void drawLeaderboard(Score[] scores) {
        TableLayout tlayout = (TableLayout)findViewById(R.id.table);
        for(Score score : scores) {
            TableRow row = new TableRow(this);
            row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            TextView cell1 = new TextView(this);
            cell1.setText(score.getUserName(), TextView.BufferType.NORMAL);
            cell1.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            cell1.setPadding(3,3,3,3);
            cell1.setGravity(Gravity.LEFT);
            row.addView(cell1);

            TextView cell2 = new TextView(this);
            cell2.setText("" + score.getHighScore(), TextView.BufferType.NORMAL);
            cell2.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            cell2.setPadding(3,3,3,3);
            cell2.setGravity(Gravity.RIGHT);
            row.addView(cell2);

            TextView cell3 = new TextView(this);
            cell3.setText("" + score.getLowScore(), TextView.BufferType.NORMAL);
            cell3.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            cell3.setPadding(3,3,3,3);
            cell3.setGravity(Gravity.RIGHT);
            row.addView(cell3);
            tlayout.addView(row, new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }

        EventsObservable.get().notifyEvent(EventType.RenditionComplete);
    }

    private void displayError(String msg) {
        Toast.makeText(getApplicationContext(), "Error: " + msg, Toast.LENGTH_LONG).show();
    }

    //==============
    // inner classes

    private static class Score {
        private int lowScore = 0;
        private int highScore = 0;
        private String userName = null;

        private Score(String user, int high, int low) {
            userName = user;
            lowScore = low;
            highScore = high;
        }
        
        public String getUserName()  { return userName; }
        public int    getLowScore()  { return lowScore; }
        public int    getHighScore() { return highScore; }
    }
}

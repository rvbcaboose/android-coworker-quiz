package com.appirio.android.samples;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.appirio.android.samples.util.ImageDownloader;
import com.salesforce.androidsdk.app.ForceApp;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.rest.ClientManager.LoginOptions;
import com.salesforce.androidsdk.rest.ClientManager.RestClientCallback;
import com.salesforce.androidsdk.rest.RestClient;
import com.salesforce.androidsdk.rest.RestClient.AsyncRequestCallback;
import com.salesforce.androidsdk.rest.RestRequest;
import com.salesforce.androidsdk.rest.RestRequest.RestMethod;
import com.salesforce.androidsdk.rest.RestResponse;
import com.salesforce.androidsdk.util.EventsObservable;
import com.salesforce.androidsdk.util.EventsObservable.EventType;

/**
 * Co-worker Quiz activity.
 *
 * @author appirio-appdev
 */
public class CoWorkerQuizActivity extends Activity {
    private static final int DIALOG_COWORKER_LIST = 1;
    private static final int DIALOG_COWORKER_CONF = 2;

    // Logging tag
    private static final String TAG = "CoWorkerQuizActivity";

    private ImageDownloader oImageDownloader;
    private MediaPlayer oMediaPlayer;
    private RestClient oClient;
    private String sApiVersion;
    private boolean bFirstTime;

    // all co-workers (only fetched once when the activity is created);
    // then as and when a choice is shown to user, its removed from this list
    private List<CoWorkerPerson> oAllCoWorkers;

    // current user selection
    private CharSequence sCurrentChoice;

    // current set of choices for user to make
    private List<CharSequence> oCurrentChoices;

    // current co-worker whose picture is being shown
    private CoWorkerPerson oCurrentCoWorker;

    // Activity state data (preserved if process is killed because of low-memory)
    private int nNumConsecutiveRights;
    private int nNumConsecutiveWrongs;
    private int nMaxHigh;
    private int nMaxLow;

    //============================
    // Activity lifecycle methods

    /**
     * Called when the activity is first created. 
     * Most initialization should go here.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "::onCreate");
        super.onCreate(savedInstanceState);

        // init
        bFirstTime = true;
        oImageDownloader = new ImageDownloader();
        oCurrentChoices = new ArrayList<CharSequence>();
        nNumConsecutiveRights = 0;
        nNumConsecutiveWrongs = 0;
        nMaxHigh = 0;
        nMaxLow  = 0;

        // setup view
        setContentView(R.layout.main);
        sApiVersion = getString(R.string.api_version);
    }

    /**
     * Called when activity has become visible and in the
     * foreground (it is now "resumed")
     */
    @Override
    public void onResume() {

        Log.d(TAG, "::onResume");
        super.onResume();

        // Hide everything until we are logged in
        findViewById(R.id.root).setVisibility(View.INVISIBLE);

        /*
         * Un-comment this block to have the passcode screen

        // Bring up passcode screen if needed
        ForceApp.APP.getPasscodeManager().lockIfNeeded(this, true);

        // Do nothing - when the app gets unlocked we will be back here
        if (ForceApp.APP.getPasscodeManager().isLocked()) {
            return;
        }

         */

        // Login options
        // TODO: Do we need to do this on every resume?
        String accountType = ForceApp.APP.getAccountType();
        LoginOptions loginOptions = new LoginOptions(
             null, // login host is chosen by user through the server picker
             ForceApp.APP.getPasscodeHash(),
             getString(R.string.oauth_callback_url),
             getString(R.string.oauth_client_id),
             new String[] {"api"});

        // Get a rest client
        new ClientManager(this, accountType, loginOptions).getRestClient(this,
            new RestClientCallback() {
                @Override
                public void authenticatedRestClient(RestClient client) {
                    if (client == null) {
                        ForceApp.APP.logout(CoWorkerQuizActivity.this);
                        return;
                    }

                    // if first time after login
                    CoWorkerQuizActivity.this.oClient = client;

                    // Show everything
                    findViewById(R.id.root).setVisibility(View.VISIBLE);

                    // Fetch coworker list
                    if (bFirstTime) {
                        Log.d(TAG, "::fetching fresh co-worker..");
                        fetchAllCoWorkers();
                        bFirstTime = false;
                    }
                }
            });
    }

    /**
     * Called whenever a key, touch, or trackball event is dispatched to the
     * activity. Can be used to manage activity time-outs for passcode
     * management.
     */
    @Override
    public void onUserInteraction() {
        /*
         * Un-comment this block if you use the passcode screen

        ForceApp.APP.getPasscodeManager().recordUserInteraction();

         */
    }

    /**
     * Called after onCreate()/onRestart() and activity is now being displayed
     * to the user. It will be followed by onResume().
     */
    @Override
    protected void onStart() {
        Log.d(TAG, "::onStart");
        super.onStart();
    }

    /**
     * Called when an activity is going into the background, but has not (yet)
     * been killed. The counterpart to onResume().
     */
    @Override
    protected void onPause() {
        Log.d(TAG, "::onPause");
        super.onPause();
    }

    /**
     * Called when activity is no longer visible to the user. Will next receive
     * either onRestart(), onDestroy(), or nothing, depending on later user
     * activity.
     */
    @Override
    protected void onStop() {
        Log.d(TAG, "::onStop");
        super.onStop();

        // The activity is no longer visible (it is now "stopped")
        if(null != oMediaPlayer) {
            oMediaPlayer.release();
            oMediaPlayer = null;
        }
    }

    /**
     * Called when activity is being destroyed. Perform any final cleanup
     * beforehand.
     */
    @Override
    protected void onDestroy() {
        Log.d(TAG, "::onDestroy");
        super.onDestroy();
    }

    /**
     * Called to retrieve per-instance state from an activity before being
     * killed so that the state can be restored in onCreate().
     */
    @Override
    public void onSaveInstanceState(Bundle state) {
        Log.d(TAG, "::onSaveInstanceState");
        super.onSaveInstanceState(state);

        // save activity state if android stops our app without user interaction
        if(nNumConsecutiveRights > 0) {
            state.putInt("consecutive_rights", nNumConsecutiveRights);
        }
        if(nNumConsecutiveWrongs > 0) {
            state.putInt("consecutive_wrongs", nNumConsecutiveWrongs);
        }
        if(nMaxHigh > 0) {
            state.putInt("max_high", nMaxHigh);
        }
        if(nMaxLow > 0) {
            state.putInt("max_low", nMaxLow);
        }
    }

    /**
     * This method is called after onStart() when the activity is being
     * re-initialized from a previously saved state. Convenient to implement
     * here after all of the initialization has been done in onStart().
     */
    @Override
    public void onRestoreInstanceState(Bundle state) {
        Log.d(TAG, "::onRestoreInstanceState");
        super.onRestoreInstanceState(state);

        // restore activity state if android mistakenly stops our app without
        // user interaction
        nNumConsecutiveRights = state.getInt("consecutive_rights", 0);
        nNumConsecutiveWrongs = state.getInt("consecutive_wrongs", 0);
        nMaxHigh = state.getInt("nMaxHigh", 0);
        nMaxLow = state.getInt("nMaxLow", 0);
    }

    //============================
    // dialog methods

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
        case DIALOG_COWORKER_LIST:
            // TODO: Possibly, we can circumvent removeDialog on-click and
            // update the co-worker list on the existing dialog - have to figure
            // out how..
            break;
        case DIALOG_COWORKER_CONF:
            break;
        }
    }

    /**
     * 2 dialogs
     *   1. list of co-workers to choose from
     *   2. confirmation on selection
     */
    @Override
    public Dialog onCreateDialog(int id) {
        Dialog diag = null;
        final CharSequence[] coWorkerChoices = getCoWorkerChoices();
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        switch(id) {
        case DIALOG_COWORKER_LIST:
            builder.setTitle("Pick a name");
            builder.setItems(coWorkerChoices, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int item) {
                    sCurrentChoice = oCurrentChoices.get(item);
                    Toast.makeText(getApplicationContext(), sCurrentChoice, Toast.LENGTH_SHORT).show();

                    // remove dialog so next time we can refresh its set of coworkers..
                    // alternately, we could use onPrepareDialog to replace the list. But
                    // haven't figured out how to do this.
                    CoWorkerQuizActivity.this.removeDialog(DIALOG_COWORKER_LIST);

                    // ask for confirmation
                    showDialog(DIALOG_COWORKER_CONF);
                }
            });
            diag = builder.create();
            break;
        case DIALOG_COWORKER_CONF:
            builder.setMessage("Are you sure?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // show correct/incorrect status
                        applyNewChoice();
                        // play audio to applaud/ridicule based on consecutive rights/wrongs
                        playAudio();
                    }
                    })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        applyNoChoice();
                        dialog.cancel();
                    }
                });
            diag = builder.create();
            break;
        }
        return diag;
    }

    //============================
    // button onClick methods

    /**
     * This button brings up a list of co-worker choices for user selection
     * in a AlertDialog list.
     */
    public void doGuessWho(View v) {
        Log.d(TAG, "::doGuessWho");
        // show co-worker choices
        showDialog(DIALOG_COWORKER_LIST);
    }

    /**
     * Called to bring up the "Leaderboard"
     */
    public void doLeaderboard(View v) {
        Log.d(TAG, "::doLeaderboard");
        final Intent i = new Intent(this, LeaderboardActivity.class);
        startActivity(i);
    }

    /**
     * Called when "Quit" button is clicked.
     */
    public void doQuit(View v) {
        Log.d(TAG, "::doQuit");
        finish();
    }

    /**
     * Called when "Send Score" button is clicked.
     */
    public void doSendScore(View v) {
        Log.d(TAG, "::doSendScore");
        if(nMaxHigh > 0 || nMaxLow > 0) {
            /*
              Make SF REST call to send max low/high scores to server.
            */

            try {
                Map<String, Object> fields = new HashMap<String, Object>();
                fields.put("lowScore", nMaxLow);
                fields.put("highScore", nMaxHigh);
                StringEntity httpfields =
                    new StringEntity(new JSONObject(fields).toString(), HTTP.UTF_8);
                RestRequest request =
                    new RestRequest(RestMethod.POST, "/services/apexrest/CoWorkerQuiz/", httpfields);
                oClient.sendAsync(request, new AsyncRequestCallback() {
                    @Override
                    public void onSuccess(RestResponse response) {
                        try {
                            if (response.isSuccess()) {
                                Toast.makeText(getApplicationContext(),
                                    "Score sent successfully - " + response.asString(),
                                    Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(getApplicationContext(),
                                    "Response failure - " + response.asString(), Toast.LENGTH_SHORT)
                                        .show();
                            }
                        }
                        catch (Exception e) {
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
    }

    /**
     * Called when "Logout" button is clicked.
     * 
     * @param v - Current view
     */
    public void onLogoutClick(View v) {
        ForceApp.APP.logout(this);
    }

    //=================
    // private methods

    private void displayError(String msg) {
        Toast.makeText(getApplicationContext(), "Error: " + msg, Toast.LENGTH_LONG).show();
    }

    private void applyNewChoice() {
        if(sCurrentChoice.equals(getCorrectChoice())) {
            nNumConsecutiveRights += 1;
            nNumConsecutiveWrongs = 0;
            nMaxHigh = nNumConsecutiveRights > nMaxHigh ? nNumConsecutiveRights : nMaxHigh;
        }
        else {
            nNumConsecutiveWrongs += 1;
            nNumConsecutiveRights = 0;
            nMaxLow = nNumConsecutiveWrongs > nMaxLow ? nNumConsecutiveWrongs : nMaxLow;
        }
        setStatusMessage();
        //getGuessButton().setText("Continue", TextView.BufferType.NORMAL);

        // no guessing until page refreshed
        getGuessButton().setClickable(false);

        // sleep for 2 seconds, then conintue
        Handler handler = new Handler(); 
        handler.postDelayed(new Runnable() { 
                public void run() { 
                    // check if game over
                    if(oAllCoWorkers.isEmpty()) {
                        String status = getString(R.string.status_over);
                        getStatusMsgView().setText(status, TextView.BufferType.NORMAL);
                    }
                    // bring up fresh picture
                    else {
                        applyNoChoice();
                        setNewCoWorker();
                        getGuessButton().setClickable(true);
                    }
                }
            }, 2000); 
    }

    private void applyNoChoice() {
        sCurrentChoice = null;
        setStatusMessage();
        //getGuessButton().setText("Guess Who?", TextView.BufferType.NORMAL);
    }

    private void setStatusMessage() {
        String status = null;
        if(null == sCurrentChoice) {
            status = getString(R.string.status_default);
        }
        else if(nNumConsecutiveRights > 0) {
            status = getString(R.string.status_correct) + getCorrectChoice();
        }
        else if(nNumConsecutiveWrongs > 0) {
            status = getString(R.string.status_wrong) + getCorrectChoice();
        }
        getStatusMsgView().setText(status, TextView.BufferType.NORMAL);
        getCurrentHighView().setText("Current High: " + nNumConsecutiveRights,
            TextView.BufferType.NORMAL);
        getCurrentLowView().setText("Current  Low: " + nNumConsecutiveWrongs,
            TextView.BufferType.NORMAL);
    }

    private void playAudio() {
        final int audio = chooseAudio();
        if(audio > 0) {
            oMediaPlayer = MediaPlayer.create(this, audio);
            oMediaPlayer.start();
        }
    }

    private int chooseAudio() {
        int audio = -1;
        if(nNumConsecutiveRights > 0) {
            switch(nNumConsecutiveRights) {
            case 2:
                audio = R.raw.impressive;
                break;
            case 3:
                audio = R.raw.hattrick;
                break;
            case 5:
                audio = R.raw.dominating;
                break;
            case 10:
                audio = R.raw.unstoppable;
                break;
            case 20:
                audio = R.raw.godlike;
                break;
            case 50:
                audio = R.raw.perfect;
                break;
            }
        }
        else if(nNumConsecutiveWrongs >= 4) {
            audio = R.raw.humiliation;
        }
        return audio;
    }

    private void setNewCoWorker() {
        /*
         * 1. Choose a co-worker to show from list and create a list of choices 
         *    to show that includes the chosen co-worker
         * 2. Change image on screen with new co-worker.
         */
        chooseCoWorker();
        if(null != oCurrentCoWorker) {
            setCoWorkerImage();
        }
        else {
            Toast.makeText(getApplicationContext(), "Game Over!", Toast.LENGTH_LONG).show();
        }
    }

    private TextView getStatusMsgView() {
        return (TextView) findViewById(R.id.status_msg);
    }

    private TextView getCurrentHighView() {
        return (TextView) findViewById(R.id.current_high);
    }

    private TextView getCurrentLowView() {
        return (TextView) findViewById(R.id.current_low);
    }

    private Button getGuessButton() { return (Button) findViewById(R.id.guesswho); }

    private CharSequence getCorrectChoice() {
        return oCurrentCoWorker.getName();
    }

    private void fetchAllCoWorkers() {

        try{
            String soql = "SELECT Id, FirstName, LastName, FullPhotoUrl " +
                          "FROM User " +
                          "WHERE IsActive = true";
            RestRequest request = RestRequest.getRequestForQuery(sApiVersion, soql);
            oClient.sendAsync(request, new AsyncRequestCallback() {
                @Override
                public void onSuccess(RestResponse response) {
                    try {
                        if (response == null || response.asJSONObject() == null) {
                            return;
                        }

                        JSONArray records = response.asJSONObject().getJSONArray("records");
                        if (records.length() == 0) {
                            return;
                        }
                        Log.d(TAG, "Num co-workers returned = " + records.length());

                        oAllCoWorkers = new ArrayList<CoWorkerPerson>(records.length());

                        String photoUrl;
                        for (int i = 0; i < records.length(); i++) {
                            JSONObject jsonPerson = (JSONObject) records.get(i);
                            photoUrl = jsonPerson.getString("FullPhotoUrl");

                            // Skip users without photo
                            if (photoUrl.endsWith("/005/F")) {
                                continue;
                            }

                            CoWorkerPerson cwp = new CoWorkerPerson();
                            cwp.setFName(jsonPerson.getString("FirstName"));
                            cwp.setLName(jsonPerson.getString("LastName"));
                            cwp.setPicUrl(jsonPerson.getString("FullPhotoUrl") + 
                                "?oauth_token=" + oClient.getAuthToken());

                            oAllCoWorkers.add(cwp);
                        }

                        setNewCoWorker();
                        getGuessButton().setClickable(true);  // allow guessing
                        EventsObservable.get().notifyEvent(EventType.RenditionComplete);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        displayError(e.getMessage());
                    }
                }

                @Override
                public void onError(Exception exception) {
                    displayError(exception.getMessage());
                    EventsObservable.get().notifyEvent(EventType.RenditionComplete);
                }
            });
        }
        catch (Exception e) {
            e.printStackTrace();
            displayError(e.getMessage());
        }
    }

    private void chooseCoWorker() {
        final int MAX_CHOICES = 8;
        Random r = new Random();

        oCurrentCoWorker = null;
        oCurrentChoices.clear();
        if (oAllCoWorkers.size() > 0) {
            // choose current co-worker to guess
            oCurrentCoWorker = oAllCoWorkers.get(r.nextInt(oAllCoWorkers.size()));

            // remove current co-worker from all co-workers (so it won't be
            // shown again in this session)
            oAllCoWorkers.remove(oCurrentCoWorker);

            // create set of choices
            List<CoWorkerPerson> rest = new ArrayList<CoWorkerPerson>(oAllCoWorkers);
            int num_choices = Math.min(rest.size(), MAX_CHOICES);
            if (num_choices > 0) {
                for (int i = 0; i < num_choices; i++) {
                    CoWorkerPerson currChoice = rest.get(r.nextInt(rest.size()));
                    oCurrentChoices.add(currChoice.getName());
                    rest.remove(currChoice);
                }
                // insert correct choice at random spot
                oCurrentChoices.add(r.nextInt(num_choices), oCurrentCoWorker.getName());
            }
            else {
                // insert the only choice left
                oCurrentChoices.add(oCurrentCoWorker.getName());
            }
        }
    }

    private CharSequence[] getCoWorkerChoices() {
        return oCurrentChoices.toArray(new CharSequence[0]);
    }

    private void setCoWorkerImage() {
        Log.d(TAG, "::setCoWorkerImage - " + oCurrentCoWorker.getPicUrl());
        ImageView image_view = (ImageView)findViewById(R.id.picture);

        //oImageDownloader.download(oCurrentCoWorker.getPicUrl(), image_view);
        try {
            InputStream is = (InputStream) new URL(oCurrentCoWorker.getPicUrl()).getContent();

            Bitmap image = BitmapFactory.decodeStream(is);
            image_view.setImageBitmap(image);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package sensibleclock.softylogic.com.sensibleclock;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DigitalClock;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Context mContext;
    Button button;
    public static final String DETECTED_ACTIVITY = ".DETECTED_ACTIVITY";
    DigitalClock digitalClock;

    CountDownTimer countDownTimer;
    long timeLeftInMS;
    Button startStop;
    boolean running = false;
    NumberPicker numberPicker;
    TextView countdownTimerText;
    TextView selectMinutes;
    TextView lblDigitalclock;
    TextView userEngagingText;
    Calendar calendar;
    RelativeLayout layout;
    int hour;
    int Am_Pm;
    boolean coarse = false;
    //Define an ActivityRecognitionClient//
    private ActivityRecognitionClient mActivityRecognitionClient;



    @Override

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        calendar = new GregorianCalendar();
        layout = (RelativeLayout) findViewById(R.id.relativeLayout);
        userEngagingText = (TextView) findViewById(R.id.userEngagingText);
        selectMinutes = (TextView) findViewById(R.id.selectMinutes);
        countdownTimerText = (TextView) findViewById(R.id.countdown);
        digitalClock = (DigitalClock) findViewById(R.id.digitalClock);
        startStop = (Button) findViewById(R.id.startButton);
        numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
        numberPicker.setMaxValue(100);
        numberPicker.setMinValue(0);
        numberPicker.setWrapSelectorWheel(true);
        lblDigitalclock = (TextView) findViewById(R.id.lblDigitalClock);
        button = (Button) findViewById(R.id.btnCoarse);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(coarse == false){
                    countdownTimerText.setAlpha(0);
                    selectMinutes.setAlpha(0);
                    userEngagingText.setAlpha(0);
                    lblDigitalclock.setAlpha(0);
                    digitalClock.setAlpha(0);
                    startStop.setAlpha(0);
                    numberPicker.setAlpha(0);

                    coarse = true;

                    hour = calendar.get(Calendar.HOUR_OF_DAY);
                    if(hour > 0 && hour < 4){
                        layout.setBackgroundResource(R.drawable.morning);
                    }
                    else if(hour > 4 && hour < 10){
                        layout.setBackgroundResource(R.drawable.noon);
                    }
                    else if(hour > 10 && hour < 14){
                        layout.setBackgroundResource(R.drawable.afternoon);
                    }
                    else if(hour > 14 && hour < 18){
                        layout.setBackgroundResource(R.drawable.evening);
                    }
                    else
                        layout.setBackgroundResource(R.drawable.night);


                }
                else{
                    countdownTimerText.setAlpha(1);
                    selectMinutes.setAlpha(1);
                    userEngagingText.setAlpha(1);
                    lblDigitalclock.setAlpha(1);
                    digitalClock.setAlpha(1);
                    startStop.setAlpha(1);
                    numberPicker.setAlpha(1);

                    coarse = false;
                    layout.setBackgroundResource(0);
                }
            }
        });
        startStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startStop();
            }
        });
        updateTimer();
        mContext = this;


        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this).getString(
                        DETECTED_ACTIVITY, ""));

        mActivityRecognitionClient = new ActivityRecognitionClient(this);

        for(DetectedActivity activity: detectedActivities){
            if(activity.getType()== DetectedActivity.RUNNING){
                if(activity.getConfidence() > 70){
                    digitalClock.setTextSize(60f);
                }
            }
        }

    }
    private void startStop() {
        if(running){
            stop();
        }
        else{
            timeLeftInMS = (numberPicker.getValue() - 1 ) * 60 * 1000 ;
            start();

        }
    }
    private void start() {
        if(timeLeftInMS > 0){
            countDownTimer = new CountDownTimer(timeLeftInMS, 1000) {

                public void onTick(long millisUntilFinished) {
                    timeLeftInMS = millisUntilFinished;
                    updateTimer();

                }

                public void onFinish() {

                    if (countDownTimer != null){
                        countDownTimer.cancel();
                    }

                    running = false;
                    countdownTimerText.setText("00:00");
                    startStop.setText("Start");
                }
            }.start();
            startStop.setText("Stop");
        }

        running = true;
    }
    private void stop() {
        if (countDownTimer != null){
            countDownTimer.cancel();
        }

        running = false;
        countdownTimerText.setText("00:00");
        startStop.setText("Start");
    }
    private void updateTimer() {
        int minutes = (int) timeLeftInMS / 60000;
        int seconds = (int) (timeLeftInMS % 60000 / 1000);
        String timeLeftText = null;
        if(minutes <10) timeLeftText = "0";
        timeLeftText += "" + minutes;
        timeLeftText += ":";
        if(seconds <10) timeLeftText += "0";
        timeLeftText += seconds;
        countdownTimerText.setText(timeLeftText);

    }
    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
        updateDetectedActivitiesList();
    }
    @Override
    protected void onPause() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }
    public void requestUpdatesHandler(View view) {
//Set the activity detection interval. I’m using 3 seconds//
        Task<Void> task = mActivityRecognitionClient.requestActivityUpdates(
                3000,
                getActivityDetectionPendingIntent());
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void result) {
                updateDetectedActivitiesList();
            }
        });
    }
    //Get a PendingIntent//
    private PendingIntent getActivityDetectionPendingIntent() {
//Send the activity data to our DetectedActivitiesIntentService class//
        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

    }
    //Process the list of activities//
    protected void updateDetectedActivitiesList() {
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(mContext)
                        .getString(DETECTED_ACTIVITY, ""));
        for(DetectedActivity activity: detectedActivities){
            if(activity.getType()== DetectedActivity.RUNNING){
                if(activity.getConfidence() > 70){
                    digitalClock.setTextSize(60f);
                }
            }
        }


    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        if (s.equals(DETECTED_ACTIVITY)) {
            updateDetectedActivitiesList();
        }
    }
}

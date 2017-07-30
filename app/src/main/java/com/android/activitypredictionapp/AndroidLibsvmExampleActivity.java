package com.android.activitypredictionapp;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class AndroidLibsvmExampleActivity extends Activity {
    /**
     * Called when the activity is first created.
     */
    private static final String TAG = "Libsvm";
    private AccelerometerService mAccelerometerService;
    private boolean mBound;
    private Intent serviceIntent;
    private Button trainRunningButton;
    private Button trainEatingButton;
    private Button trainWalkingButton;
    private Button copyDataToFile;
    private Button stopPredictionBtn;
    private Button classifyButton;

    private TextView onGoingActivityTxt;

    private static boolean isRunning;
    private static boolean isEating;
    private static boolean isWalking;
    private static final int K = 3;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        Button trainButton = (Button) findViewById(R.id.train);
        classifyButton = (Button) findViewById(R.id.classifiy);

        trainRunningButton = (Button) findViewById(R.id.train_running);
        trainEatingButton = (Button) findViewById(R.id.train_eating);
        trainWalkingButton = (Button) findViewById(R.id.train_walking);
        copyDataToFile = (Button) findViewById(R.id.copy_data_to_file);
        stopPredictionBtn = (Button) findViewById(R.id.stop_prediction);
        onGoingActivityTxt = (TextView) findViewById(R.id.on_going_activity);
        onGoingActivityTxt.setVisibility(View.GONE);
        serviceIntent = new Intent(this, AccelerometerService.class);
        startService(serviceIntent);

        stopPredictionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopPrediction();
            }
        });

        copyDataToFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyDataBaseToFile();
            }
        });

        trainRunningButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trainRunning();
            }
        });

        trainEatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trainEating();
            }
        });

        trainWalkingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trainWalking();
            }
        });

        trainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trainSVM();
            }
        });
        classifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                classify();
//                predictActivity();
                calculatePercentage();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mBound == false)
            bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        classifyButton.setEnabled(false);
        onGoingActivityTxt.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound == true) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(TAG, "OnServiceConnected");
            AccelerometerService.LocalBinder localBinder = (AccelerometerService.LocalBinder) service;
            mAccelerometerService = localBinder.getService();
            mAccelerometerService.setHandler(handler);
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBound = false;
        }
    };

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {

                case AccelerometerService.CLOCK_TICK:
                    float testValue = msg.getData().getFloat("AxisValue");
                    Log.v(TAG, "Update Value Received : " + testValue);

                    break;

                case AccelerometerService.CLEAR_GRAPH: {
                    Toast.makeText(AndroidLibsvmExampleActivity.this, "Data Saved Successfully", Toast.LENGTH_LONG).show();
                    Log.v(TAG, "Data Saved Successfully");
                }
                break;

                case AccelerometerService.CLEAR_PREDICTION:
                    Toast.makeText(AndroidLibsvmExampleActivity.this, "Prediction Data Cleared", Toast.LENGTH_LONG).show();
                    onGoingActivityTxt.setVisibility(View.GONE);
                    break;

                case AccelerometerService.ACTIVITY_PREDICTION:
                    String str = msg.getData().getString(AccelerometerService.PREDICTED_ACTIVITY);
                    if (str.equals("1"))
                        str = "Eating";
                    else if (str.equals("2"))
                        str = "Running";
                    else
                        str = "Walking";
                    onGoingActivityTxt.setVisibility(View.VISIBLE);
                    onGoingActivityTxt.setText(str);
                    break;

                case AccelerometerService.ACCURACY:
                    float accuracy = msg.getData().getFloat(AccelerometerService.PREDICTION_ACCURACY);
                    onGoingActivityTxt.setVisibility(View.VISIBLE);
                    onGoingActivityTxt.setText("Accuracy : " + accuracy);
                    break;
                case AccelerometerService.SVM_TRAINED:
                    classifyButton.setEnabled(true);
                    Toast.makeText(AndroidLibsvmExampleActivity.this, "SVM Trained", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    };

    private void stopPrediction() {
        if (mAccelerometerService != null && mBound == true) {
            mAccelerometerService.stopPredictingActivity();
        }
    }

    private void copyDataBaseToFile() {
        if (mAccelerometerService != null && mBound == true) {
            mAccelerometerService.copyDataBaseToFile();
        }
    }

    private void trainSVM() {
        if (mAccelerometerService != null && mBound == true) {
            mAccelerometerService.train();
        }
    }


    private void trainRunning() {
        if (mAccelerometerService != null && mBound == true) {
            if (!isRunning) {
                trainRunningButton.setText("Stop Saving Running Data");
                trainEatingButton.setEnabled(false);
                trainWalkingButton.setEnabled(false);
                mAccelerometerService.startFetchingData("Running");
                isRunning = true;
            } else {
                trainRunningButton.setText("Start Saving Running Data");
                mAccelerometerService.stopFetchingData();
                trainEatingButton.setEnabled(true);
                trainWalkingButton.setEnabled(true);
                isRunning = false;
            }
        }
    }

    private void trainEating() {
        if (mAccelerometerService != null && mBound == true) {
            if (!isEating) {
                trainEatingButton.setText("Stop Saving Eating Data");
                trainRunningButton.setEnabled(false);
                trainWalkingButton.setEnabled(false);
                mAccelerometerService.startFetchingData("Eating");
                isEating = true;
            } else {
                trainEatingButton.setText("Start Saving Eating Data");
                mAccelerometerService.stopFetchingData();
                trainRunningButton.setEnabled(true);
                trainWalkingButton.setEnabled(true);
                isEating = false;
            }
        }
    }

    private void trainWalking() {
        if (mAccelerometerService != null && mBound == true) {
            if (!isWalking) {
                trainWalkingButton.setText("Stop Saving Walking Data");
                trainEatingButton.setEnabled(false);
                trainRunningButton.setEnabled(false);
                mAccelerometerService.startFetchingData("Walking");
                isWalking = true;
            } else {
                trainWalkingButton.setText("Start Saving Walking Data");
                mAccelerometerService.stopFetchingData();
                trainEatingButton.setEnabled(true);
                trainRunningButton.setEnabled(true);
                isWalking = false;
            }
        }
    }


    private void calculatePercentage() {
        if (mAccelerometerService != null && mBound == true) {
            onGoingActivityTxt.setText("Detecting...");
            onGoingActivityTxt.setVisibility(View.VISIBLE);
            mAccelerometerService.calculatePercentageAccuracy();
        }
    }

    private void predictActivity() {
        if (mAccelerometerService != null && mBound == true) {
            mAccelerometerService.stopFetchingData(); //stop fetching if any...
            mAccelerometerService.stopPredictingActivity();
            mAccelerometerService.startPredictingActivity();
        }
    }

}
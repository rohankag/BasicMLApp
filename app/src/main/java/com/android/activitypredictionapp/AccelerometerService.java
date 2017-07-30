package com.android.activitypredictionapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;


/**
 * Created by aditya on 10/1/16.
 */
public class AccelerometerService extends Service implements SensorEventListener {

    private static final String TAG = AccelerometerService.class.getSimpleName();
    private static final int K = 3;
    private SensorManager accelerometerManage;
    private Sensor senseAccelerometer;
    private IBinder mBinder = new LocalBinder();
    private SensorEvent mSensorEvent;
    private boolean doFetchData;
    private boolean isPredicting;
    private Handler mHandler;
    private String tableName;
    private int whichAxis;
    UserDbHelper userDbHelper;
    //Random number assigned for message which is our group number
    public static final int CLOCK_TICK = 22;
    public static final int CLEAR_GRAPH = 23;
    public static final int CLEAR_PREDICTION = 24;
    public static final int ACTIVITY_PREDICTION = 25;
    public static final int ACCURACY = 26;
    public static final int SVM_TRAINED = 27;
    public static final String PREDICTED_ACTIVITY = "predicted_activity";
    public static final String PREDICTION_ACCURACY = "prediction_accuracy";
    private float accuracy;
    private static final String BASE_URI = Environment.getExternalStorageDirectory() + "/databaseFolder/";

    private static ArrayList<AccumulatedData> accumulatedDataArrayList;

    // svm native
    private native int trainClassifierNative(String trainingFile, int kernelType,
                                             int cost, float gamma, int isProb, String modelFile);

    private native int doClassificationNative(float values[][], int indices[][],
                                              int isProb, String modelFile, int labels[], double probs[]);

    static {
        System.loadLibrary("signal");
    }


    public void train() {
        if (!generateTrainingTestingFiles())
            return;
        for (int i = 1; i <= 3; i++) {
            train(BASE_URI + "training" + i,
                    BASE_URI + "training_model" + i);
        }

        Message msg = mHandler.obtainMessage();
        msg.what = SVM_TRAINED;
        mHandler.sendMessage(msg);
    }

    private boolean generateTrainingTestingFiles() {

        try {
            String inputFile = BASE_URI + "training_set"; // Source File Name.
            // output file.
            File file = new File(inputFile);
            if (!file.exists()) {
                Toast.makeText(this, "Training Set File Not Found", Toast.LENGTH_LONG).show();
                return false;
            }
            Scanner scanner = new Scanner(file);
            int count = 0;
            while (scanner.hasNextLine()) {
                scanner.nextLine();
                count++;
            }
            System.out.println("Lines in the file: " + count);
            double nol = count / K; // No. of lines to be split and saved in each

            double temp = (count / nol);
            int temp1 = (int) temp;
            int nof = 0;
            if (temp1 == temp) {
                nof = temp1;
            } else {
                nof = temp1 + 1;
            }
            System.out.println("No. of files to be generated :" + nof);

            FileInputStream fstream = new FileInputStream(inputFile);
            DataInputStream in = new DataInputStream(fstream);

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;

            for (int j = 1; j <= nof; j++) {
                FileWriter fstream1 = new FileWriter(BASE_URI + "testing" + j); // Destination
                // File
                // Location
                BufferedWriter out = new BufferedWriter(fstream1);
                for (int i = 1; i <= nol; i++) {
                    strLine = br.readLine();
                    if (strLine != null) {
                        out.write(strLine);
                        if (i != nol) {
                            out.newLine();
                        }
                    }
                }
                out.close();
            }

            in.close();

            String testingfile1 = BASE_URI + "testing1";
            String testingfile2 = BASE_URI + "testing2";
            String testingfile3 = BASE_URI + "testing3";
            String trainingfile1 = BASE_URI + "training1";
            String trainingfile2 = BASE_URI + "training2";
            String trainingfile3 = BASE_URI + "training3";

            combine(testingfile2, testingfile3, trainingfile1);
            combine(testingfile1, testingfile3, trainingfile2);
            combine(testingfile1, testingfile2, trainingfile3);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void combine(String file1path, String file2, String file)
            throws IOException {
        ArrayList<String> list = new ArrayList<String>();
        try {

            BufferedReader br = null;
            BufferedReader r = null;

            br = new BufferedReader(new FileReader(file1path));
            r = new BufferedReader(new FileReader(file2));
            String s1 = null;
            String s2 = null;

            while ((s1 = br.readLine()) != null) {
                list.add(s1);
            }
            while ((s2 = r.readLine()) != null) {
                list.add(s2);
            }

            br.close();
            r.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        BufferedWriter writer = null;
        writer = new BufferedWriter(new FileWriter(file));
        String listWord;
        for (int i = 0; i < list.size(); i++) {
            listWord = list.get(i);
            writer.write(listWord);
            writer.write("\n");
        }

        writer.close();
    }

    public void train(String trainingFileLocation, String modelFileLocation) {
        // Svm training
        int kernelType = 2; // Radial basis function
        int cost = 1; // Cost
        int isProb = 0;
        float gamma = 0.0f; // Gamma
//        String trainingFileLoc = Environment.getExternalStorageDirectory() + "/databaseFolder/training_set";
//        String modelFileLoc = Environment.getExternalStorageDirectory() + "/databaseFolder/training_model";

        String trainingFileLoc = trainingFileLocation;
        String modelFileLoc = modelFileLocation;

        if (trainClassifierNative(trainingFileLoc, kernelType, cost, gamma, isProb,
                modelFileLoc) == -1) {
            Log.d(TAG, "training err");
        }
        Log.v(TAG, "Training is done");
    }

    public class AccumulatedData {
        private float[] accumulatedData = new float[150];

        AccumulatedData(float[] accumulatedData) {
            for (int i = 0; i < 150; i++) {
                this.accumulatedData[i] = accumulatedData[i];
            }
//            this.accumulatedData = accumulatedData;
        }

        float[] getAccumulatedData() {
            return this.accumulatedData;
        }

    }

    public class LocalBinder extends Binder {
        public AccelerometerService getService() {
            return AccelerometerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //create database in external directory...
        userDbHelper = new UserDbHelper(getApplicationContext());
        userDbHelper.createPatientTable(UserDbHelper.TABLE_NAME);
        accumulatedDataArrayList = new ArrayList<AccumulatedData>();
        accelerometerManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccelerometer = accelerometerManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelerometerManage.registerListener(this, senseAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accelerometerManage.unregisterListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startFetchingData(String activityLabel) {
        doFetchData = true;
        //start thread for saving accelerometer data...
        FetchAccelerometerDataThread threadObj = new FetchAccelerometerDataThread(activityLabel);
        threadObj.start();
    }

    public void calculatePercentageAccuracy() {
        float totalAccuracy = 0.0f;
        for (int i = 1; i <= 3; i++) {
            totalAccuracy += getAccuracy(Environment.getExternalStorageDirectory() + "/databaseFolder/testing" + i,
                    Environment.getExternalStorageDirectory() + "/databaseFolder/training_model" + i);
        }
        totalAccuracy = totalAccuracy / 3;
        Log.v(TAG, "Total Accuracy = " + totalAccuracy);

        Message msg = mHandler.obtainMessage();
        msg.what = ACCURACY;
        Bundle bundle = new Bundle();
        bundle.putFloat(PREDICTION_ACCURACY, totalAccuracy);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private float getAccuracy(String testingFileLocation, String modelFileLocation) {
        accuracy = 0;
        float[] lineDataArr = new float[150];
        ArrayList<AccumulatedData> arrList = new ArrayList<AccumulatedData>();
        ArrayList<String> groundTruthList = new ArrayList<String>();
        int k = 0;


        try {
            FileInputStream fstream = new FileInputStream(testingFileLocation);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String line;
            while ((line = br.readLine()) != null) {
                int firstSpaceIndex = line.indexOf(" ");
                groundTruthList.add(line.substring(0, firstSpaceIndex));
                line = line + " ";
                firstSpaceIndex++;
                int arrIndex = 0;
                for (int i = firstSpaceIndex, j = firstSpaceIndex; i < line
                        .length() && j < line.length(); ) {
                    i = line.indexOf(":", i);
                    j = line.indexOf(" ", j);
                    i++;
                    lineDataArr[arrIndex++] = Float.parseFloat(line.substring(i,
                            j));
                    j++;
                }
                arrList.add(new AccumulatedData(lineDataArr));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(AccelerometerService.this, "Train SVM First", Toast.LENGTH_SHORT).show();
        }
        int size = groundTruthList.size();
        int[] groundTruth = new int[size];
        float[][] values = new float[size][150];
        int[][] indices = new int[size][150];
        int[] indexArray = {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85,
                86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
                120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150
        };

        for (int i = 0; i < size; i++) {
            values[i] = arrList.get(i).getAccumulatedData();
            indices[i] = indexArray;
        }


        for (int i = 0; i < groundTruthList.size(); i++) {
            groundTruth[i] = Integer.parseInt(groundTruthList.get(i));
        }


        int[] labels = new int[size];
        double[] probs = new double[size];
        int isProb = 0; // Not probability prediction

        if (callSVM(values, indices, groundTruth, isProb, modelFileLocation, labels, probs) != 0) {
            Log.d(TAG, "Classification is incorrect");
        } else {
            String m = "";
            for (int l : labels)
                m += l + "";
            Log.v(TAG, m);
        }
        return accuracy;
    }


    public void startPredictingActivity() {
        isPredicting = true;
        PredictActivityThread predictActivityThread = new PredictActivityThread();
        predictActivityThread.start();
    }

    public void stopPredictingActivity() {
        isPredicting = false;
        accumulatedDataArrayList.clear();
    }

    public void stopFetchingData() {
        doFetchData = false;
    }

    public void copyDataBaseToFile() {
        Cursor cursor = userDbHelper.getTrainingData();
        if (cursor != null) {
            Log.v(TAG, "Cursor count : " + cursor.getCount());
            Log.v(TAG, "Cursor column count : " + cursor.getColumnCount());
            Log.v(TAG, "Last Column Name : " + cursor.getColumnName(cursor.getColumnCount() - 1));
            cursor.moveToFirst();
            try {
                PrintWriter writer = new PrintWriter(UserDbHelper.TRAINING_FILE_NAME, "UTF-8");
                while (!cursor.isAfterLast()) {
                    String str = cursor.getString(cursor.getColumnCount() - 1);
//                    String first = "";
//                    String second = "";
                    if (str.equals("Eating"))
                        str = "1";
                    else if (str.equals("Running"))
                        str = "2";
                    else
                        str = "3";
                    for (int i = 1; i < cursor.getColumnCount() - 1; i++) {
                        str = str + " " + i + ":" + cursor.getFloat(i);
//                        first += i + ",";
//                        second += cursor.getFloat(i) + "f,";
                    }
//                    Log.v(TAG, first);
//                    Log.v(TAG, second);
                    writer.println(str);
                    cursor.moveToNext();
                }
                writer.close();
                Log.v(TAG, "File Saved Successfully");
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    private class PredictActivityThread extends Thread {

        @Override
        public void run() {
            while (isPredicting) {
                float valuesArray[] = new float[150];
                int i = 149;
                while (i >= 0) {
                    try {
                        Thread.sleep(100);
                        SensorEvent sensorEvent = AccelerometerService.this.mSensorEvent;
                        valuesArray[i--] = sensorEvent.values[2];
                        valuesArray[i--] = sensorEvent.values[1];
                        valuesArray[i--] = sensorEvent.values[0];
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                accumulateDataForPrediction(valuesArray);
                Log.v(TAG, "Data sent For Prediction");
            }
            Message msg = mHandler.obtainMessage();
            msg.what = CLEAR_PREDICTION;
            mHandler.sendMessage(msg);
        }
    }

    private void accumulateDataForPrediction(float[] values) {
        AccumulatedData accumulatedData = new AccumulatedData(values);
        accumulatedDataArrayList.add(accumulatedData);
        getPredictedActivity(accumulatedDataArrayList);
    }

    private void getPredictedActivity(ArrayList<AccumulatedData> arrayList) {

        int list_size = arrayList.size();
        int size = 1;
        float[][] values = new float[size][150];
        int[][] indices = new int[size][150];
        int[] indexArray = {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85,
                86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
                120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150
        };

        for (int i = 0; i < size; i++) {
            values[i] = arrayList.get(list_size - 1).getAccumulatedData();
            indices[i] = indexArray;
        }

        int[] labels = new int[size];
        double[] probs = new double[size];
        Log.v(TAG, "Value Final = " + values[0][149]);

/////////////////////////////////

        /*
        float[][] values = {
                {
                        -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f,
                        10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f,
                        6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f,
                        -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f,
                        10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f,
                        6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f,
                        -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f,
                        10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f,
                        6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f,
                        -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f,
                        10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f,
                        6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f,
                        -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f,
                        10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f,
                        6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f, -2.7400055f, 10.762711f, 6.9476166f
                }

        };

        int[][] indices = {{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
                45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85,
                86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
                120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150
        }
        };
        int[] labels = new int[1];
        double[] probs = new double[1];
        */
////////////////////////////////
        int[] groundTruth = null;
        Log.v(TAG, "Values length = " + values.length + " Values Array Length = " + values[0].length);
        int isProb = 0; // Not probability prediction
        String modelFileLoc = Environment.getExternalStorageDirectory() + "/databaseFolder/training_model";

        if (callSVM(values, indices, groundTruth, isProb, modelFileLoc, labels, probs) != 0) {
            Log.d(TAG, "Classification is incorrect");
        } else {
            String m = "";
            for (int l : labels)
                m += l + "";
            Log.v(TAG, m);
            Message msg = mHandler.obtainMessage();
            msg.what = ACTIVITY_PREDICTION;
            Bundle bundle = new Bundle();
            bundle.putString(PREDICTED_ACTIVITY, m);
            msg.setData(bundle);
            mHandler.sendMessage(msg);
//            Toast.makeText(this, "Classification is done, the result is " + m, 2000).show();
        }
    }

    /**
     * classify generate labels for features.
     * Return:
     * -1: Error
     * 0: Correct
     */
    public int callSVM(float values[][], int indices[][], int groundTruth[], int isProb, String modelFile,
                       int labels[], double probs[]) {
        // SVM type
        final int C_SVC = 0;
        final int NU_SVC = 1;
        final int ONE_CLASS_SVM = 2;
        final int EPSILON_SVR = 3;
        final int NU_SVR = 4;

        // For accuracy calculation
        int correct = 0;
        int total = 0;
        float error = 0;
        float sump = 0, sumt = 0, sumpp = 0, sumtt = 0, sumpt = 0;
        float MSE, SCC;
        float accuracy = 0.0f;

        int num = values.length;
        int svm_type = C_SVC;
        if (num != indices.length)
            return -1;
        // If isProb is true, you need to pass in a real double array for probability array
        int r = doClassificationNative(values, indices, isProb, modelFile, labels, probs);

        // Calculate accuracy
        if (groundTruth != null) {
            if (groundTruth.length != indices.length) {
                return -1;
            }
            for (int i = 0; i < num; i++) {
                int predict_label = labels[i];
                int target_label = groundTruth[i];
                if (predict_label == target_label)
                    ++correct;
                error += (predict_label - target_label) * (predict_label - target_label);
                sump += predict_label;
                sumt += target_label;
                sumpp += predict_label * predict_label;
                sumtt += target_label * target_label;
                sumpt += predict_label * target_label;
                ++total;
            }

            if (svm_type == NU_SVR || svm_type == EPSILON_SVR) {
                MSE = error / total; // Mean square error
                SCC = ((total * sumpt - sump * sumt) * (total * sumpt - sump * sumt)) / ((total * sumpp - sump * sump) * (total * sumtt - sumt * sumt)); // Squared correlation coefficient
            }
            accuracy = (float) correct / total * 100;
            Log.d(TAG, "Classification accuracy is " + accuracy);
        }

        this.accuracy = accuracy;
        return r;
    }

    private class FetchAccelerometerDataThread extends Thread {

        String activityLabel = "";

        FetchAccelerometerDataThread(String activityLabel) {
            this.activityLabel = activityLabel;
        }

        @Override
        public void run() {
            while (doFetchData) {
                int count = 51;
                float fiveSecondsData[][] = new float[50][3];
                while (count-- > 1) {
                    try {
                    /*
                    Since frequency is 1Hz there fore sleep of 1 second is introduced.
                     */
                        Thread.sleep(100);
//                    Message msg = mHandler.obtainMessage();
//                    msg.what = CLOCK_TICK;
//                    Bundle bundle = new Bundle();
                        SensorEvent sensorEvent = AccelerometerService.this.mSensorEvent;
//                    float[] value = sensorEvent.values;
                        fiveSecondsData[count - 1] = sensorEvent.values;
//                    bundle.putFloat("AxisValue", value[whichAxis]);
//                    msg.setData(bundle);
//                    mHandler.sendMessage(msg);
                        //save value in database;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                userDbHelper.insertUserActivityData(fiveSecondsData, activityLabel);
                Log.v(TAG, "Saved data");
            }
            Message msg = mHandler.obtainMessage();
            msg.what = CLEAR_GRAPH;
            mHandler.sendMessage(msg);
        }
    }

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mSensorEvent = event;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

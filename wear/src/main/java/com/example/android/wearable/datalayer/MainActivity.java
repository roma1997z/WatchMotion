/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.datalayer;

import static com.example.android.wearable.datalayer.DataLayerListenerService.LOGD;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.view.DotsPageIndicator;
import android.support.wearable.view.FragmentGridPagerAdapter;
import android.support.wearable.view.GridViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.wearable.datalayer.fragments.AssetFragment;
import com.example.android.wearable.datalayer.fragments.DataFragment;
import com.example.android.wearable.datalayer.fragments.DiscoveryFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The main activity with a view pager, containing three pages:<p/>
 * <ul>
 * <li>
 * Page 1: shows a list of DataItems received from the phone application
 * </li>
 * <li>
 * Page 2: shows the photo that is sent from the phone application
 * </li>
 * <li>
 * Page 3: includes two buttons to show the connected phone and watch devices
 * </li>
 * </ul>
 */
public class MainActivity extends Activity implements  SensorEventListener,
        ConnectionCallbacks,
        OnConnectionFailedListener,
        DataApi.DataListener,
        MessageApi.MessageListener,
        CapabilityApi.CapabilityListener {

    private static final String TAG = "MainActivity";
    private static final String CAPABILITY_1_NAME = "capability_1";
    private static final String CAPABILITY_2_NAME = "capability_2";

    private GoogleApiClient mGoogleApiClient;
    private GridViewPager mPager;
    private DataFragment mDataFragment;
    private AssetFragment mAssetFragment;
    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;

    private View mView;
    private TextView mTextTitle;
    private TextView mTextValues, mAccText, mHyroText;
    private SensorManager mSensorManager;
    private Sensor mSensor, mSensorh, mSensorg;
    private int mSensorType;
    private long mShakeTime = 0;
    private long mRotationTime = 0;
    private long timeStart, relativeTime;
    String accData = "1", hyroData = "0";
    String mData = "0";
    private int SensNum = 1;
    private int step = 0;
    private int blockSize = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //mAccText = (TextView) findViewById(R.id.acctext);
        //mHyroText = (TextView) findViewById(R.id.hyrotext);
        setupViews();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorh = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorg = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Log.d(TAG, "Accelerometer" + Boolean.toString(mSensor.isWakeUpSensor()));
        mSensorManager.registerListener(this, mSensor, 10000);
        mSensorManager.registerListener(this, mSensorh, 10000);
        mSensorManager.registerListener(this, mSensorg, 10000);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            Toast.makeText(MainActivity.this, "Successs" + Boolean.toString(mSensor.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
        } else if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            Toast.makeText(MainActivity.this, "Successsg" + Boolean.toString(mSensorh.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(MainActivity.this, "Very bad" + Boolean.toString(mSensorg.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
        }
        timeStart = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGoogleApiClient.connect();
        mSensorManager.registerListener(this, mSensor, 10000);
        mSensorManager.registerListener(this, mSensorh, 10000);
        mSensorManager.registerListener(this, mSensorg, 10000);
    }

    @Override
    protected void onPause() {
        if ((mGoogleApiClient != null) && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.CapabilityApi.addListener(
                mGoogleApiClient, this, Uri.parse("wear://"), CapabilityApi.FILTER_REACHABLE);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
       /* LOGD(TAG, "onDataChanged(): " + dataEvents);

        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    Asset photoAsset = dataMapItem.getDataMap()
                            .getAsset(DataLayerListenerService.IMAGE_KEY);
                    // Loads image on background thread.
                    new LoadBitmapAsyncTask().execute(photoAsset);

                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
                    LOGD(TAG, "Data Changed for COUNT_PATH");
                    mDataFragment.appendItem("DataItem Changed", event.getDataItem().toString());
                } else {
                    LOGD(TAG, "Unrecognized path: " + path);
                }

            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                mDataFragment.appendItem("DataItem Deleted", event.getDataItem().toString());
            } else {
                mDataFragment.appendItem("Unknown data event type", "Type = " + event.getType());
            }
        }*/
    }

    private void increaseCounter() {
        TransTask task1 = new TransTask();
        task1.execute(accData);
      /*  new Thread(new Runnable() {
            @Override
            public void run() {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
        putDataMapReq.getDataMap().putString("com.example.key.count", accData);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        //  }}).start();*/
        if (step >= blockSize) {
            step = 0;
            accData = "";
            hyroData = "";
            //    timeStart=System.currentTimeMillis();

        }
    }

    public void onClicked(View view) {
        switch (view.getId()) {
            case R.id.capability_2_btn:
                showNodes(CAPABILITY_2_NAME);
                Toast.makeText(MainActivity.this, accData, Toast.LENGTH_SHORT).show();
                //  mAccText.setText("a");
                //  mHyroText.setText(hyroData);
                increaseCounter();
                // mDataFragment.appendItem("Sending Data", " = " + accData);
                // mDataFragment.appendItem("Sending Data", " = " + hyroData);
             /*   Log.d(TAG,"Accelerometer"+Boolean.toString(mSensor.isWakeUpSensor()));
                if(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null){
                    Toast.makeText(MainActivity.this, "Successs"+Boolean.toString(mSensor.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
                }
                else if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null){
                    Toast.makeText(MainActivity.this, "Successsg"+Boolean.toString(mSensorh.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(MainActivity.this, "Very bad"+Boolean.toString(mSensorg.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
                }*/
                //  Toast.makeText(MainActivity.this, "Accelerometer"+Boolean.toString(mSensor.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
                //  mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

                //  Toast.makeText(MainActivity.this, "Accelerometer"+Boolean.toString(mSensorg.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
                //  Toast.makeText(MainActivity.this, "Accelerometer"+Boolean.toString(mSensorh.isWakeUpSensor()), Toast.LENGTH_SHORT).show();
                break;
            case R.id.capabilities_1_and_2_btn:
                showNodes(CAPABILITY_1_NAME, CAPABILITY_2_NAME);
                break;
            default:
                Log.e(TAG, "Unknown click event registered");
        }
    }

    /**
     * Find the connected nodes that provide at least one of the given capabilities
     */
    private void showNodes(final String... capabilityNames) {

        PendingResult<CapabilityApi.GetAllCapabilitiesResult> pendingCapabilityResult =
                Wearable.CapabilityApi.getAllCapabilities(
                        mGoogleApiClient,
                        CapabilityApi.FILTER_REACHABLE);

        pendingCapabilityResult.setResultCallback(
                new ResultCallback<CapabilityApi.GetAllCapabilitiesResult>() {
                    @Override
                    public void onResult(
                            CapabilityApi.GetAllCapabilitiesResult getAllCapabilitiesResult) {

                        if (!getAllCapabilitiesResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Failed to get capabilities");
                            return;
                        }

                        Map<String, CapabilityInfo> capabilitiesMap =
                                getAllCapabilitiesResult.getAllCapabilities();
                        Set<Node> nodes = new HashSet<>();

                        if (capabilitiesMap.isEmpty()) {
                            showDiscoveredNodes(nodes);
                            return;
                        }
                        for (String capabilityName : capabilityNames) {
                            CapabilityInfo capabilityInfo = capabilitiesMap.get(capabilityName);
                            if (capabilityInfo != null) {
                                nodes.addAll(capabilityInfo.getNodes());
                            }
                        }
                        showDiscoveredNodes(nodes);
                    }

                    private void showDiscoveredNodes(Set<Node> nodes) {
                        List<String> nodesList = new ArrayList<>();
                        for (Node node : nodes) {
                            nodesList.add(node.getDisplayName());
                        }
                        LOGD(TAG, "Connected Nodes: " + (nodesList.isEmpty()
                                ? "No connected device was found for the given capabilities"
                                : TextUtils.join(",", nodesList)));
                        String msg;
                        if (!nodesList.isEmpty()) {
                            msg = getString(R.string.connected_nodes,
                                    TextUtils.join(", ", nodesList));
                        } else {
                            msg = getString(R.string.no_device);
                        }
                        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
        mDataFragment.appendItem("Message", event.toString());
    }

    @Override
    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
        LOGD(TAG, "onCapabilityChanged: " + capabilityInfo);
        mDataFragment.appendItem("onCapabilityChanged", capabilityInfo.toString());
    }

    private void setupViews() {
        mPager = (GridViewPager) findViewById(R.id.pager);
        mPager.setOffscreenPageCount(2);
        DotsPageIndicator dotsPageIndicator = (DotsPageIndicator) findViewById(R.id.page_indicator);
        dotsPageIndicator.setDotSpacing((int) getResources().getDimension(R.dimen.dots_spacing));
        dotsPageIndicator.setPager(mPager);
        mDataFragment = new DataFragment();
        mAssetFragment = new AssetFragment();
        DiscoveryFragment discoveryFragment = new DiscoveryFragment();
        List<Fragment> pages = new ArrayList<>();
        pages.add(mDataFragment);
        pages.add(mAssetFragment);
        pages.add(discoveryFragment);
        final MyPagerAdapter adapter = new MyPagerAdapter(getFragmentManager(), pages);
        mPager.setAdapter(adapter);
    }

    /**
     * Switches to the page {@code index}. The first page has index 0.
     */
    private void moveToPage(int index) {
        mPager.setCurrentItem(0, index, true);
    }

    private class MyPagerAdapter extends FragmentGridPagerAdapter {

        private List<Fragment> mFragments;

        public MyPagerAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount(int row) {
            return mFragments == null ? 0 : mFragments.size();
        }

        @Override
        public Fragment getFragment(int row, int column) {
            return mFragments.get(column);
        }

    }

    /*
     * Extracts {@link android.graphics.Bitmap} data from the
     * {@link com.google.android.gms.wearable.Asset}
     */
    private class LoadBitmapAsyncTask extends AsyncTask<Asset, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Asset... params) {

            if (params.length > 0) {

                Asset asset = params[0];

                InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                        mGoogleApiClient, asset).await().getInputStream();

                if (assetInputStream == null) {
                    Log.w(TAG, "Requested an unknown Asset.");
                    return null;
                }
                return BitmapFactory.decodeStream(assetInputStream);

            } else {
                Log.e(TAG, "Asset must be non-null");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {

            if (bitmap != null) {
                LOGD(TAG, "Setting background image on second page..");
                moveToPage(1);
                mAssetFragment.setBackgroundImage(bitmap);
            }
        }
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
       /* if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.d(TAG, "Sensors unreacheable");
            return;
        }*/
        relativeTime = System.currentTimeMillis() - timeStart;
        LOGD(TAG, "Sensors..");
        if ((event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) && (SensNum == 1)) {
            accData = accData + Long.toString(relativeTime) + "," + Float.toString(event.values[0]) + "," + Float.toString(event.values[1]) + "," + Float.toString(event.values[2]) + ",";
            SensNum = 2;
        } else if ((event.sensor.getType() == Sensor.TYPE_GYROSCOPE) && (SensNum == 2)) {
            accData = accData + Float.toString(event.values[0]) + "," + Float.toString(event.values[1]) + "," + Float.toString(event.values[2]) + "/";
            SensNum = 1;
            step = step + 1;
            if (step >= blockSize) increaseCounter();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color

        }
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color

        }
    }


    class TransTask extends AsyncTask<String,Void,Void> {
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    @Override
    protected Void doInBackground(String... params) {
        for(String str:params) {
            PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/count");
            putDataMapReq.getDataMap().putString("com.example.key.count", str);
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        }
        //  }}).start();
        return null;
    }
    @Override
    protected void onPostExecute(Void result) {
        super.onPostExecute(result);
    }
    }
}
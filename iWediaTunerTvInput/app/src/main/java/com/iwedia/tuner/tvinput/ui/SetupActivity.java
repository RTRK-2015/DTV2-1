/*
 * Copyright (C) 2015 iWedia S.A. Licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package com.iwedia.tuner.tvinput.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.media.tv.TvView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.iwedia.dtv.scan.IScanCallback;
import com.iwedia.dtv.scan.Modulation;
import com.iwedia.dtv.scan.ScanInstallStatus;
import com.iwedia.dtv.scan.Polarization;
import com.iwedia.dtv.scan.FecType;
import com.iwedia.dtv.service.SourceType;

import com.iwedia.tuner.tvinput.R;
import com.iwedia.tuner.tvinput.TvService;
import com.iwedia.tuner.tvinput.data.ChannelDescriptor;
import com.iwedia.tuner.tvinput.engine.ChannelManager;
import com.iwedia.tuner.tvinput.engine.DtvEngine;
import com.iwedia.tuner.tvinput.engine.RouteManager;
import com.iwedia.tuner.tvinput.utils.Logger;

/**
 * Setup activity for this TvInput.
 */
public class SetupActivity extends Activity {
    private TvView mTvView;
    /**
     * Object used to write to logcat output
     */
    private final Logger mLog = new Logger(
            TvService.APP_NAME + SetupActivity.class.getSimpleName(), Logger.ERROR);

    private static final int ON_INIT_TEXT = 0;

    private static final int ON_NEW_CHANNEL_FOUND = 1;

    private static final int ON_SCAN_START = 2;

    private static final int ON_SCAN_START_NIT = 3;

    private static final int ON_SCAN_COMPLETED = 4;

    private static final int ON_SCAN_COMPLETED_NIT = 5;

    private static final int ON_SETUP_FINISHED = 6;

    private enum ScanState {
        IDLE, SCANNING_MANUAL
    }

    private static ScanState mScanState;
    private static DtvEngine mDtvEngine = null;
    private static boolean isCallbackRegistered;
    public static boolean isAlreadyScanned = false;

    private RouteManager mRouteManager;
    private String mSubtitleText;
    private int mChannelCounter;
    private Handler mHandler;
    private Object mLocker;
    private int mScanCallbackId = 0;
    private boolean scanSuccessful = false;
    private EditText mFreqEditText;
    private EditText mSymRateEditText;
    private Button mStartScanButton;
    public Spinner mModulation;
    public Spinner mSymbolRate;
    private AlertDialog alert;

    public void displayModulation() {
        mLog.d("[displayModulation]");
        mModulation = (Spinner) findViewById(R.id.actionbar_filemenu_selectmenu1);
        mModulation.setVisibility(View.VISIBLE);
        String[] items = { "QPSK", "8PSK"};
        ArrayAdapter<String> spinnerMenuList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        spinnerMenuList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mModulation.setAdapter(spinnerMenuList);
    }

    public void displaySimbolRate() {
        mLog.d("[displaySimbolRate]");
        mSymbolRate = (Spinner) findViewById(R.id.actionbar_filemenu_selectmenu2);
        mSymbolRate.setVisibility(View.VISIBLE);
        String[] items = {"6.875 M", "6.9M"};
        ArrayAdapter<String> spinnerMenuList = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, items);
        spinnerMenuList.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSymbolRate.setAdapter(spinnerMenuList);

    }

    public void displayAlertDialog() {
        mLog.d("[displayAlertDialog]");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        TextView title = new TextView(this);
        // You Can Customise your Title here
        title.setText("SCANNING ...");
        title.setBackgroundColor(Color.WHITE);
        title.setPadding(10, 10, 10, 10);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.BLACK);
        title.setTextSize(30);
        builder.setCustomTitle(title);
        alert = builder.create();
        alert.show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLog.d("[onCreate]");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_activity);
        Intent intent = new Intent(getApplicationContext(), TvService.class);
        getApplicationContext().startService(intent);
        mFreqEditText = (EditText) findViewById(R.id.editTextFreq);
	    mSymRateEditText = (EditText) findViewById(R.id.editTextSymRate);
        mStartScanButton = (Button) findViewById(R.id.startScanButton);
        isCallbackRegistered = false;

        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ON_INIT_TEXT:
                        mSubtitleText = (String) msg.obj;

                        break;
                    case ON_NEW_CHANNEL_FOUND:
                        synchronized (mLocker) {
                            mChannelCounter++;
                            String temp = mSubtitleText + "\n"
                                    + "DVB channels found: " + mChannelCounter + "\n";
                        }
                        break;
                    case ON_SCAN_START:
                        mSubtitleText += "\n" + "Scan started";
                        displayAlertDialog();
                        break;
                    case ON_SCAN_COMPLETED:
                        mLog.i("mHandler - on scan completed");
                        mScanState = ScanState.IDLE;
                        scanSuccessful = true;
                        finish();
                        break;
                    case ON_SETUP_FINISHED:
                        mLog.d("[Handler][Finish setup activity]");
                        break;
                }
            }
        };

        mScanState = ScanState.IDLE;
        mLocker = new Object();
        mDtvEngine = DtvEngine.getInstance();

        if (mDtvEngine == null) {
            mLog.d("mDtvEngine == null");
            return;
        }

        mRouteManager = mDtvEngine.getRouteManager();

        if (mRouteManager == null) {
            mLog.d("mRouteManager == null");
            return;
        }

        if (mRouteManager.getSourceType() == SourceType.CAB) {
            mLog.d("[Displaying Modulation and Symbol Rate!]");

            displayModulation();
            displaySimbolRate();
        } else if (mRouteManager.getSourceType() == SourceType.SAT) {
            mLog.d("[Displaying Modulation]");
            displayModulation();
        }
        displayModulation();

        if (!isCallbackRegistered) {
            try {
                mScanCallbackId = mDtvEngine.getDtvManager().getScanControl()
                        .registerCallback(mScanCallback);
                isCallbackRegistered = true;
            } catch (RemoteException e2) {
                e2.printStackTrace();
            }
        }
    }

    public void onDestroy() {
        mLog.d("[onDestroy]");
        super.onDestroy();
        if (mDtvEngine != null && mDtvEngine.getDtvManager() != null) {
            try {
                mDtvEngine.getDtvManager().getScanControl().unregisterCallback(mScanCallbackId);
                isCallbackRegistered = false;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    public void onClickScanAction(View view) {
        mLog.d("[onClickScanAction][" + mScanState + "][" + (view == null) + "]");
        mStartScanButton.setEnabled(false);

        switch (mScanState) {
            case IDLE:
                mLog.i("case IDLE");
                mLog.d("[onClickScanAction] 1");
                mSubtitleText = "";
                Message msg = new Message();
                msg.what = ON_INIT_TEXT;
                msg.obj = mSubtitleText;
                mHandler.sendMessage(msg);
                mLog.d("[onClickScanAction] 2");
                mChannelCounter = 0;

                if (mDtvEngine == null) {
                    mLog.d("[onClickScanAction] mDtvEngine == null");
                    return;
                }

                if (mRouteManager == null) {
                    mLog.d("[onClickScanAction] mRouteManager == null");
                }

                if (mRouteManager.getSourceType() == SourceType.TER) {
                    // Terrestrial scan
                } else if (mRouteManager.getSourceType() == SourceType.CAB) {
                    // Cable scan

                } else if (mRouteManager.getSourceType() == SourceType.SAT) {
                    mLog.d("[onClickScanAction] SAT");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
				                int freq = Integer.valueOf(mFreqEditText.getText().toString());
                                int symRate = Integer.valueOf(mSymRateEditText.getText().toString());
				                Modulation modulation = Modulation.MODULATION_QPSK;
				                if ("QPSK".equals(mModulation.getSelectedItem().toString())) {
                                    modulation = Modulation.MODULATION_QPSK;
                                } else if ("8PSK".equals(mModulation.getSelectedItem().toString())) {
					                modulation = Modulation.MODULATION_8PSK;
				            }
                                mDtvEngine.getChannelManager().startManualScanSat(freq, modulation, Polarization.VERTICAL, symRate, FecType.FEC_5_6);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    mScanState = ScanState.SCANNING_MANUAL;
                    mHandler.sendEmptyMessage(ON_SCAN_START);
                    mLog.d("[onClickScanAction] 5");
		}

                break;
            case SCANNING_MANUAL:
                mLog.i("[onClickScanAction] case manual");
                if (view == null) {
                    // scan completed
                    mHandler.sendEmptyMessage(ON_SCAN_COMPLETED);
                } else {

                }
                mScanState = ScanState.IDLE;
                break;
        }
    }

    public void onClickFinishAction(View view) {
        mLog.d("[onClickFinish][Finishing setup activity]");

        if (scanSuccessful) {
            SetupActivity.this.setResult(RESULT_OK);
        } else {
            SetupActivity.this.setResult(RESULT_CANCELED);
        }
        SetupActivity.this.finish();
    }

    private IScanCallback mScanCallback = new IScanCallback.Stub() {
        @Override
        public void antennaConnected(int routeId, boolean state) {
            mLog.d("[antennaConnected][routeId:" + routeId + "][connected: " + state + "]");
        }

        @Override
        public void installServiceDATAName(int routeId, String name) {
            mLog.d("[installServiceDATAName][routeId:" + routeId + "][name: " + name + "]");
            mHandler.sendEmptyMessage(ON_NEW_CHANNEL_FOUND);
        }

        @Override
        public void installServiceDATANumber(int routeId, int name) {
            mLog.d("[installServiceDATANumber][routeId:" + routeId + "][name: " + name + "]");
        }

        @Override
        public void installServiceRADIOName(int routeId, String name) {
            mLog.d("[installServiceRADIOName][routeId:" + routeId + "][name: " + name + "]");
            mHandler.sendEmptyMessage(ON_NEW_CHANNEL_FOUND);
        }

        @Override
        public void installServiceRADIONumber(int routeId, int name) {
            mLog.d("[installServiceRADIONumber][routeId:" + routeId + "][name: " + name + "]");
        }

        @Override
        public void installServiceTVName(int routeId, String name) {
            mLog.d("[installServiceTVName][routeId:" + routeId + "][name: " + name + "]");
            if (!name.contains(ChannelManager.IP_CHANNEL_NAME) && !name.contains(ChannelManager.DVB_CAB_VOD_CHANNEL_NAME)) {
                mHandler.sendEmptyMessage(ON_NEW_CHANNEL_FOUND);
            }
        }

        @Override
        public void installServiceTVNumber(int routeId, int name) {
            mLog.d("[installServiceTVNumber][routeId:" + routeId + "][name: " + name + "]");
        }

        @Override
        public void installStatus(ScanInstallStatus scanStatus) {
            mLog.d("[installStatus][" + scanStatus + "]");
        }

        @Override
        public void networkChanged(int networkId) {
            mLog.d("[networkChanged][network Id: " + networkId + "]");
        }

        @Override
        public void sat2ipServerDropped(int routeId) {
            mLog.d("[sat2ipServerDropped][routeId:" + routeId + "]");
        }

        @Override
        public void scanFinished(int routeId) {
            mLog.d("[scanFinished][routeId:" + routeId + "]");
            try {
                mDtvEngine.getChannelManager().refreshChannelList();
                isAlreadyScanned = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            //Use current selected frontend
            synchronized (mScanState) {
                if (mScanState == ScanState.SCANNING_MANUAL) {
                    onClickScanAction(null);
                    mLog.i("[scanFinished][Scan finshed for IP route]");
                }
            }
        }

        @Override
        public void scanNoServiceSpace(int routeId) {
            mLog.d("[scanFinished][routeId:" + routeId + "]");
        }

        @Override
        public void scanProgressChanged(int routeId, int value) {
            mLog.d("[scanProgressChanged][routeId:" + routeId + "]");
        }

        @Override
        public void scanTunFrequency(int routeId, int frequency) {
            mLog.d("[scanTunFrequency][routeId:" + routeId + "][frequency: " + frequency + "]");
        }

        @Override
        public void signalBer(int routeId, int ber) {
            mLog.d("[signalBer][routeId:" + routeId + "]");
        }

        @Override
        public void signalQuality(int routeId, int quality) {
            mLog.d("[signalQuality][routeId:" + routeId + "][quality: " + quality + "]");
        }

        @Override
        public void signalStrength(int routeId, int strength) {
            mLog.d("[signalStrength][routeId:" + routeId + "][strength: " + strength + "]");
        }

        @Override
        public void triggerStatus(int routeId) {
            mLog.d("[triggerStatus][routeId:" + routeId + "]");
        }

        @Override
        public void signalReturned() {
            mLog.d("[signalReturned]");
        }

        @Override
        public void signalLost() {
            mLog.d("[signalLost]");
        }

        @Override
        public void tunerLocked(int id, boolean locked){
            mLog.d("[tunerLocked]");
        }
    };
}

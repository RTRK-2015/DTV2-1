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
package com.iwedia.tuner.tvinput.engine;

import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.RemoteException;
import android.view.WindowManager;

import com.iwedia.dtv.DTVServiceLocator;
import com.iwedia.dtv.IDTVManager;
import com.iwedia.dtv.epg.IEpgControl;
import com.iwedia.dtv.route.broadcast.routemanager.Routes;
import com.iwedia.dtv.service.Service;
import com.iwedia.dtv.service.ServiceDescriptor;
import com.iwedia.tuner.tvinput.TvService;
import com.iwedia.tuner.tvinput.callbacks.EpgCallback;
import com.iwedia.tuner.tvinput.data.ChannelDescriptor;
import com.iwedia.tuner.tvinput.engine.epg.EpgFull;
import com.iwedia.tuner.tvinput.engine.epg.EpgNowNext;
import com.iwedia.tuner.tvinput.utils.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Semaphore;

/**
 * Manager for handling MW Components.
 */
public class DtvEngine {

    /**
     * Object used to write to logcat output
     */
    private static final Logger mLog = new Logger(TvService.APP_NAME + DtvEngine.class.getSimpleName(),
            Logger.ERROR);


    public enum MwRunningState {
        UNKNOWN, NOT_RUNNING, RUNNING
    };

    /* Current route used for playback */
    private Routes mCurrentRoutes = null;


    /**
     * Comedia's Master list index
     */
    public static final int MASTER_LIST_INDEX = 0;

    private int mCallbackId;

    public void setCallbackId(int cbId) {
        mCallbackId = cbId;
    }
    /**
     * CallBack for EPG events.
     */
    public interface IEpgListener {

        /** Update Now Next values. */
        public void updateNowNext(int filterID, int serviceIndex);

        /** Update EPG list */
        public void updateEpgList();
    }

    /**
     * DtvManager instance
     */
    private IDTVManager mDtvManager = null;

    /**
     * Audio Track manager instance
     */
    private AudioManager mAudioManager;

    /**
     * Volume manager instance
     */
    private android.media.AudioManager mVolumeManager;

    /**
     * Channel manager instance
     */
    private ChannelManager mChannelManager;

    /**
     * Route manager instance
     */
    private RouteManager mRouteManager;

    /**
     * Instance of this manager
     */
    private static DtvEngine sInstance = null;

    /**
     * Current active channel
     */
    private int mCurrentlyActiveChannel = 0;

    public void setCurrentlyActiveChannel(int ac) {
        mCurrentlyActiveChannel = ac;
    }

    /**
     * Thread for handler creation
     */
    private HandlerThread mHandlerThread;

    /** Logic for acquisition timings */
    private EpgAcquisitionManager mEpgAcquisitionManager;

    /** Handler for adding EPG events */
    private Handler mEpgHandler;

    /** EPG CallBack */
    private EpgCallback mEPGCallBack = null;
    private int mEPGCAllbackId;

    /** EPG manager helper class */
    private EpgManager mEpgManager = null;

    /**
     * Application context
     */
    private static Context mContext;

    /**
     * Current volume
     */
    private int mVolume;

    /**
     * Video destination rectangle
     */

    private static CheckMiddlewareAsyncTask mCheckMw;

    private static MwRunningState mMwRunningState = MwRunningState.UNKNOWN;

    private static Semaphore mMwLocker = new Semaphore(0);

    private static int mMwClientWaitingCounter;

    private static Object mMwClientWaitingCounterLocker = new Object();

    public static DTVServiceLocator mServiceLocator = null;

    /**
     * Gets an instance of this manager
     *
     * @return Instance of this manager
     */
    public static DtvEngine getInstance() {
        return sInstance;
    }

    /**
     * Instantiates this manager
     *
     * @throws RemoteException If something is wrong with initialization of MW API
     */
    public static void instantiate(Context context) {
        mLog.d("[instantiate]");
        mContext = context;
        if (sInstance == null) {
            switch (mMwRunningState) {
                case UNKNOWN:
                    synchronized (mMwClientWaitingCounterLocker) {
                        mMwClientWaitingCounter = 0;
                    }
                    mMwRunningState = MwRunningState.NOT_RUNNING;
                    mCheckMw = new CheckMiddlewareAsyncTask();
                    mCheckMw.execute();
                case NOT_RUNNING:
                    synchronized (mMwClientWaitingCounterLocker) {
                        mMwClientWaitingCounter++;
                    }
                    // task already started, wait for finish
                    try {
                        mLog.d("[instantiate][waiting for client: " + mMwClientWaitingCounter + "]");
                        mMwLocker.acquire();
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                    break;
                case RUNNING:
                    mLog.d("[instantiate][already running]");
                    break;
            }
        }
    }

    /**
     * Constructor
     *
     * @throws RemoteException If something is wrong with initialization of MW API
     */
    private DtvEngine(DTVServiceLocator locator) throws RemoteException {
        mVolumeManager = (android.media.AudioManager) mContext
                .getSystemService(Context.AUDIO_SERVICE);
        mServiceLocator = locator;
        mDtvManager = locator.getDTVManager();
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
    }

    /**
     * Initialize Service.
     */
    private void initializeDtvFunctionality() throws RemoteException {
        mLog.d("[initializeDtvFunctionality]");
        mRouteManager = new RouteManager(mDtvManager);
        mAudioManager = new AudioManager(mDtvManager.getAudioControl());
        mChannelManager = new ChannelManager(sInstance, mContext);
        mChannelManager.init();
        mHandlerThread = new HandlerThread(TvService.class.getSimpleName());
        mHandlerThread.start();
        mEpgAcquisitionManager = new EpgAcquisitionManager(mContext);
        mEpgAcquisitionManager.loadEpgPrefs();
        mEpgHandler = new Handler(mHandlerThread.getLooper());
        mEPGCallBack = new EpgCallback(this);
        mEpgManager = new EpgManager(this);
    }

    public IDTVManager getDtvManager() {
        mLog.d("[getDtvManager]");
        return mDtvManager;
    }

    public EpgCallback getEPGCallBack() {
        return mEPGCallBack;
    }

    /**
     * Stop MW video playback.
     *
     * @throws RemoteException
     */
    public void stop() throws RemoteException {
        mLog.d("[stop]");
        try {
            mDtvManager.getServiceControl().stopService(mCurrentRoutes.getLiveRouteID());
        } catch (Exception e) {

        }
    }

    /**
     * Change Channel by Number.
     *
     * @throws RemoteException
     */
    public boolean start(ChannelDescriptor channel) throws RemoteException {
        mLog.d("[startDvb][" + channel.toString() + "]");

        mCurrentRoutes = mRouteManager.getRouteByServiceType(channel.getSourceType());
        if ((mCurrentRoutes == null) || (mCurrentRoutes.getLiveRoute() == null)) {
            mLog.e("[startDvb][unknown source type: " + channel.getSourceType() + "]");
            return false;
        }

        mCurrentlyActiveChannel = channel.getServiceId();
        mDtvManager.getServiceControl().startService(mCurrentRoutes.getLiveRouteID(), MASTER_LIST_INDEX,
                mCurrentlyActiveChannel);

        mDtvManager.getDisplayControl().scaleWindow(mCurrentRoutes.getLiveRouteID(), 0, 0, 1920, 1080);
        return true;
    }

    public int getCurrentServiceIndex() throws RemoteException {
        mLog.d("[getCurrentServiceIndex]");
        Service service = mDtvManager.getServiceControl().getActiveService(mCurrentRoutes.getLiveRouteID());
        return service.getServiceIndex();
    }

    /**
     * Set Current Volume.
     */
    public void setVolume(double volume) {
        mLog.d("[setVolume]");
        try {
            if (isMuted()) {
                setMute();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mVolume = (int) volume;
        mVolumeManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, mVolume, 0);
    }

    public boolean isMuted() throws RemoteException {
        mLog.d("[isMuted]");
        boolean throwException = false;
        boolean isMuted = false;
        Method isMasterMute = null;
        try {
            isMasterMute = android.media.AudioManager.class.getDeclaredMethod("isMasterMute",
                    (Class[]) null);
        } catch (NoSuchMethodException e) {
            throwException = true;
            e.printStackTrace();
        }
        if (isMasterMute != null) {
            try {
                isMuted = (Boolean) isMasterMute.invoke(mVolumeManager);
            } catch (IllegalAccessException e) {
                throwException = true;
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                throwException = true;
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                throwException = true;
                e.printStackTrace();
            }
        }
        if (throwException) {
            throw new RemoteException("Failed to reflect isMasterMute method.");
        }
        return isMuted;
    }

    /**
     * Set Volume Mute Status.
     *
     * @throws RemoteException
     */
    public void setMute() throws RemoteException {
        mLog.d("[setMute]");

        boolean throwException = false;
        Method setMasterMute = null;
        try {
            setMasterMute = android.media.AudioManager.class.getDeclaredMethod("setMasterMute",
                    boolean.class, int.class);
        } catch (NoSuchMethodException e) {
            throwException = true;
            e.printStackTrace();
        }
        if (setMasterMute != null) {
            try {
                setMasterMute.invoke(mVolumeManager, !isMuted(), 0);
            } catch (IllegalAccessException e) {
                throwException = true;
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                throwException = true;
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                throwException = true;
                e.printStackTrace();
            }
        }
        if (throwException) {
            throw new RemoteException("Failed to reflect setMasterMute method.");
        }
    }

    /**
     * Called in order to update the full EPG list
     * @throws RemoteException
     */
    public void updateEpgList() throws RemoteException {
        mLog.d("[updateEpgList]");
        mEpgHandler.post(new EpgFull(mContext, mEpgAcquisitionManager, getCurrentServiceIndex(),
                getCurrentTransponder()));
    }

    public Long getCurrentTransponder() throws RemoteException {
        ServiceDescriptor serviceDescriptor = mDtvManager.getServiceControl().getServiceDescriptor(
                MASTER_LIST_INDEX, getCurrentServiceIndex());
        return (long) serviceDescriptor.getFrequency();
    }
    /**
     * Called in order to update EPG Now and Next events
     */
    public void updateNowNext(int filterID, int serviceIndex) {
        mLog.d("[updateNowNext][filter id: " + filterID + "][service index: " + serviceIndex + "]");

        // ODOT: Create a new EpgNowNext Runnable to update epg event

        mEpgHandler.post(new EpgNowNext(mContext, mCurrentlyActiveChannel, filterID));
    }
    /**
     * Gets Audio Manager
     *
     * @return Manager instance
     */
    public AudioManager getAudioManager() {
        mLog.d("[getAudioManager]");
        return mAudioManager;
    }

    /**
     * Gets Channel Manager
     *
     * @return Manager instance
     */
    public ChannelManager getChannelManager() {
        mLog.d("[getChannelManager]");
        return mChannelManager;
    }

    /**
     * Gets Route Manager
     *
     * @return Manager instance
     */
    public RouteManager getRouteManager() {
        mLog.d("[getRouteManager]");
        return mRouteManager;
    }
    /**
     * Gets MW Control handle of EPG
     *
     * @return EpgControl handle
     * @throws RemoteException
     */
    public IEpgControl getEpgControl() throws RemoteException {
        return mDtvManager.getEpgControl();
    }

    public EpgManager getEpgManager() {
        return mEpgManager;
    }

    /**
     * Gets Epg Acquisition Manager
     *
     * @return EpgAcquisitionManager instance
     */
    public EpgAcquisitionManager getEpgAcquisitionManager() {
        return mEpgAcquisitionManager;
    }

    /**
     * Deinit DVB manager
     */
    public void deinit() {
        mMwRunningState = MwRunningState.UNKNOWN;

        // ODOT: Unregister EPG callback

        try {
            mLog.i("THE LAND WILL BURN");
            mEpgManager.unregisterCallback(mCallbackId);
            mLog.i("AND THE PEOPLE TOO");
            stop();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        sInstance = null;
        mHandlerThread.quit();
        mHandlerThread = null;
    }


    private static class CheckMiddlewareAsyncTask extends AsyncTask<Void, Void, String> {

        // ! in ms
        private int mWaitCycleMs;

        // ! in wait cycle
        private int mWaitCounter;


        public CheckMiddlewareAsyncTask() {
            super();

            mWaitCycleMs = 1000;
            mWaitCounter = 100000;
        }

        @Override
        protected String doInBackground(Void... params) {
            mServiceLocator = new DTVServiceLocator();

            while (true) {
                if (mServiceLocator.connectBlocking(mContext, mWaitCycleMs) != null) {
                    break;
                }

                if (mWaitCounter == 0) {
                    mLog.d("[CheckMiddlewareAsyncTask][doInBackground][timeout 10 seconds, mw not started]");
                    break;
                }
                mLog.d("[CheckMiddlewareAsyncTask][doInBackground][wait for MW service][" + mWaitCounter + "]");
                mWaitCounter--;
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            mLog.d("[CheckMiddlewareAsyncTask][onPostExecute][result: " + result + "]");
            if (mServiceLocator.getDTVManager() != null) {
                try {
                    sInstance = new DtvEngine(mServiceLocator);
                    sInstance.initializeDtvFunctionality();

                    mMwRunningState = MwRunningState.RUNNING;

                    int mwClientWaitingCounter;
                    synchronized (mMwClientWaitingCounterLocker) {
                        mwClientWaitingCounter = mMwClientWaitingCounter;
                        mMwClientWaitingCounter = 0;
                    }

                    mLog.d("[onPostExecute][releasing: " + mwClientWaitingCounter + "]");

                    mMwLocker.release(mwClientWaitingCounter);
                } catch (RemoteException re) {
                    re.printStackTrace();
                }
            }
        }
    }

}

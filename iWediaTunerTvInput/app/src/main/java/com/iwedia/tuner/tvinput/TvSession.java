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

package com.iwedia.tuner.tvinput;

import android.content.ContentUris;
import android.content.Context;
import android.media.tv.TvContentRating;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;
import android.media.tv.TvTrackInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;

import com.iwedia.dtv.audio.AudioTrack;
import com.iwedia.dtv.display.SurfaceBundle;
import com.iwedia.dtv.service.IServiceCallback;
import com.iwedia.dtv.service.ServiceListUpdateData;
import com.iwedia.dtv.service.ServiceStateChangeError;
import com.iwedia.dtv.service.ServiceType;
import com.iwedia.tuner.tvinput.callbacks.EpgCallback;
import com.iwedia.tuner.tvinput.data.ChannelDescriptor;
import com.iwedia.tuner.tvinput.engine.AudioManager;
import com.iwedia.tuner.tvinput.engine.ChannelManager;
import com.iwedia.tuner.tvinput.engine.DtvEngine;
import com.iwedia.tuner.tvinput.engine.RouteManager;
import com.iwedia.tuner.tvinput.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;

public class TvSession extends TvInputService.Session{
    private static final int DISP_LAYER_VIDEO_ID_0 = 0x01;
    /**
     * Application context
     */
    public static Context mContext;
    /**
     * Session object
     */
    public static TvSession sSession;
    /**
     * TextureView for rendering subtitles, ovned by overlay view
     */
    private static ImageView mImageViewRadio = null;
    /**
     * Object used to write to logcat output
     */
    private final Logger mLog = new Logger(TvService.APP_NAME
            + TvSession.class.getSimpleName(), Logger.ERROR);
    /**
     * Stores tracks acquired from Comedia MW
     */
    private ArrayList<TvTrackInfo> mTracks = new ArrayList<TvTrackInfo>();
    /**
     * Stores real Comedia MW tracks indexes
     */
    private HashMap<String, Integer> mTracksIndices = new HashMap<String, Integer>();
    /**
     * Flag that is used to determine weather subtitles are enabled
     */
    private boolean mIsSubtitleEnabled = false;
    /**
     * DvbManager for accessing MW API
     */
    private DtvEngine mDtvEngine;

    /**
     * Audio track manager
     */
    private AudioManager mAudioManager;
    /**
     * Route manager
     */
    private RouteManager mRouteManager;

    /**
     * Listener for session events
     */
    private ITvSession mSessionListener;
    /**
     * Channel manager object
     */
    private ChannelManager mChannelManager;
    /**
     * Input ID for TV session
     */
    private String mInputID;
    /**
     * Android TIF manager
     */
    private TvInputManager mTvManager;
    /**
     * Video playback surface returned by TIF
     */
    private Surface mVideoSurface = null;
    /**
     * Overview layout omposition
     */
    private ViewGroup mOverlayView = null;
    /**
     * Service callback identifier
     */
    private int mServiceCallbackId = 0;
    /**
     * Is current content block by parental control
     */
    private boolean mContentIsBlocked = false;

    private boolean reshowSubtitle = false;

    /**
     * Uri of the currently active channel
     */
    private ChannelDescriptor mCurrentChannel = null;

    private IServiceCallback mServiceCallback = new IServiceCallback.Stub() {
        @Override
        public void channelChangeStatus(int routeId, boolean channelChanged, ServiceStateChangeError reason) {
            mLog.d("[channelChangeStatus][" + routeId + "][" + channelChanged + "]" + "[" + reason + "]");

            // ODOT: Register EPG callback on first successfull channel change

            try {
                mLog.i("THE SKIES WILL BURN");
                int id = mDtvEngine.getEpgManager().registerCallback(mDtvEngine.getEPGCallBack());
                mDtvEngine.setCallbackId(id);
                mLog.i("THE OCEANS TOO");
                updateTracks();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            notifyVideoAvailable();
            mLog.i("channelChangeStatus with notifyVideoAvailable()");
        }

        @Override
        public void safeToUnblank(int routeId) {
            mLog.d("[safeToUnblank][" + routeId + "]");
        }

        @Override
        public void serviceScrambledStatus(int routeId, boolean serviceScrambled) {
            mLog.d("[serviceScrambledStatus][" + routeId + "][" + serviceScrambled + "]");
        }

        @Override
        public void serviceStopped(int routeId, boolean serviceStopped, ServiceStateChangeError ssce) {
            mLog.d("[serviceStopped][" + routeId + "][" + serviceStopped + "]");
        }

        @Override
        public void signalStatus(int routeId, boolean signalAvailable) {
            mLog.d("[signalStatus][" + routeId + "][" + signalAvailable + "]");
        }

        @Override
        public void updateServiceList(ServiceListUpdateData serviceListUpdateData) {
            mLog.d("[updateServiceList][service list update date: " + serviceListUpdateData + "]");
        }
    };

    /**
     * Constructor
     *
     * @param sessionListener Listener through which reporting when session onRelease() is
     *                        called.
     */
    public TvSession(Context context, ITvSession sessionListener, String inputID) {
        super(context);
        mLog.d("[TvSession][Started!]");
        mDtvEngine = DtvEngine.getInstance();
        mTvManager = (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        mInputID = inputID;
        mSessionListener = sessionListener;
        mContext = context;
        sSession = this;
        mIsSubtitleEnabled = ((CaptioningManager) mContext
                .getSystemService(Context.CAPTIONING_SERVICE)).isEnabled();
        initTvManagers();
    }

    private boolean initTvManagers() {
        mDtvEngine = DtvEngine.getInstance();
        if (mDtvEngine == null) {
            return false;
        }
        mChannelManager = mDtvEngine.getChannelManager();
        mAudioManager = mDtvEngine.getAudioManager();
        mRouteManager = mDtvEngine.getRouteManager();
        try {
            mServiceCallbackId = (mDtvEngine.getDtvManager().getServiceControl())
                    .registerCallback(mServiceCallback);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onRelease() {
        mLog.d("[onRelease]");
        resetTracks();
        stopPlayback();
        try {
           //
            if (mDtvEngine != null) {
                if (mDtvEngine.getDtvManager() != null) {
                    (mDtvEngine.getDtvManager().getServiceControl()).unregisterCallback(mServiceCallbackId);
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mContentIsBlocked = false;
        mSessionListener.onSessionRelease(this);

        mDtvEngine.mServiceLocator.disconnect();

    }

    @Override
    public boolean onSetSurface(Surface surface) {
        mLog.d("[onSetSurface][" + surface + "]");
        if (surface == null || mDtvEngine == null) {
            return true;
        }
        mVideoSurface = surface;
        SurfaceBundle bundle = new SurfaceBundle(surface);
        try {
            mDtvEngine.getDtvManager().getDisplayControl().setVideoLayerSurface(DISP_LAYER_VIDEO_ID_0, bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public View onCreateOverlayView() {

        mLog.d("[onCreateOverlayView]");
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mOverlayView = (ViewGroup) inflater.inflate(R.layout.overlay_view, null);
        mImageViewRadio = (ImageView) mOverlayView.findViewById(R.id.imageViewRadio);
        mLog.d("[onCreateOverlayView] view=" + mOverlayView);
        return mOverlayView;
    }

    @Override
    public void onSetStreamVolume(float volume) {
        mLog.d("[onSetStreamVolume][volume: " + volume + "]");
        if (volume == 0.0f) {
            try {
                if (mDtvEngine != null) {
                    mDtvEngine.setMute();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            if (mDtvEngine != null) {
                mDtvEngine.setVolume(volume * 100);
            }
        }
    }

    @Override
    public boolean onTune(Uri channelUri) {
        mLog.d("[onTune][uri: " + channelUri + "]");
        notifyTimeShiftStatusChanged(TvInputManager.TIME_SHIFT_STATUS_UNAVAILABLE);

        resetTracks();
        long id = ContentUris.parseId(channelUri);

        if (mChannelManager == null) {
            if (!initTvManagers()) {
                mLog.e("[onTune][managers not created][uri: " + channelUri + "]");
                return false;
            }
        }

        mCurrentChannel = mChannelManager.getChannelById(id);
        mDtvEngine.setCurrentlyActiveChannel((int)mCurrentChannel.getChannelId());

        if (mCurrentChannel == null) {
            mLog.d("[onTune][channel not fount][uri: " + channelUri + "]");
            mContentIsBlocked = false;
            return false;
        }

        new Thread(new Runnable() {

            @Override
            public void run() {
                notifyContentAllowed();
                startPlayback();
            }
        }).start();
        return true;
    }

    @Override
    public void onSetCaptionEnabled(boolean enabled) {
        mLog.d("[onSetCaptionEnabled]" + "[isSubtitleEnabled: " + mIsSubtitleEnabled + "][enabled: " + enabled + "]");
        mIsSubtitleEnabled = enabled;
    }

    @Override
    public boolean onSelectTrack(int type, String trackId) {
        mLog.d("[onSelectTrack][type: " + type + "][track Id:" + trackId + "]");
        switch (type) {
            case TvTrackInfo.TYPE_SUBTITLE:
                // Subtitle
                return true;
            case TvTrackInfo.TYPE_AUDIO:
                if (mAudioManager == null) {
                    if (!initTvManagers()) {
                        mLog.e("[onSelectTrack][managers not created]");
                        return false;
                    }
                }
                try {
                    mLog.d("[onSelectTrack][settingAudioTrack][routeId="
                            + mRouteManager.getMainLiveRouteId()
                            + " index=" + mTracksIndices.get(trackId) + "]");
                    mAudioManager.setAudioTrack(mRouteManager.getMainLiveRouteId(),
                            mTracksIndices.get(trackId));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                notifyTrackSelected(type, trackId);
                return true;
            case TvTrackInfo.TYPE_VIDEO:
                // This feature is not supported by Comedia MW
        }
        return false;
    }

    @Override
    public void onUnblockContent(TvContentRating rating) {
        mLog.d("[onUnblockContent][rating: " + rating + "]");
        if (mCurrentChannel != null && mContentIsBlocked) {
            mContentIsBlocked = false;
            startPlayback();
        }
    }

    public String getInputID() {
        return mInputID;
    }


    /**
     * Update audio and subtitle tracks information for currently selected
     * channel.
     *
     * @throws RemoteException
     */
    private void updateTracks() throws RemoteException {
        String firstAudioTrack = null;

        if (mDtvEngine == null) {
            if (!initTvManagers()) {
                mLog.e("[updateTracks][managers not created]");
                return;
            }
        }

        synchronized (mTracks) {
            mTracks.clear();
            mTracksIndices.clear();

            if (mCurrentChannel != null) {
                // Audio tracks
                int audioTrackCount = mAudioManager.getTrackCount(mRouteManager.getMainLiveRouteId());
                for (int trackIndex = 0; trackIndex < audioTrackCount; trackIndex++) {
                    AudioTrack audioTrack = mAudioManager.getTrack(mRouteManager.getMainLiveRouteId(), trackIndex);
                    String trackId = mTracks.size()
                            + "_" + audioTrack.getName()
                            + "_" + audioTrack.getLanguage();
                    mTracks.add(new TvTrackInfo.Builder(TvTrackInfo.TYPE_AUDIO, trackId)
                            .setLanguage(audioTrack.getLanguage())
                            .build());
                    mLog.d("[updateTracks][audioTrack]["
                            + " number = " + trackIndex
                            + " AudioTrack = " + audioTrack.toString()
                            + "]");
                    mTracksIndices.put(trackId, audioTrack.getIndex());
                    if (firstAudioTrack == null) {
                        firstAudioTrack = trackId;
                    }
                }
            }
        }
        // Notify tracks update
        notifyTracksChanged(mTracks);
        if (firstAudioTrack != null) {
            notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, firstAudioTrack);
        }
        notifyTrackSelected(TvTrackInfo.TYPE_SUBTITLE, null);
    }

    private boolean startPlayback() {
        if (mCurrentChannel != null) {
            notifyVideoUnavailable(TvInputManager.VIDEO_UNAVAILABLE_REASON_TUNING);
            if (mDtvEngine == null) {
                if (!initTvManagers()) {
                    mLog.e("[startPlayback][managers not created]");
                    return false;
                }
            }
            try {
                mDtvEngine.start(mCurrentChannel);
            } catch (RemoteException e) {
                e.printStackTrace();
                return false;
            }
            mLog.d("[startPlayback] mImageViewRadio SHOW: "
                    + (mCurrentChannel.getServiceType() == ServiceType.DIG_RAD));
            if (mImageViewRadio != null) {
                mImageViewRadio.post(new Runnable() {

                    @Override
                    public void run() {
                        mImageViewRadio.setVisibility(mCurrentChannel.getServiceType() == ServiceType.DIG_RAD
                                ? View.VISIBLE : View.GONE);
                    }
                });
            }
            notifyVideoAvailable();


        } else {
            mLog.e("[startPlayback]Channel is NULL");
        }
        return true;
    }

    private boolean stopPlayback() {
        mLog.d("[stopPlayback]");
        if (mDtvEngine == null) {
            mLog.e("[stopPlayback][managers not created]");
            return false;
        }

        try {
            mDtvEngine.stop();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void resetTracks() {
        mTracks.clear();
        mTracksIndices.clear();
        notifyTracksChanged(mTracks);
        notifyTrackSelected(TvTrackInfo.TYPE_AUDIO, null);
        notifyTrackSelected(TvTrackInfo.TYPE_VIDEO, null);
    }

    /**
     * Interface used for session event reporting
     */
    public interface ITvSession {
        public void onSessionRelease(TvSession session);
    }
}


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

import android.media.tv.TvTrackInfo;
import android.os.RemoteException;

import com.iwedia.dtv.audio.AudioTrack;
import com.iwedia.dtv.audio.IAudioControl;
import com.iwedia.dtv.types.AudioTrackType;
import com.iwedia.tuner.tvinput.TvService;
import com.iwedia.tuner.tvinput.engine.utils.TrackManager;
import com.iwedia.tuner.tvinput.utils.Logger;

/**
 * Manager for handling Teletext, Subtitle and Audio tracks.
 */
public class AudioManager extends TrackManager<AudioTrack> {

    private final Logger mLog = new Logger(
            TvService.APP_NAME + AudioManager.class.getSimpleName(), Logger.ERROR);


    public class AudioTrackInfo {

        public AudioTrackInfo(String id, int index, AudioTrackType type, String language,
                              AudioTrack comediaTrack) {
            this.mId = id;
            this.mIndex = index;
            this.mType = type;
            this.mLanguage = language;
            this.mComediaTrack = comediaTrack;
        }

        public String getId() {
            return mId;
        }

        public int getIndex() {
            return mIndex;
        }

        public AudioTrackType getType() {
            return mType;
        }

        public String getLanguage() {
            return mLanguage;
        }

        public TvTrackInfo getTifInfo() {
            return mTifInfo;
        }

        public void setTifInfo(TvTrackInfo tifInfo) {
            mTifInfo = tifInfo;
        }

        public AudioTrack getComediaTrack() {
            return mComediaTrack;
        }

        // TIF unique track ID
        private String mId;

        // Internal comedia track index
        private int mIndex;

        // Track type
        private AudioTrackType mType;

        private String mLanguage;

        private TvTrackInfo mTifInfo;

        private AudioTrack mComediaTrack;

        @Override
        public String toString() {
            return "[AudioTrackInfo id='" + mId + "' index=" + mIndex + " type=" + mType
                    + " language="
                    + mLanguage + ", comedia=" + mComediaTrack + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof AudioTrackInfo) {
                AudioTrackInfo otherTrack = (AudioTrackInfo) other;

                return otherTrack.mId.equals(mId) && otherTrack.mIndex == mIndex
                        && otherTrack.mLanguage.equals(mLanguage) && otherTrack.mType == mType;
            }

            return false;
        }
    }

    /** Audio control object */
    private IAudioControl mAudioControl;

    /**
     * Constructor
     *
     * @param audioControl Object through which audio control is achieved
     */
    public AudioManager(IAudioControl audioControl) {
        this.mAudioControl = audioControl;
    }


    /**
     * Returns number of audio tracks for current channel.
     * @throws RemoteException 
     */
    @Override
    public int getTrackCount(int routeId) throws RemoteException {
        return mAudioControl.getAudioTrackCount(routeId);
    }

    /**
     * Returns audio track by index.
     * @throws RemoteException 
     */
    @Override
    public AudioTrack getTrack(int routeId, int index) throws RemoteException {
        return mAudioControl.getAudioTrack(routeId, index);
    }

    /**
     * Sets audio track with desired index as active.
     * @throws RemoteException 
     */
    public void setAudioTrack(int routeId, int index) throws RemoteException {
        mAudioControl.setCurrentAudioTrack(routeId, index);
    }

}

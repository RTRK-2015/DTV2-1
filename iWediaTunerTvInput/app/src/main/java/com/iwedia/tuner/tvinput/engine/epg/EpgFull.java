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
package com.iwedia.tuner.tvinput.engine.epg;

import android.content.Context;
import android.net.ParseException;
import android.os.RemoteException;

import com.iwedia.dtv.epg.EpgEvent;
import com.iwedia.dtv.types.TimeDate;
import com.iwedia.tuner.tvinput.TvService;
import com.iwedia.tuner.tvinput.engine.utils.EpgRunnable;
import com.iwedia.tuner.tvinput.utils.Logger;

import java.util.ArrayList;

/**
 * Runnable class for inserting all EPG data into program Database
 */
public class EpgFull extends EpgRunnable {

    /** Object used to write to logcat output */
    private final Logger mLog = new Logger(TvService.APP_NAME + EpgRunnable.class.getSimpleName(),
            Logger.DEBUG);

    public interface IEpgAcquisitionListener {

        public void epgAcquisitionStarted(Long frequency);

        public void epgAcquisitionFinished(Long frequency);
    }

    private IEpgAcquisitionListener mEpgAcquisitionListener;

    /**
     * Constructor
     *
     * @param context Application context
     */
    public EpgFull(Context context) {
        super(context);
        mEpgAcquisitionListener = null;
        mServiceIndex = -1;
        mFrequency = -1L;
    }

    public EpgFull(Context context, IEpgAcquisitionListener listener, int serviceIndex,
                   Long frequency) {
        super(context);
        mEpgAcquisitionListener = listener;
        mServiceIndex = serviceIndex;
        mFrequency = frequency;
    }

    @Override
    public void run() {
        int channelListSize = 0;
        try {
            mDtvManager.getChannelManager().getChannelListSize();
        } catch (RemoteException e1) {
            e1.printStackTrace();
            return;
        }
        mLog.d("[EpgFull] channel list zie:" + channelListSize);
        TimeDate startTime = mDtvManager.getEpgManager().getWindowStartTime();
        TimeDate endTime = mDtvManager.getEpgManager().getWindowEndTime();
        mLog.d("[EpgFull][start time: " + startTime + "]");
        mLog.d("[EpgFull][end time: " + endTime + "]");
        mEpgAcquisitionListener.epgAcquisitionStarted(mFrequency);
        for (int channelIndex = 0; channelIndex < channelListSize; channelIndex++) {
            ArrayList<EpgEvent> events = null;
            try {
                events = mDtvManager.getEpgManager().getEpgEvents(mDtvManager.getChannelManager().getChannelByIndex(channelIndex).getServiceId());
            } catch (ParseException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            if (events != null) {
                try {
                    addPrograms(events, channelIndex);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
        mEpgAcquisitionListener.epgAcquisitionFinished(mFrequency);
    }
}

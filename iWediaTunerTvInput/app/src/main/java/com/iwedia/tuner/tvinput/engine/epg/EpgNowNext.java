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
import android.os.RemoteException;
import android.util.Log;

import com.iwedia.dtv.epg.EpgComponentType;
import com.iwedia.dtv.epg.EpgEvent;
import com.iwedia.dtv.epg.EpgEventType;
import com.iwedia.dtv.epg.IEpgControl;
import com.iwedia.dtv.types.TimeDate;
import com.iwedia.tuner.tvinput.TvService;
import com.iwedia.tuner.tvinput.engine.DtvEngine;
import com.iwedia.tuner.tvinput.engine.utils.EpgRunnable;
import com.iwedia.tuner.tvinput.utils.Logger;

import java.util.Calendar;
import java.util.Date;

/**
 * Runnable class for inserting Now/Next EPG data into program Database
 */
public class EpgNowNext extends EpgRunnable {

    /** Now/Next events origin channel index */
    private final int mChannelIndex;
    private final int mFilterID;

    private static final Logger mLog = new Logger(TvService.APP_NAME + DtvEngine.class.getSimpleName(),
            Logger.ERROR);

    /**
     * Contructor
     *
     * @param context   Application context
     * @param channelIndex Channel id for Now/Next event (MW index)
     */
    public EpgNowNext(Context context, int channelIndex, int filterID) {
        super(context);
        mChannelIndex = channelIndex;
        mFilterID = filterID;
    }

    @Override
    public void run() {
        EpgEvent now;
        EpgEvent next;
        IEpgControl epgControl = null;
        // ODOT: Acquire IEpgControl object
        try {
            epgControl = mDtvManager.getEpgControl();
        } catch (RemoteException e1) {
            e1.printStackTrace();
        }

        // ODOT: Acquire present event from MW (EpgEventType.PRESENT_EVENT)
        // ODOT: Acquire following event from MW (EpgEventType.FOLLOWING_EVENT)
        // ODOT: Add present and following events to DB (addProgram)
        // ODOT: what the hell is filter index
        try {
            mLog.i("Getting now");
            now = epgControl.getPresentFollowingEvent(mFilterID, mChannelIndex, EpgEventType.PRESENT_EVENT);
            addProgram(now, mChannelIndex);
            mLog.i("Getting next");
            next = epgControl.getPresentFollowingEvent(mFilterID, mChannelIndex, EpgEventType.FOLLOWING_EVENT);
            addProgram(next, mChannelIndex);

            // TimeDate startTime,
            // TimeDate endTime,
            // String description,
            // String name,
            // int genre,
            // int subGenre,
            // int serviceIndex,
            // int parentalRate,
            // String language,
            // int eventID,
            // boolean scrambled,
            // int numberOfComponents,
            // EpgComponentType[] componentType
            final int serviceId = mChannelIndex - 3;
            final long shitOffset = 8 * 60 * 60 * 1000L;
            final long second = 1000L;
            final long tenMinutes = 10 * 60 * 1000L;

            TimeDate ts = next.getEndTime();
            int numCustom = 20;
            for (int i = 0; i < numCustom; i++) {
                // TS
                long tsMillis = ts.getCalendar().getTimeInMillis();
                tsMillis += shitOffset;
                tsMillis += second;
                Date tsDate = new Date(tsMillis);
                ts = new TimeDate(tsDate);
                mLog.i("Starting from " + ts.getCalendar().getTime());

                // TE
                tsMillis = ts.getCalendar().getTimeInMillis();
                tsMillis += shitOffset;
                tsMillis += (i + 1) * tenMinutes;
                Date teDate = new Date(tsMillis);
                TimeDate te = new TimeDate(teDate);
                mLog.i("New event ending in " + te.getCalendar().getTime());

                EpgEvent e = new EpgEvent(
                        ts,
                        te,
                        "Mrs_" + serviceId + "_" + i,
                        "Name_" + serviceId + "_" + i,
                        1,
                        1,
                        serviceId,
                        4,
                        "DE",
                        30000 + serviceId * numCustom  + i,
                        false,
                        0,
                        new EpgComponentType[] { }
                );
                mLog.i("Adding custom program");
                addProgram(e, mChannelIndex);

                mLog.i("Reassigning time");
                ts = te;
            }
        } catch (RemoteException e) {
            mLog.e("epgControl.getPFE");
        }
    }
}

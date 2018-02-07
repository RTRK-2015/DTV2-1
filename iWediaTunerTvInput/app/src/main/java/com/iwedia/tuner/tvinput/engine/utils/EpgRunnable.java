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
package com.iwedia.tuner.tvinput.engine.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContentRating;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.RemoteException;

import com.iwedia.dtv.epg.EpgEvent;
import com.iwedia.dtv.types.TimeDate;
import com.iwedia.tuner.tvinput.TvService;
import com.iwedia.tuner.tvinput.data.ChannelDescriptor;
import com.iwedia.tuner.tvinput.data.EpgProgram;
import com.iwedia.tuner.tvinput.engine.ChannelManager;
import com.iwedia.tuner.tvinput.engine.DtvEngine;
import com.iwedia.tuner.tvinput.utils.Logger;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

/**
 * Abstract class that contains mutual methods for EPG runnable classes
 */
public abstract class EpgRunnable implements Runnable {

    /** Object used to write to logcat output */
    private final Logger mLog = new Logger(TvService.APP_NAME + EpgRunnable.class.getSimpleName(),
            99);
    /** Domain used for content rating */
    private static final String DOMAIN = "com.android.tv";
    /** Content rating system */
    private static final String RATING_SYSTEM = "DVB";
    /** Projection for DB filling */
    private static final String[] projection = {
            TvContract.Programs.COLUMN_TITLE
    };
    protected int mServiceIndex;
    protected Long mFrequency;
    /** Application context */
    protected final Context mContext;
    /** DvbManager for accessing middleware API */
    protected DtvEngine mDtvManager;
    /** Channel Manager */
    private ChannelManager mChannelManager;

    /**
     * Contructor
     *
     * @param context   Application context
     */
    protected EpgRunnable(Context context) {
        mContext = context;
        mDtvManager = DtvEngine.getInstance();
        mChannelManager = mDtvManager.getChannelManager();
    }

    /**
     * Convert DVB rating from middleware values to predefined String constants
     *
     * @param rate DVB rate of the current program
     * @return Converted rate to String constant
     */
    public static String convertDVBRating(int rate) {
        switch (rate) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return "DVB_4";
            case 5:
                return "DVB_5";
            case 6:
                return "DVB_6";
            case 7:
                return "DVB_7";
            case 8:
                return "DVB_8";
            case 9:
                return "DVB_9";
            case 10:
                return "DVB_10";
            case 11:
                return "DVB_11";
            case 12:
                return "DVB_12";
            case 13:
                return "DVB_13";
            case 14:
                return "DVB_14";
            case 15:
                return "DVB_15";
            case 16:
                return "DVB_16";
            case 17:
                return "DVB_17";
            case 18:
                return "DVB_18";
            default:
                return "DVB_4";
        }
    }

    /**
     * Convert program genre from middleware values to predifined constants
     *
     * @param genre Genre of the program
     * @return String value of the program
     */
    public static String convertDVBGenre(int genre) {
        switch (genre) {
            case 0x1:
                return "MOVIES";
            case 0x2:
                return "NEWS";
            case 0x3:
                // epg_genre_show_game_show;
                return "GAMING";
            case 0x4:
                return "SPORTS";
            case 0x5:
                return "FAMILY_KIDS";
            case 0x6:
                // epg_genre_music_ballet_dance;
                return "DRAMA";
            case 0x7:
                // epg_genre_arts_culture;
                return "EDUCATION";
            case 0x8:
                // epg_genre_social_political_issues;
                return "NEWS";
            case 0x9:
                return "EDUCATION";
            case 0xA:
                // epg_genre_leisure_hobbies;
                return "TRAVEL";
            default:
                return "ANIMAL_WILDLIFE";
        }
    }

    /**
     * This method is used to conver EPG event to content values needed for DB
     * @param event EPG event
     * @param channelIndex MW channel index
     * @return
     * @throws RemoteException
     */
    private ContentValues makeProgramContentValues(EpgEvent event, int channelIndex) throws RemoteException {
        mLog.d("[makeProgramContentValues] event [" + event + "] channelIndex:" + channelIndex);
        // ODOT: Adjust start and end time since PF events are from 6th December 2012
        // ODOT: Create content rating
        // ODOT: convert genre
        // ODOT: Get extended description from MW
        // ODOT: Create EPG program by using EpgProgram.Builder
        // Required fields are: ChannelId, title, canonical genre, description,
        // long description, start time, end time, content ratings
        final int channelId = channelIndex - 3;

        EpgProgram.Builder builder = new EpgProgram.Builder();
        mLog.i("Setting chanelid " + (channelId) + " for China");
        builder.setChannelId(channelId);
        mLog.i("Setting title " + event.getName() + " for China");
        builder.setTitle(event.getName());
        mLog.i("Setting genre " + convertDVBGenre(event.getGenre()) + " for China");
        builder.setCanonicalGenres(convertDVBGenre(event.getGenre()));
        mLog.i("Setting sdesc " + event.getDescription() + " for China");
        builder.setDescription(event.getDescription());
        String desc = mDtvManager.getEpgManager().getEventExtendedDescription(event.getEventId(), channelId);
        mLog.i("Setting ldesc " + desc + " for China");
        builder.setLongDescription(desc);

        final long nowTime = 1517972400000L;
        final long theirTime = 1354752000000L;
        final long offset = nowTime - theirTime;
        mLog.i("Now time is " + new Date(nowTime));
        mLog.i("Their time is " + new Date(theirTime));

        mLog.i("Shit time calculations");
        long ogStartWTF = event.getStartTime().getCalendar().getTimeInMillis();
        long ogEndWTF = event.getEndTime().getCalendar().getTimeInMillis();
        mLog.i("ogStart " + new Date(ogStartWTF) + " ogEnd " + new Date(ogEndWTF));
        Date now = new Date();
        long startWTF = ogStartWTF + offset;
        long endWTF = ogEndWTF + offset;
        mLog.i("Should start at " + new Date(startWTF) + " and end at " + new Date(endWTF));
        if (checkifExist(channelId, startWTF, endWTF)) {
            mLog.i("Already exists!");
            return null;
        }

        builder.setStartTimeUtcMillis(startWTF);
        builder.setEndTimeUtcMillis(endWTF);

        TvContentRating[] ratings = new TvContentRating[1];
        String rating = convertDVBRating(event.getParentalRate());
        ratings[0] = TvContentRating.createRating(DOMAIN, RATING_SYSTEM, rating);
        builder.setContentRatings(ratings);

        mLog.i("I build for China");
        EpgProgram program = builder.build();

        // ODOT: Return actual value
        mLog.i("I return values for China");
        return program.toContentValues();
    }

    /**
     * This method is used to insert single program
     * @param event EPG event that is beeing added to DB
     * @param channelIndex MW channel index
     * @return
     * @throws RemoteException
     */
    protected boolean addProgram(EpgEvent event, int channelIndex) throws RemoteException {
        // ODOT: Convert program to content values (makeProgramContentValues)
        // ODOT: Insert content values to Tv Provider DB (Table URI: TvContract.Programs.CONTENT_URI)
        mLog.i("Converting to contentvalues");
        ContentValues values = makeProgramContentValues(event, channelIndex);
        if (values == null)
            return false;
        ContentResolver resolver = mContext.getContentResolver();
        Uri uri = TvContract.Programs.CONTENT_URI;
        mLog.i("inserting at Uri " + uri);

        Uri ruri = resolver.insert(uri, values);
        mLog.i("RUri is " + ruri);
        return ruri != null;
    }

    /**
     * This method is used to add multiple programs
     * @param events event list
     * @param channelIndex channel index
     * @throws RemoteException
     */
    protected void addPrograms(ArrayList<EpgEvent> events, int channelIndex) throws RemoteException {
        ArrayList<ContentValues> list = new ArrayList<ContentValues>();
        for (EpgEvent event : events) {
            ContentValues values = makeProgramContentValues(event, channelIndex);
            if (values != null) {
                list.add(values);
            }
        }
        ContentValues array[] = new ContentValues[list.size()];
        list.toArray(array);
        mContext.getContentResolver().bulkInsert(TvContract.Programs.CONTENT_URI, array);
    }

    /**
     * This method is used to check if the current event is already present in the DB
     *
     * @return True if the program is present in the DB, false otherwise
     */
    protected boolean checkifExist(long channelID, long startTime, long endTime) {
        Uri uri = TvContract.buildProgramsUriForChannel(channelID, startTime, endTime);
        Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
        if (cursor == null || cursor.getCount() == 0) {
            cursor.close();
            mLog.w("[checkifExist][item does not exist in DB][[uri: " + uri.toString() + "]");
            return false;
        } else {
            cursor.close();
            return true;
        }
    }
}

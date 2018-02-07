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

import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.tv.TvContract;
import android.media.tv.TvContract.Channels;
import android.net.Uri;
import android.os.RemoteException;


import com.iwedia.dtv.IDTVManager;
import com.iwedia.dtv.route.broadcast.IBroadcastRouteControl;
import com.iwedia.dtv.route.broadcast.RouteFrontendType;
import com.iwedia.dtv.route.broadcast.RouteInstallSettings;
import com.iwedia.dtv.route.broadcast.routemanager.Routes;
import com.iwedia.dtv.scan.IScanCallback;
import com.iwedia.dtv.scan.IScanControl;
import com.iwedia.dtv.scan.Modulation;
import com.iwedia.dtv.scan.Polarization;
import com.iwedia.dtv.scan.FecType;
import com.iwedia.dtv.service.IServiceControl;
import com.iwedia.dtv.service.ServiceDescriptor;
import com.iwedia.dtv.service.SourceType;
import com.iwedia.tuner.tvinput.TvService;
import com.iwedia.tuner.tvinput.data.ChannelDescriptor;
import com.iwedia.tuner.tvinput.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


/**
 * Manager for all channel related operation
 */
public class ChannelManager {

    /**
     * Object used to write to logcat output
     */
    private final Logger mLog = new Logger(TvService.APP_NAME + ChannelManager.class.getSimpleName(), Logger.ERROR);
    /**
     * Dummy IP channel name
     */
    public static final String IP_CHANNEL_NAME = "IP VOD";

    /**
     * DVB Cable VOD channel name
     */
    public static final String DVB_CAB_VOD_CHANNEL_NAME = "DVB-C VOD";
    /**
     * All channels
     */
    private ArrayList<ChannelDescriptor> mAllChannels;
    /**
     * ID of TV Input
     */
    private String mInputId;
    /**
     * Application context
     */
    private Context mContext;

    private int mDvbChannelCounter;
    /**
     * DVB manager, entry point for MW
     */
    private DtvEngine mDtvEngine;

    /**
     * DTV manager
     */
    private IDTVManager mDTVManger;

    /**
     * Route manager
     */
    private RouteManager mRouteManager;

    /**
     * Scan control
     */
    private IScanControl mScanControl;

    /**
     * Broadcast route control
     */
    private IBroadcastRouteControl mBroadcastRouteControl;

    /**
     * Constructor
     *
     * @param context Application context
     * @throws RemoteException
     */
    public ChannelManager(DtvEngine dvbManager, Context context) throws RemoteException {
        mContext = context;
        mDtvEngine = dvbManager;
        mDTVManger = mDtvEngine.getDtvManager();
        mBroadcastRouteControl = mDTVManger.getBroadcastRouteControl();
        mRouteManager = mDtvEngine.getRouteManager();
        mScanControl = mDtvEngine.getDtvManager().getScanControl();
        mInputId = TvContract.buildInputId(new ComponentName(mContext,TvService.class));
        mDvbChannelCounter = 0;

    }

    /**
     * Initialize channel list
     *
     * @throws RemoteException
     */
    public void init() throws RemoteException {
        mLog.v("initialize ChannelManager");
        mAllChannels = loadChannels(mInputId);
        mDvbChannelCounter = mAllChannels.size();
        if (mAllChannels.isEmpty()) {
            mLog.i("[initialize][first time initialization]");

            refreshChannelList();
        }
        print(mAllChannels);
    }

    /**
     * Gets channel by given uri
     *
     * @return
     */
    public ChannelDescriptor getChannelById(long id) {
        mLog.d("[getChannelByUri][" + id + "]");
        for (ChannelDescriptor cd : mAllChannels) {
            if (cd.getChannelId() == id) {
                return cd;
            }
        }
        return null;
    }

    public ChannelDescriptor getChannelByMwIndex(int channelIndex) {
        mLog.d("[getChannelByIndex][" + channelIndex + "]");
        for (int i = 0; i < mAllChannels.size(); i++) {
            if (mAllChannels.get(i).getServiceId() == channelIndex) {
                return mAllChannels.get(i);
            }
        }

        return null;
    }

    private ArrayList<ChannelDescriptor> loadChannels(String inputId) {
        mLog.d("[loadChannels]");
        ArrayList<ChannelDescriptor> ret = new ArrayList<ChannelDescriptor>();
        final String[] projection = {
                Channels._ID,
                Channels.COLUMN_DISPLAY_NAME, Channels.COLUMN_DISPLAY_NUMBER,
                Channels.COLUMN_SERVICE_ID, Channels.COLUMN_TYPE, Channels.COLUMN_SERVICE_TYPE
        };
        Cursor cursor = mContext.getContentResolver().query(
                TvContract.buildChannelsUriForInput(mInputId), projection,
                null, null, null);
        if (cursor == null) {
            return ret;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            ChannelDescriptor cd = new ChannelDescriptor(cursor);
            mLog.d("[loadChannels] index=" + cd.getChannelId() + " info=" + cd);
            ret.add(cd);
            cursor.moveToNext();
        }
        cursor.close();

        return ret;
    }

    /**
     * Inserts channels into TvProvider database
     *
     * @param inputId  this TV input service
     * @param channels to be inserted into a TVProvider database
     */
    private void storeChannels(String inputId, List<ChannelDescriptor> channels) {
        mLog.d("[storeChannels]");
        final String[] projection = {
                TvContract.Channels._ID,
                TvContract.Channels.COLUMN_DISPLAY_NAME,
                TvContract.Channels.COLUMN_DISPLAY_NUMBER,
        };
        Cursor cursor = mContext.getContentResolver().query(
                TvContract.buildChannelsUriForInput(mInputId), projection,
                null, null, null);
        if (cursor == null) {
            mLog.e("[storeChannels][cursor is null");
            return;
        }
        for (ChannelDescriptor channel : channels) {
            Uri retUri = mContext.getContentResolver().insert(
                    TvContract.Channels.CONTENT_URI,
                    channel.getContentValues(inputId));
            if (retUri == null) {
                mLog.e("[storeChannels][error adding channel to the database");
            } else {
                channel.setId(ContentUris.parseId(retUri));
                mLog.i("[storeChannels][add channel][" + channel + "]");
            }
        }
        cursor.close();
    }

    public void refreshChannelList(/*Routes routes*/) throws RemoteException {
        mLog.d("[refreshChannelList]");
        int displayNumber = 1;
        String formattedChannelNumber = "";
        List<ChannelDescriptor> channels = new ArrayList<ChannelDescriptor>();
        IServiceControl serviceControl = mDTVManger.getServiceControl();
        mAllChannels = new ArrayList<ChannelDescriptor>();
        // 1) Delete all channels from TV provider database
        mContext.getContentResolver().delete(
                TvContract.buildChannelsUriForInput(mInputId), null, null);
        mContext.getContentResolver().delete(TvContract.Programs.CONTENT_URI,
                null, null);
        // 2) Add DVB channels founded from scan
        int channelListSize = getChannelListSize();
        // ! Limitation: support only 1 DVB route in this cable
        SourceType type = SourceType.UNDEFINED;
        //if (routes == mDtvEngine.getRouteManager().getCabRoute()) {
        type = SourceType.SAT;
        //}
        for (int i = 0; i < channelListSize; i++) {
            // ! If there is IP first element in service list (use case with
            // Hybrid tuner) it's a DUMMY channel
            ServiceDescriptor servDesc = serviceControl.getServiceDescriptor(
                    DtvEngine.MASTER_LIST_INDEX, i);

            /*
             *  Ignore dummy IP channel and DVB cable VOD channel in master list.
             *  Name for IP channel and DVB cable VOD are hardcoded in Middleware.
             *         In cases when the name change, it's needed to update IWedia input service,
             *         because statement will be incorrect.
             */
            if ((servDesc.getName().contains(IP_CHANNEL_NAME)) ||
                    (servDesc.getName().contains(DVB_CAB_VOD_CHANNEL_NAME))) {
                mLog.d("Skip service [" + servDesc.toString() + "]");
                continue;
            }

            mLog.d("Service [" + i + "] " + servDesc.toString());
            formattedChannelNumber = String.format(Locale.ENGLISH, "%02d",
                    displayNumber);
            channels.add(new ChannelDescriptor(formattedChannelNumber, servDesc
                    .getName(), servDesc.getMasterIndex(), type, servDesc.getServiceType()));
            displayNumber++;
        }
        print(channels);
        // Save channels to TV provider database
        storeChannels(mInputId, channels);
        // Load channels to TIF memory
        mAllChannels = loadChannels(mInputId);
    }

    public ArrayList<ChannelDescriptor> getAllDatabaseChannels() {
        mLog.d("[getAllDatabaseChannels]");
        return mAllChannels;
    }

    public int getDtvChannelListSize(Routes routes) throws RemoteException {
        mLog.d("[getDtvChannelListSize]");
        return mDvbChannelCounter;
    }

    private void print(List<ChannelDescriptor> channels) {
        mLog.d("[print]");
        for (ChannelDescriptor channel : channels) {
            mLog.d(channel.toString());
        }
    }

    // Auto Scan for cab and ter routes
    public void startAutoScan(final IScanCallback callback, SourceType type) throws RemoteException {
        mLog.d("[startAutoScan]");
        if ((type == SourceType.CAB)
                && (mDtvEngine.getRouteManager().getCabRoute().getInstallRoute() != null)) {
            mLog.i("[startScan][Starting scan for cable frontend]");
            RouteInstallSettings settings = new RouteInstallSettings();
            settings.setFrontendType(RouteFrontendType.CAB);
            int cabInstallRoute = mDtvEngine.getRouteManager().getCabRoute().getInstallRouteID();
            mBroadcastRouteControl.configureInstallRoute(cabInstallRoute, settings);
            mScanControl.autoScan(cabInstallRoute);
        }

        if ((type == SourceType.TER)
                && (mDtvEngine.getRouteManager().getCabRoute().getInstallRoute() != null)) {
            mLog.i("[startScan][Starting scan for terrestrial frontend]");
            RouteInstallSettings settings = new RouteInstallSettings();
            settings.setFrontendType(RouteFrontendType.TER);
            int terInstallRoute = mDtvEngine.getRouteManager().getTerRoute().getInstallRouteID();
            mBroadcastRouteControl.configureInstallRoute(terInstallRoute, settings);
            mScanControl.autoScan(terInstallRoute);
        }

        if ((type == SourceType.SAT)
                && (mDtvEngine.getRouteManager().getCabRoute().getInstallRoute() != null)) {
            mLog.i("[startScan][Starting scan for satelite frontend]");
            RouteInstallSettings settings = new RouteInstallSettings();
            settings.setFrontendType(RouteFrontendType.SAT);
            int satInstallRoute = mDtvEngine.getRouteManager().getSatRoute().getInstallRouteID();
            mBroadcastRouteControl.configureInstallRoute(satInstallRoute, settings);
            mScanControl.autoScan(satInstallRoute);
        }
    }


    public void startManualScanTer(int frequency) throws RemoteException {
        mLog.i("[startScan] Started scan for terrestrial frontend!");
        RouteInstallSettings settings = new RouteInstallSettings();
        settings.setFrontendType(RouteFrontendType.TER);
        int terInstallRoute = mDtvEngine.getRouteManager().getTerRoute().getInstallRouteID();
        mBroadcastRouteControl.configureInstallRoute(terInstallRoute, settings);
        mScanControl.setFrequency(frequency * 1000); // in kHz
        mScanControl.manualScan(terInstallRoute);
    }

    public void startManualScanCab(int frequency, String modulation, String symbolRate) throws RemoteException {
        mLog.i("[startScan] Started scan for cable frontend!");
        RouteInstallSettings settings = new RouteInstallSettings();
        settings.setFrontendType(RouteFrontendType.CAB);
        int cabInstallRoute = mDtvEngine.getRouteManager().getCabRoute().getInstallRouteID();
        mBroadcastRouteControl.configureInstallRoute(cabInstallRoute, settings);
        mScanControl.setModulation(Modulation.MODULATION_QAM256);
        mScanControl.setSymbolRate(6900);
        mScanControl.setFrequency(frequency);
        mScanControl.manualScan(cabInstallRoute);
    }

    public void startManualScanSat(int frequency, Modulation modulation, Polarization polarization, int symbolrate, FecType fec) throws RemoteException {
        mLog.i("[startScan] Started scan for satelite frontend!");
        RouteInstallSettings settings = new RouteInstallSettings();
        settings.setFrontendType(RouteFrontendType.SAT);
        int satInstallRoute = mDtvEngine.getRouteManager().getSatRoute().getInstallRouteID();
        mBroadcastRouteControl.configureInstallRoute(satInstallRoute, settings);
        mScanControl.setModulation(modulation);
        mScanControl.setFrequency(frequency);
	mScanControl.setFecType(fec);
        mScanControl.setSymbolRate(symbolrate);
	mScanControl.setPolarization(polarization);
        mScanControl.manualScan(satInstallRoute);
    }

    public void stopScan() throws RemoteException {
        mLog.d("[stopScan]");
        mScanControl.abortScan(mRouteManager.getMainInstallRouteId());
    }

    /**
     * Get Size of Channel List.
     *
     * @throws RemoteException
     */
    public int getChannelListSize() throws RemoteException {
        mLog.d("[getChannelListSize]");
        int serviceCount = mDTVManger.getServiceControl().getServiceListCount(
                DtvEngine.MASTER_LIST_INDEX);
        return serviceCount;
    }

    public ChannelDescriptor getChannelByIndex(int channelIndex) {
        mLog.d("[getChannelByIndex][" + channelIndex + "]");
        try {
            return mAllChannels.get(channelIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

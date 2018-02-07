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

import android.content.Context;
import android.content.IntentFilter;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputService;

import com.iwedia.tuner.tvinput.TvSession.ITvSession;
import com.iwedia.tuner.tvinput.engine.DtvEngine;
import com.iwedia.tuner.tvinput.utils.Logger;

/**
 * Main class for iWedia TV Input Service
 */
public class TvService extends TvInputService implements ITvSession {

    /**
     * App name is used to help with logcat output filtering
     */
    public static final String APP_NAME = "iWediaTvInput_";
    /**
     * Object used to write to logcat output
     */
    private final Logger mLog = new Logger(APP_NAME + TvService.class.getSimpleName(), Logger.ERROR);
    /**
     * DVB manager instance.
     */
    protected DtvEngine mDtvEngine = null;
    /**
     * List of all TVSessions
     */
    private TvSession mCurrentSession = null;

    private Context mContext;

    @Override
    public void onCreate() {
        mLog.d("[onCreateService]");
        super.onCreate();

        mContext = getApplicationContext();

        IntentFilter filter = new IntentFilter();
        filter.addAction(TvInputManager.ACTION_BLOCKED_RATINGS_CHANGED);
        filter.addAction(TvInputManager.ACTION_PARENTAL_CONTROLS_ENABLED_CHANGED);

        Thread mwInitThread = new Thread() {
            @Override
            public void run() {
                super.run();
                // ! blocking call
                DtvEngine.instantiate(TvService.this);
                mDtvEngine = DtvEngine.getInstance();

            }
        };
        mwInitThread.start();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        mLog.d("[onDestroyService]");
        super.onDestroy();

    }

    @Override
    public final Session onCreateSession(String inputId) {
        mLog.d("[onCreateSession][" + inputId + "]");
        TvSession tvSession = new TvSession(this, this, inputId);
        return tvSession;
    }

    @Override
    public void onSessionRelease(TvSession session) {
        mLog.d("[onSessionRelease]");
        mDtvEngine.deinit();
    }
}

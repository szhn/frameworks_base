/*
 * Copyright (C) 2014-2015 The MoKee OpenSource Project
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

package com.android.systemui.titanium.carrierlabel;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.internal.util.titanium.TitaniumUtils;
import com.android.internal.telephony.TelephonyIntents;

import com.android.systemui.Dependency;
import com.android.systemui.titanium.carrierlabel.SpnOverride;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.plugins.DarkIconDispatcher.DarkReceiver;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import com.android.systemui.R;

public class CarrierLabel extends TextView implements DarkReceiver {

    private Context mContext;
    private boolean mAttached;
    private static boolean isCN;
    private int mCarrierFontSize = 14;
    private int mCarrierColor = 0xffffffff;
    private int mTintColor = Color.WHITE;

    private int mCarrierLabelFontStyle = FONT_NORMAL;
    public static final int FONT_NORMAL = 0;
    public static final int FONT_ITALIC = 1;
    public static final int FONT_BOLD = 2;
    public static final int ANTIPASTOPRO = 3;
    public static final int ARTUBUSSOURCE = 4;
    public static final int ARVOLATO = 5;
    public static final int BARIOLSOURCE = 6;
    public static final int CAGLIOSTROSOURCE = 7;
    public static final int CIRCULARSTD = 8;
    public static final int COMFORTAA = 9;
    public static final int EVOLVESANS = 10;
    public static final int EXOTWO = 11;
    public static final int FIRASANS = 12;
    public static final int FUCEK = 13;
    public static final int GOBOLD_LIGHT_SYS = 14;
    public static final int GOOGLESANSMEDIUM = 15;
    public static final int LEMONMILK = 16;
    public static final int NOKIAPURE = 17;
    public static final int QUANDO = 18;
    public static final int REEMKUFI = 19;
    public static final int ROSEMARYSOURCE = 20;
    public static final int RUBIKRUBIK = 21;
    public static final int SAMSUNGONE = 22;
    public static final int SIMPLEDAY = 23;
    public static final int SLATEFORONEPLUS = 24;
    public static final int UBUNTU = 25;

    Handler mHandler;

    class SettingsObserver extends ContentObserver {
        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_CARRIER_COLOR), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_CARRIER_FONT_SIZE), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.STATUS_BAR_CARRIER_FONT_STYLE), false, this);
        }

        @Override
        public void onChange(boolean selfChange) {
	     updateColor();
	     updateSize();
	     updateStyle();
        }
    }

    public CarrierLabel(Context context) {
        this(context, null);
    }

    public CarrierLabel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CarrierLabel(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        updateNetworkName(true, null, false, null);
        /* Force carrier label to the lockscreen. This helps us avoid
        the carrier label on the statusbar if for whatever reason
        the user changes notch overlays */
        if (TitaniumUtils.hasNotch(mContext)) {
            Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.STATUS_BAR_SHOW_CARRIER, 1);
        }
        mHandler = new Handler();
        SettingsObserver settingsObserver = new SettingsObserver(mHandler);
        settingsObserver.observe();
        updateColor();
        updateSize();
        updateStyle();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(DarkIconDispatcher.class).addDarkReceiver(this);
        if (!mAttached) {
            mAttached = true;
            IntentFilter filter = new IntentFilter();
            filter.addAction(TelephonyIntents.SPN_STRINGS_UPDATED_ACTION);
            filter.addAction(Intent.ACTION_CUSTOM_CARRIER_LABEL_CHANGED);
            mContext.registerReceiver(mIntentReceiver, filter, null, getHandler());
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(DarkIconDispatcher.class).removeDarkReceiver(this);
        if (mAttached) {
            mContext.unregisterReceiver(mIntentReceiver);
            mAttached = false;
        }
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        mTintColor = DarkIconDispatcher.getTint(area, this, tint);
        if (mCarrierColor == 0xFFFFFFFF) {
            setTextColor(mTintColor);
        } else {
            setTextColor(mCarrierColor);
        }
    }

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (TelephonyIntents.SPN_STRINGS_UPDATED_ACTION.equals(action)
                    || Intent.ACTION_CUSTOM_CARRIER_LABEL_CHANGED.equals(action)) {
                        updateNetworkName(intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_SPN, true),
                        intent.getStringExtra(TelephonyIntents.EXTRA_SPN),
                        intent.getBooleanExtra(TelephonyIntents.EXTRA_SHOW_PLMN, false),
                        intent.getStringExtra(TelephonyIntents.EXTRA_PLMN));
                isCN = TitaniumUtils.isChineseLanguage();
            }
        }
    };

    void updateNetworkName(boolean showSpn, String spn, boolean showPlmn, String plmn) {
        final String str;
        final boolean plmnValid = showPlmn && !TextUtils.isEmpty(plmn);
        final boolean spnValid = showSpn && !TextUtils.isEmpty(spn);
        if (spnValid) {
            str = spn;
        } else if (plmnValid) {
            str = plmn;
        } else {
            str = "";
        }
        String customCarrierLabel = Settings.System.getStringForUser(mContext.getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL, UserHandle.USER_CURRENT);
        if (!TextUtils.isEmpty(customCarrierLabel)) {
            setText(customCarrierLabel);
        } else {
            setText(TextUtils.isEmpty(str) ? getOperatorName() : str);
        }
    }

    private String getOperatorName() {
        String operatorName = getContext().getString(R.string.quick_settings_wifi_no_network);
        TelephonyManager telephonyManager = (TelephonyManager) getContext().getSystemService(
                Context.TELEPHONY_SERVICE);
        if (isCN) {
            String operator = telephonyManager.getNetworkOperator();
            if (TextUtils.isEmpty(operator)) {
                operator = telephonyManager.getSimOperator();
            }
            SpnOverride mSpnOverride = new SpnOverride();
            operatorName = mSpnOverride.getSpn(operator);
        } else {
            operatorName = telephonyManager.getNetworkOperatorName();
        }
        if (TextUtils.isEmpty(operatorName)) {
            operatorName = telephonyManager.getSimOperatorName();
        }
        return operatorName;
    }

    public void getFontStyle(int font) {
        switch (font) {
            case FONT_NORMAL:
            default:
                setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
                break;
            case FONT_ITALIC:
                setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                break;
            case FONT_BOLD:
                setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
                break;
            case ANTIPASTOPRO:
                setTypeface(Typeface.create("antipastopro", Typeface.NORMAL));
                break;
            case ARTUBUSSOURCE:
                setTypeface(Typeface.create("arbutussource", Typeface.NORMAL));
                break;
            case ARVOLATO:
                setTypeface(Typeface.create("arvolato", Typeface.NORMAL));
                break;
            case BARIOLSOURCE:
                setTypeface(Typeface.create("bariolsource", Typeface.NORMAL));
                break;
            case CAGLIOSTROSOURCE:
                setTypeface(Typeface.create("cagliostrosource", Typeface.NORMAL));
                break;
            case CIRCULARSTD:
                setTypeface(Typeface.create("circularstd", Typeface.NORMAL));
                break;
            case COMFORTAA:
                setTypeface(Typeface.create("comfortaa", Typeface.NORMAL));
                break;
            case EVOLVESANS:
                setTypeface(Typeface.create("evolvesans", Typeface.NORMAL));
                break;
            case EXOTWO:
                setTypeface(Typeface.create("exotwo", Typeface.NORMAL));
                break;
            case FIRASANS:
                setTypeface(Typeface.create("firasans", Typeface.NORMAL));
                break;
            case FUCEK:
                setTypeface(Typeface.create("fucek", Typeface.NORMAL));
                break;
            case GOBOLD_LIGHT_SYS:
                setTypeface(Typeface.create("gobold-light-sys", Typeface.NORMAL));
                break;
            case GOOGLESANSMEDIUM:
                setTypeface(Typeface.create("googlesansmedium", Typeface.NORMAL));
                break;
            case LEMONMILK:
                setTypeface(Typeface.create("lemonmilk", Typeface.NORMAL));
                break;
            case NOKIAPURE:
                setTypeface(Typeface.create("nokiapure", Typeface.NORMAL));
                break;
            case QUANDO:
                setTypeface(Typeface.create("quando", Typeface.NORMAL));
                break;
            case REEMKUFI:
                setTypeface(Typeface.create("reemkufi", Typeface.NORMAL));
                break;
            case ROSEMARYSOURCE:
                setTypeface(Typeface.create("rosemarysource", Typeface.NORMAL));
                break;
            case RUBIKRUBIK:
                setTypeface(Typeface.create("rubikrubik", Typeface.NORMAL));
                break;
            case SAMSUNGONE:
                setTypeface(Typeface.create("samsungone", Typeface.NORMAL));
                break;
            case SIMPLEDAY:
                setTypeface(Typeface.create("simpleday", Typeface.NORMAL));
                break;
            case SLATEFORONEPLUS:
                setTypeface(Typeface.create("slateforoneplus", Typeface.NORMAL));
                break;
            case UBUNTU:
                setTypeface(Typeface.create("ubuntu", Typeface.NORMAL));
                break;
        }
    }

    private void updateColor() {
        mCarrierColor = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER_COLOR, 0xffffffff);
        if (mCarrierColor == 0xFFFFFFFF) {
            setTextColor(mTintColor);
        } else {
            setTextColor(mCarrierColor);
        }
    }

    private void updateSize() {
        mCarrierFontSize = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER_FONT_SIZE, 14);
        setTextSize(mCarrierFontSize);
    }

    private void updateStyle() {
        mCarrierLabelFontStyle = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.STATUS_BAR_CARRIER_FONT_STYLE, FONT_NORMAL);
        getFontStyle(mCarrierLabelFontStyle);
    }
}

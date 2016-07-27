/*
 SWService.java
 Copyright (c) 2014 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.sw;

import android.bluetooth.BluetoothDevice;

import org.deviceconnect.android.deviceplugin.bluetooth.BluetoothDeviceManager;
import org.deviceconnect.android.deviceplugin.sw.profile.SWKeyEventProfile;
import org.deviceconnect.android.deviceplugin.sw.profile.SWSystemProfile;
import org.deviceconnect.android.deviceplugin.sw.profile.SWTouchProfile;
import org.deviceconnect.android.deviceplugin.sw.service.SWService;
import org.deviceconnect.android.deviceplugin.sw.service.SWServiceFactory;
import org.deviceconnect.android.event.EventManager;
import org.deviceconnect.android.event.cache.MemoryCacheController;
import org.deviceconnect.android.message.DConnectMessageService;
import org.deviceconnect.android.profile.KeyEventProfile;
import org.deviceconnect.android.profile.SystemProfile;
import org.deviceconnect.android.profile.TouchProfile;
import org.deviceconnect.android.service.DConnectService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * 本デバイスプラグインのプロファイルをDeviceConnectに登録するサービス.
 */
public class SWDeviceService extends DConnectMessageService {

    private final Logger mLogger = Logger.getLogger(SWConstants.LOGGER_NAME);

    private BluetoothDeviceManager mDeviceMgr;

    private final BluetoothDeviceManager.DeviceListener mDeviceListener
        = new BluetoothDeviceManager.DeviceListener() {
        @Override
        public void onFound(final BluetoothDevice smartWatch) {
            mLogger.info("onFound: name = " + smartWatch.getName());

            DConnectService service = getService(smartWatch);
            if (service == null) {
                service = SWServiceFactory.createService(smartWatch);
                getServiceProvider().addService(service);
            }
        }

        @Override
        public void onConnected(final BluetoothDevice smartWatch) {
            mLogger.info("onConnected: name = " + smartWatch.getName());
            // 接続状態は「スマートコネクト」アプリのデータベースから別途取得するため、
            // ここでは何もしない.
        }

        @Override
        public void onDisconnected(final BluetoothDevice smartWatch) {
            mLogger.info("onDisconnected: name = " + smartWatch.getName());
            // 接続状態は「スマートコネクト」アプリのデータベースから別途取得するため、
            // ここでは何もしない.
        }
    };

    private DConnectService getService(final BluetoothDevice device) {
        return getServiceProvider().getService(SWService.createServiceId(device));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        EventManager.INSTANCE.setController(new MemoryCacheController());

        mDeviceMgr = new SWDeviceManager(this);
        mDeviceMgr.addDeviceListener(mDeviceListener);
        mDeviceMgr.start();
        for (BluetoothDevice smartWatch : mDeviceMgr.getCachedDeviceList()) {
            DConnectService service = SWServiceFactory.createService(smartWatch);
            getServiceProvider().addService(service);
        }
    }

    @Override
    public void onDestroy() {
        mDeviceMgr.removeDeviceListener(mDeviceListener);
        mDeviceMgr.stop();

        super.onDestroy();
    }

    @Override
    protected void onManagerUninstalled() {
        // Managerアンインストール検知時の処理。
        mLogger.info("Plug-in : onManagerUninstalled");
    }

    @Override
    protected void onManagerTerminated() {
        // Manager正常終了通知受信時の処理。
        mLogger.info("Plug-in : onManagerTerminated");
    }

    @Override
    protected void onManagerEventTransmitDisconnected(String sessionKey) {
        // ManagerのEvent送信経路切断通知受信時の処理。
        mLogger.info("Plug-in : onManagerEventTransmitDisconnected");
        if (sessionKey != null) {
            EventManager.INSTANCE.removeEvents(sessionKey);
        } else {
            EventManager.INSTANCE.removeAll();
        }
    }

    @Override
    protected void onDevicePluginReset() {
        // Device Plug-inへのReset要求受信時の処理。
        mLogger.info("Plug-in : onDevicePluginReset");
        resetPluginResource();
    }

    /**
     * リソースリセット処理.
     */
    private void resetPluginResource() {
        /** 全イベント削除. */
        EventManager.INSTANCE.removeAll();

        /** KeyEvent イベント 解放. */
        List<SWKeyEventProfile> keyeventProfiles = new ArrayList<SWKeyEventProfile>();
        for (DConnectService service : getServiceProvider().getServiceList()) {
            SWKeyEventProfile profile = (SWKeyEventProfile) service.getProfile(KeyEventProfile.PROFILE_NAME);
            if (profile != null && !keyeventProfiles.contains(profile)) {
                keyeventProfiles.add(profile);
            }
        }
        for (SWKeyEventProfile profile : keyeventProfiles) {
            profile.releaseKeyEvent();
        }

        /** Touch イベント 解放. */
        List<SWTouchProfile> touchProfiles = new ArrayList<SWTouchProfile>();
        for (DConnectService service : getServiceProvider().getServiceList()) {
            SWTouchProfile profile = (SWTouchProfile) service.getProfile(TouchProfile.PROFILE_NAME);
            if (profile != null && !touchProfiles.contains(profile)) {
                touchProfiles.add(profile);
            }
        }
        for (SWTouchProfile profile : touchProfiles) {
            profile.releaseTouchEvent();
        }
    }

    @Override
    protected SystemProfile getSystemProfile() {
        return new SWSystemProfile();
    }


}

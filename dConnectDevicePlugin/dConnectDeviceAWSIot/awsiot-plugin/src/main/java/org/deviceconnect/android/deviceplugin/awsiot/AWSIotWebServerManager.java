package org.deviceconnect.android.deviceplugin.awsiot;

import android.content.Context;
import android.util.Log;

import org.deviceconnect.android.deviceplugin.awsiot.core.RemoteDeviceConnectManager;
import org.deviceconnect.android.deviceplugin.awsiot.p2p.WebServer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AWSIotWebServerManager {
    private static final boolean DEBUG = true;
    private static final String TAG = "AWS-Remote";

    private Map<RemoteDeviceConnectManager, WebServer> mWebServerList = new ConcurrentHashMap<>();

    private AWSIotRemoteManager mIot;
    private Context mContext;

    public AWSIotWebServerManager(final Context context, final AWSIotRemoteManager ctl) {
        mContext = context;
        mIot = ctl;
    }

    public void destroy() {
        for (Map.Entry<RemoteDeviceConnectManager, WebServer> e : mWebServerList.entrySet()) {
            e.getValue().stop();
        }
        mWebServerList.clear();
    }

    public String createWebServer(final RemoteDeviceConnectManager remote, final String address, final String path) {
        if (DEBUG) {
            Log.i(TAG, "createWebServer: " + remote);
        }

        // TODO WebServerをどのタイミングで止めるか検討
        WebServer webServer = new WebServer(mContext, address) {
            @Override
            public void onNotifySignaling(final String signaling) {
                mIot.publish(remote, mIot.createP2P(signaling));
            }
            @Override
            protected void onConnected() {
                if (DEBUG) {
                    Log.i(TAG, "WebServer#onConnected");
                }
            }
            @Override
            protected void onDisconnected() {
                if (DEBUG) {
                    Log.i(TAG, "WebServer#onDisconnected");
                }
                mWebServerList.remove(remote);
            }
        };
        webServer.setPath(path);
        String url = webServer.start();
        if (url != null) {
            mWebServerList.put(remote, webServer);
        }

        if (DEBUG) {
            Log.i(TAG, "url=" + url);
        }

        return url;
    }

    public void deleteWebServer(final RemoteDeviceConnectManager remote) {
        WebServer webServer = mWebServerList.remove(remote);
        if (webServer != null) {
            webServer.stop();
        }
    }

    public void onReceivedSignaling(final RemoteDeviceConnectManager remote, final String message) {
        WebServer webServer = mWebServerList.get(remote);
        if (webServer != null) {
            webServer.onReceivedSignaling(message);
        }
    }
}
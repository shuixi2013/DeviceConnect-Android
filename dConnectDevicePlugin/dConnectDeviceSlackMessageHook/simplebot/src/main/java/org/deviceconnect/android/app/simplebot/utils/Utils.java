/*
 Utils.java
 Copyright (c) 2016 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.app.simplebot.utils;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.WindowManager;
import android.widget.EditText;

import org.deviceconnect.android.app.simplebot.R;
import org.deviceconnect.android.app.simplebot.data.SettingData;
import org.deviceconnect.profile.AuthorizationProfileConstants;
import org.deviceconnect.profile.ServiceDiscoveryProfileConstants;
import org.deviceconnect.profile.ServiceInformationProfileConstants;
import org.deviceconnect.profile.SystemProfileConstants;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ユーティリティクラス
 */
public class Utils {

    //---------------------------------------------------------------------------------------
    //region Etc.

    /**
     * 画面遷移
     * @param fragment Fragment
     * @param manager FragmentManager
     */
    public static void transition(Fragment fragment, FragmentManager manager, boolean backStack) {
        FragmentTransaction transaction = manager.beginTransaction();
        String name = fragment.getClass().getName();
        transaction.replace(R.id.container, fragment, name);
        if (backStack){
            transaction.addToBackStack(name);
        }
        transaction.commit();
    }

    /**
     * MapからJsonへ変換する
     * @param map Map
     * @return Json
     */
    public static String mapToJson(Map map) {
        if (map == null) {
            return null;
        }
        return new JSONObject(map).toString();
    }

    /**
     * JsonからMapへ変換する
     * @param json Json
     * @return Map
     */
    public static Map<String, String> jsonToMap(String json) {
        if (json == null) {
            return null;
        }
        try {
            JSONObject jsonObj = new JSONObject(json);
            Iterator<String> keys = jsonObj.keys();
            // 今回は文字列限定
            Map<String, String> params = new HashMap<>();
            while (keys.hasNext()) {
                String key = keys.next();
                String val = jsonObj.getString(key);
                params.put(key, val);
            }
            return params;
        } catch (JSONException e) {
            return null;
        }
    }



    //endregion
    //---------------------------------------------------------------------------------------
    //region Dialog

    /**
     * プログレスダイアログを表示
     * @param context Context
     * @return ダイアログ
     */
    public static ProgressDialog showProgressDialog(Context context) {
        ProgressDialog dialog = new ProgressDialog(context);
        dialog.setMessage("Please wait...");
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        return dialog;
    }

    /**
     * エラーダイアログ表示
     * @param context Context
     * @param e Exception
     */
    public static void showErrorDialog(Context context, Exception e) {
        String msg;
        if (e != null) {
            if (e.getClass().equals(DConnectHelper.DConnectInvalidResultException.class)) {
                msg = context.getString(R.string.err_server_res);
            } else {
                msg = e.toString();
            }
        } else {
            msg = context.getString(R.string.err_occurred);
        }
        new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.err))
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * 警告ダイアログ表示
     * @param context Context
     * @param msg メッセージ
     */
    public static void showAlertDialog(Context context, String msg) {
        new AlertDialog.Builder(context)
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * 確認ダイアログ表示
     * @param context Context
     * @param title タイトル
     * @param msg メッセージ
     * @param listener OKボタンイベントリスナー
     */
    public static void showConfirmDialog(Context context, String title, String msg, DialogInterface.OnClickListener listener) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("OK", listener)
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * 入力ダイアログ表示
     * @param context Context
     * @param title タイトル
     * @param text 初期表示テキスト
     * @param inputType InputType
     * @param callback Callback
     */
    public static void showInputDialog(Context context, String title, String text, int inputType, final DConnectHelper.FinishCallback<String> callback) {
        final EditText editView = new EditText(context);
        editView.setInputType(inputType);
        editView.setText(text);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(editView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null) {
                            callback.onFinish(editView.getText().toString(), null);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    /**
     * 選択ダイアログ表示
     * @param context Context
     * @param title タイトル
     * @param items アイテム
     * @param callback Callback
     */
    public static void showSelectDialog(Context context, String title, String[] items, final DConnectHelper.FinishCallback<Integer> callback) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null) {
                            callback.onFinish(which, null);
                        }
                    }
                })
                .show();
    }


    //endregion
    //---------------------------------------------------------------------------------------
    //region Connection

    /**
     * 接続処理
     * @param context context
     * @param callback 終了コールバック
     */
    public static void connect(final Context context, final DConnectHelper.FinishCallback<DConnectHelper.AuthInfo> callback) {
        final String origin = context.getPackageName();
        String appName = context.getString(R.string.app_name);
        final SettingData setting = SettingData.getInstance(context);
        if (setting.scopes == null) {
            // 初期スコープ
            setting.scopes = new HashSet<>();
            setting.scopes.add(SystemProfileConstants.PROFILE_NAME);
            setting.scopes.add(AuthorizationProfileConstants.PROFILE_NAME);
            setting.scopes.add(ServiceDiscoveryProfileConstants.PROFILE_NAME);
            setting.scopes.add(ServiceInformationProfileConstants.PROFILE_NAME);
            setting.scopes.add("messageHook");
            setting.save();
        }
        String scopes[] = setting.scopes.toArray(new String[0]);

        // 接続先設定
        DConnectHelper.INSTANCE.setHostInfo(
                setting.ssl,
                setting.host,
                setting.port,
                origin
        );

        if (setting.accessToken == null) {
            // 新規接続
            // 認証
            DConnectHelper.INSTANCE.auth(appName, setting.clientId, scopes, new DConnectHelper.FinishCallback<DConnectHelper.AuthInfo>() {
                @Override
                public void onFinish(DConnectHelper.AuthInfo authInfo, Exception error) {
                    if (authInfo != null) {
                        // 設定に保存
                        setting.accessToken = authInfo.accessToken;
                        setting.clientId = authInfo.clientId;
                    } else {
                        // 失敗したらクリア
                        setting.accessToken = null;
                        setting.clientId = null;
                    }
                    setting.save();
                    callback.onFinish(authInfo, error);
                }
            });
        } else {
            // 接続済み情報あり
            callback.onFinish(new DConnectHelper.AuthInfo(setting.clientId, setting.accessToken), null);
        }
    }


    /**
     * サービス一覧を取得
     * @param context context
     * @param callback 終了コールバック
     */
    public static void fetchServices(final Context context, final DConnectHelper.FinishCallback<List<DConnectHelper.ServiceInfo>> callback) {
        DConnectHelper.FinishCallback<DConnectHelper.AuthInfo> finishCallback = new DConnectHelper.FinishCallback<DConnectHelper.AuthInfo>() {
            @Override
            public void onFinish(DConnectHelper.AuthInfo authInfo, Exception error) {
                if (error == null) {
                    // サービス検索
                    DConnectHelper.INSTANCE.serviceDiscovery(authInfo.accessToken, new DConnectHelper.FinishCallback<List<DConnectHelper.ServiceInfo>>() {
                        @Override
                        public void onFinish(List<DConnectHelper.ServiceInfo> serviceInfos, Exception error) {
                            // TODO: エラーの種類によっては再接続
                            callback.onFinish(serviceInfos, error);
                        }
                    });
                } else {
                    // TODO: エラー処理 ループするので、エラー内容によって接続しなおすかメッセージ表示かを切り替える
                    Log.e("a", "err", error);
                    callback.onFinish(null, error);
                    // 設定をクリア
//                    setting.accessToken = null;
//                    setting.clientId = null;
//                    setting.save();
//                    // 再接続
//                    Utils.connect(context, this);
                }
            }
        };
        Utils.connect(context, finishCallback);
    }

    /**
     * イベントを登録
     * @param context context
     * @param callback 終了コールバック
     */
    public static void registEvent(final Context context, final DConnectHelper.FinishCallback<Void> callback) {
        DConnectHelper.FinishCallback<DConnectHelper.AuthInfo> finishCallback = new DConnectHelper.FinishCallback<DConnectHelper.AuthInfo>() {
            @Override
            public void onFinish(DConnectHelper.AuthInfo authInfo, Exception error) {
                if (error == null) {
                    // 登録
                    SettingData setting = SettingData.getInstance(context);
                    DConnectHelper.INSTANCE.registerEvent("messageHook", "message", setting.serviceId, setting.accessToken, setting.clientId, new DConnectHelper.FinishCallback<Void>() {
                        @Override
                        public void onFinish(Void aVoid, Exception error) {
                            if (error == null) {
                                // WebSocket接続
                                SettingData setting = SettingData.getInstance(context);
                                DConnectHelper.INSTANCE.openWebsocket(setting.clientId);
                                callback.onFinish(null, null);
                            } else {
                                // TODO: エラーの種類によっては再接続
                                callback.onFinish(null, error);
                            }
                        }
                    });
                } else {
                    // TODO: エラー処理 ループするので、エラー内容によって接続しなおすかメッセージ表示かを切り替える
                    Log.e("a", "err", error);
                    callback.onFinish(null, error);
                    // 設定をクリア
//                    setting.accessToken = null;
//                    setting.clientId = null;
//                    setting.save();
//                    // 再接続
//                    Utils.connect(context, this);
                }
            }
        };
        Utils.connect(context, finishCallback);
    }

    /**
     * メッセージ送信
     * @param context context
     * @param callback 終了コールバック
     */
    public static void sendMessage(final Context context, final String channel, final String text, final DConnectHelper.FinishCallback<Void> callback) {
        DConnectHelper.FinishCallback<DConnectHelper.AuthInfo> finishCallback = new DConnectHelper.FinishCallback<DConnectHelper.AuthInfo>() {
            @Override
            public void onFinish(DConnectHelper.AuthInfo authInfo, Exception error) {
                if (error == null) {
                    // メッセージ送信
                    SettingData setting = SettingData.getInstance(context);
                    DConnectHelper.INSTANCE.sendMessage(setting.serviceId, setting.accessToken, channel, text, new DConnectHelper.FinishCallback<Void>() {
                        @Override
                        public void onFinish(Void aVoid, Exception error) {
                            // TODO:
                            callback.onFinish(null, error);
                        }
                    });
                } else {
                    // TODO: エラー処理 ループするので、エラー内容によって接続しなおすかメッセージ表示かを切り替える
                    Log.e("a", "err", error);
                    callback.onFinish(null, error);
                    // 設定をクリア
//                    setting.accessToken = null;
//                    setting.clientId = null;
//                    setting.save();
//                    // 再接続
//                    Utils.connect(context, this);
                }
            }
        };
        Utils.connect(context, finishCallback);
    }

    /**
     * リクエスト送信
     * @param context context
     * @param method メソッド
     * @param path パス
     * @param params パラメータ
     * @param callback 終了コールバック
     */
    public static void sendRequest(final Context context, final String method, final String path, final String serviceId, final Map<String, String> params, final DConnectHelper.FinishCallback<Map<String, Object>> callback) {
        final DConnectHelper.FinishCallback<DConnectHelper.AuthInfo> finishCallback = new DConnectHelper.FinishCallback<DConnectHelper.AuthInfo>() {
            @Override
            public void onFinish(DConnectHelper.AuthInfo authInfo, Exception error) {
                if (error == null) {
                    // リクエスト送信
                    SettingData setting = SettingData.getInstance(context);
                    DConnectHelper.INSTANCE.sendRequest(method, path, serviceId, setting.accessToken, params, new DConnectHelper.FinishCallback<Map<String, Object>>() {
                        @Override
                        public void onFinish(Map<String, Object> stringObjectMap, Exception error) {
                            callback.onFinish(stringObjectMap, error);
                        }
                    });
                } else {
                    // TODO: エラー処理 ループするので、エラー内容によって接続しなおすかメッセージ表示かを切り替える
                    Log.e("a", "err", error);
                    callback.onFinish(null, error);
                    // 設定をクリア
//                    setting.accessToken = null;
//                    setting.clientId = null;
//                    setting.save();
//                    // 再接続
//                    Utils.connect(context, this);
                }
            }
        };
        Utils.connect(context, finishCallback);
    }

    //endregion
    //---------------------------------------------------------------------------------------
}

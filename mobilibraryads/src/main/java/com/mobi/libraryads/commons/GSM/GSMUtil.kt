@file:Suppress("NULLABILITY_MISMATCH_BASED_ON_EXPLICIT_TYPE_ARGUMENTS_FOR_JAVA")

package com.mobi.libraryads.commons.GSM

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ANDROID_ID
import android.util.Log
import com.mobi.libraryads.BuildConfig
import com.google.gson.JsonObject
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object GSMUtil {
    const val DOMAIN_GSM_PROD: String =  /*""*/"https://gsm.mobicorp.com"
    const val DOMAIN_GSM_DEV: String =  /*""*/"https://gsmdev.mobicorp.com"
    var retryLoginGSM: Int = 0
    var accessTokenGSM: String = ""

    var DOMAIN_GSM_TRACKING: String = "https://gsmdev.mobicorp.com"
    var DOMAIN_GSM_LOGIN: String = "https://gsmdev.mobicorp.com"


    @SuppressLint("HardwareIds")
    fun getDeviceID(context: Context): String =
        Settings.Secure.getString(context.contentResolver, ANDROID_ID)

    fun login(
        context: Context,
        gsmAppId: String,
        version: String,
        loginGSMCallback: LoginGSMCallback? = null
    ) {
        val params = JsonObject()
        params.addProperty("appId", gsmAppId)
        params.addProperty(
            "deviceId",
            getDeviceID(context)
        )
        params.addProperty("pkName", context.packageName)
        params.addProperty("os", 1) // 1 = android, 2 = ios
        params.addProperty("version", version)

        GSMClient.getClient(DOMAIN_GSM_LOGIN)
            .create<ApiGSM>(ApiGSM::class.java)
            .login(params)
            .enqueue(object : Callback<JsonObject?> {
                override fun onResponse(
                    call: Call<JsonObject?>,
                    response: Response<JsonObject?>
                ) {
                    if (response.isSuccessful) {
                        retryLoginGSM = 0
                        try {
                            if (response.body() != null && JSONObject(
                                    response.body().toString()
                                ).has("accessToken")
                            ) {
                                val obj = JSONObject(response.body().toString())
                                accessTokenGSM = obj.getString("accessToken")
                                loginGSMCallback?.loginSuccess(accessTokenGSM)
                            }
                        } catch (_: JSONException) {
                        }
                    } else {
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                }
            })
    }


    fun getConfig(context: Context, gsmAppId: String) {
        GSMClient.getClient(if (BuildConfig.DEBUG) DOMAIN_GSM_DEV else DOMAIN_GSM_PROD)
            .create<ApiGSM>(ApiGSM::class.java)
            .getConfig(gsmAppId, getDeviceID(context), 1)
            .enqueue(object : Callback<JsonObject?> {
                override fun onResponse(
                    call: Call<JsonObject?>,
                    response: Response<JsonObject?>
                ) {
                    if (response.isSuccessful) {
                        try {
                            if (response.body() != null && JSONObject(
                                    response.body().toString()
                                ).has("domains")
                            ) {
                                val obj = JSONObject(response.body().toString())
                                val domains = obj.getString("domains")
                                DOMAIN_GSM_LOGIN = JSONObject(domains).getString("sso")
                                DOMAIN_GSM_TRACKING = JSONObject(domains).getString("gsmAnalytic")
                            }
                        } catch (_: JSONException) {
                        }
                    } else {
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                }
            })
    }


    fun checkKeyHash(
        context: Context,
        gsmAppId: String,
        keyHash: String,
        result: (Boolean) -> Unit
    ) {

        GSMClient.getClient(if (BuildConfig.DEBUG) DOMAIN_GSM_DEV else DOMAIN_GSM_PROD)
            .create<ApiGSM>(ApiGSM::class.java)
            .checkKeyHash(gsmAppId, getDeviceID(context), keyHash)
            .enqueue(object : Callback<JsonObject?> {
                override fun onResponse(
                    call: Call<JsonObject?>,
                    response: Response<JsonObject?>
                ) {
                    if (response.isSuccessful) {
                        try {
                            if (response.body() != null && JSONObject(
                                    response.body().toString()
                                ).has("isValid")
                            ) {
                                val obj = JSONObject(response.body().toString())
                                result.invoke(obj.getBoolean("isValid"))
                            }
                        } catch (_: JSONException) {
                            result.invoke(false)
                        }
                    } else result.invoke(false)
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                    result.invoke(false)
                }
            })
    }


    fun login(
        context: Context,
        gsmId: String?,
        isDebug: Boolean,
        versionName: String?,
        loginGSMCallback: LoginGSMCallback?
    ) {
        val params = JsonObject()
        params.addProperty("appId", gsmId)
        //        params.addProperty("deviceId", MyUtil.Companion.getDeviceID(context.getApplicationContext()));
        params.addProperty("pkName", context.getPackageName())
        params.addProperty("os", 1) // 1 = android, 2 = ios
        params.addProperty("version", versionName)
        //        Log.d("sobu","set access token GSM params ======= " + params);
        GSMClient.getClient(if (isDebug) DOMAIN_GSM_DEV else DOMAIN_GSM_PROD)
            .create<ApiGSM>(ApiGSM::class.java)
            .login(params)
            .enqueue(object : Callback<JsonObject?> {
                override fun onResponse(call: Call<JsonObject?>, response: Response<JsonObject?>) {
                    Log.d("sobubu", "set access token GSM response ======= " + response)

                    if (response.isSuccessful()) {
                        try {
                            if (response.body() != null && JSONObject(
                                    response.body().toString()
                                ).has("accessToken")
                            ) {
                                val obj = JSONObject(response.body().toString())
                                accessTokenGSM = obj.getString("accessToken")
                                //                                    Log.d("sobubu","set access token GSM ======= " + accessTokenGSM);
                                println("set access token GSM ======= " + accessTokenGSM)
                                if (loginGSMCallback != null) {
                                    loginGSMCallback.loginSuccess(accessTokenGSM)
                                }
                            }
                        } catch (e: JSONException) {
                            if (retryLoginGSM++ < 4) {
                                login(context, gsmId, isDebug, versionName, loginGSMCallback)
                            }
                        }
                    } else {
                        if (retryLoginGSM++ < 4) {
                            login(context, gsmId, isDebug, versionName, loginGSMCallback)
                        }
                    }
                }

                override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
//                        SLog.INSTANCE.d("sobubu","onFailure");
                    login(context, gsmId, isDebug, versionName, loginGSMCallback)
                }
            })
    }

    fun verifyIAP(
        context: Context,
        gsmId: String?,
        isDebug: Boolean,
        versionName: String?,
        params: JsonObject?,
        verifyIAPGSMCallback: VerifyIAPGSMCallback?
    ) {
        if (!accessTokenGSM.isEmpty()) {
            GSMClient.getClient(if (isDebug) DOMAIN_GSM_DEV else DOMAIN_GSM_PROD)
                .create<ApiGSM>(ApiGSM::class.java)
                .verifyIAP("Bearer " + accessTokenGSM, params)
                .enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(
                        call: Call<JsonObject?>,
                        response: Response<JsonObject?>
                    ) {
                        if (response.isSuccessful()) {
                            println("verifyIAP GSM ------ " + response.body())
                            if (verifyIAPGSMCallback != null) verifyIAPGSMCallback.verifySuccess()
                        } else {
                            if (verifyIAPGSMCallback != null) verifyIAPGSMCallback.verifyFail()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        if (verifyIAPGSMCallback != null) verifyIAPGSMCallback.verifyFail()
                    }
                })
        } else {
            retryLoginGSM = 0
            //khi chua co accessToken thi phai call login GSM de lay token truoc
            login(context, gsmId, isDebug, versionName, object : LoginGSMCallback {
                override fun loginSuccess(accessToken: String?) {
                    verifyIAP(context, gsmId, isDebug, versionName, params, verifyIAPGSMCallback)
                }

                override fun loginFail() {
                }
            })
        }
    }

    fun verifySUBS(
        context: Context,
        gsmId: String?,
        isDebug: Boolean,
        versionName: String?,
        params: JsonObject?,
        verifyIAPGSMCallback: VerifyIAPGSMCallback?
    ) {
        if (!accessTokenGSM.isEmpty()) {
            GSMClient.getClient(if (isDebug) DOMAIN_GSM_DEV else DOMAIN_GSM_PROD)
                .create<ApiGSM>(ApiGSM::class.java)
                .verifySubs("Bearer " + accessTokenGSM, params)
                .enqueue(object : Callback<JsonObject?> {
                    override fun onResponse(
                        call: Call<JsonObject?>,
                        response: Response<JsonObject?>
                    ) {
                        if (response.isSuccessful()) {
                            println("verifySubs GSM ------ " + response.body())
                            if (verifyIAPGSMCallback != null) verifyIAPGSMCallback.verifySuccess()
                        } else {
                            if (verifyIAPGSMCallback != null) verifyIAPGSMCallback.verifyFail()
                        }
                    }

                    override fun onFailure(call: Call<JsonObject?>, t: Throwable) {
                        if (verifyIAPGSMCallback != null) verifyIAPGSMCallback.verifyFail()
                    }
                })
        } else {
            retryLoginGSM = 0
            //khi chua co accessToken thi phai call login GSM de lay token truoc
            login(context, gsmId, isDebug, versionName, object : LoginGSMCallback {
                override fun loginSuccess(accessToken: String?) {
                    verifySUBS(context, gsmId, isDebug, versionName, params, verifyIAPGSMCallback)
                }

                override fun loginFail() {
                }
            })
        }
    }
}

package com.game.dhanraj.myownalexa;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.game.dhanraj.myownalexa.sharedpref.Util;
import com.google.gson.Gson;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.game.dhanraj.myownalexa.MainActivity.PREF_ACCESS_TOKEN;
import static com.game.dhanraj.myownalexa.MainActivity.PREF_REFRESH_TOKEN;
import static com.game.dhanraj.myownalexa.MainActivity.PREF_TOKEN_EXPIRES;
import static com.game.dhanraj.myownalexa.MainActivity.getOkhttp;
import static com.game.dhanraj.myownalexa.MainActivity.myContext;
import static com.game.dhanraj.myownalexa.MainActivity.saveToken;

/**
 * Created by dhanraj on 16/7/17.
 */

public class TokenHandler {



    public Context myContext;
    public String myresponse;
    public static final int FirstMainActivityDoPostRequest = 101;
    public static final int DownChannelCase1 = 102;
    public static final int DownChannelCase2 = 103;
    public static final int DownChannelCase3 = 104;
    public static final int SendAudioRequest = 105;
    public static final int SendSpeechFinishedEvent=106;
    public static final int SendSpeechStartedEvent=107;
    public static final int SendPlaybackStartedEvent = 108;


    public TokenHandler(Context context) {
        myContext = context;
    }

    public void getAccessToken(int event) {
        SharedPreferences preferences = Util.getPrefernces(myContext);
        //if we have an access token
        if (preferences.contains(PREF_ACCESS_TOKEN)) {
            Log.d("containAccessToken","contains");
            if (preferences.getLong(PREF_TOKEN_EXPIRES, 0) > System.currentTimeMillis()) {
                //if it's not expired, return the existing token
                Log.d("notexpired","contains");
              //  return preferences.getString(PREF_ACCESS_TOKEN, null);
                String accessToken = preferences.getString(PREF_ACCESS_TOKEN,null);
                EventBus.getDefault().post(new MessageEvent(event,accessToken));

            } else {
                //if it is expired but we have a refresh token, get a new token

                if (preferences.contains(PREF_REFRESH_TOKEN)) {
                    Log.d("expired","doesnotcontains");
                    getRefreshToken(preferences.getString(PREF_REFRESH_TOKEN, ""),event);

                }
            }
        }

    }

    private void getRefreshToken(String string,int event) {
        RequestBody formBody = new FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", string)
                .add("client_id", Util.getPrefernces(myContext).getString("clientId",""))
                .build();
        doPostRequest(formBody,event);

    }

    public void doPostRequest(final RequestBody form, final int checkRefreshTokenEvent) {

        OkHttpClient okclient = getOkhttp();
        Request request = new Request.Builder()
                .url("https://api.amazon.com/auth/O2/token")
                .post(form)
                .build();
        Response response=null;



        okclient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
                Log.d("accessToken","failed");
                doPostRequest(form,checkRefreshTokenEvent);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                SharedPreferences preferences = Util.getPrefernces(myContext);
                myresponse = response.body().string();


                MainActivity.TokenResponse tokenResponse = new Gson().fromJson(myresponse, MainActivity.TokenResponse.class);
                saveToken(tokenResponse);

                if(checkRefreshTokenEvent==FirstMainActivityDoPostRequest)
                {
                    Intent st = new Intent(myContext,DownChannel.class);
                    myContext.startService(st);
                }else
                    EventBus.getDefault().post(new MessageEvent(checkRefreshTokenEvent,preferences.getString(PREF_ACCESS_TOKEN,null)));



            }
        });


    }

}

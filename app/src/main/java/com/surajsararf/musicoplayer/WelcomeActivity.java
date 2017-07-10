package com.surajsararf.musicoplayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.surajsararf.musicoplayer.AsyncTasks.AsyncGetSongList;
import com.surajsararf.musicoplayer.Custom.MediaStoreAccessHelper;
import com.surajsararf.musicoplayer.service.SongPlayback;
import com.surajsararf.musicoplayer.util.PlayerConstants;
import com.surajsararf.musicoplayer.util.UtilFunctions;

import java.util.concurrent.TimeUnit;


public class WelcomeActivity extends AppCompatActivity implements AsyncGetSongList.OnBuildLibraryProgressUpdate{

    private static Intent intent,intent1;
    private static Activity activity;
    public static Handler mHandler,mHandler1;
    private Context mContext;
    private static int counter=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        mHandler=new Handler();
        int getAction=MainActivity.forNothingKey;
        int isPlay=SongPlayback.isPlayFromMainFalse;
        try {
            if (getIntent().hasExtra(SongPlayback.RestartServiceAction))
                getAction = getIntent().getExtras().getInt(SongPlayback.RestartServiceAction, MainActivity.forNothingKey);
            if (getIntent().hasExtra(SongPlayback.isPlayFromMain))
                isPlay = getIntent().getExtras().getInt(SongPlayback.isPlayFromMain, SongPlayback.isPlayFromMainFalse);
        }
        catch (Exception e){}

        setHandler();
        intent = new Intent(mContext, SongPlayback.class);
        intent.putExtra(SongPlayback.isPlayFromMain,isPlay);
        intent.putExtra(SongPlayback.RestartServiceAction,getAction);
        intent.putExtra(SongPlayback.isStartFromMain,false);
        intent1 = new Intent(mContext, MainActivity.class);
        activity=this;

        if (UtilFunctions.isServiceRunning(SongPlayback.class.getName(),this))
        {
            if (!PlayerConstants.mSongPlayback.getCurrentMediaPlayer().isPlaying() && !PlayerConstants.mSongPlayback.isPlay)
                mHandler.post(lastPosition);

            Cursor mCursor= MediaStoreAccessHelper.getAllSongs(mContext, null, null);
            if (PlayerConstants.SONGS_LIST == null
                    || PlayerConstants.SONGS_LIST.size() <= 0
                    || mCursor.getCount()!=PlayerConstants.SONGS_LIST.size()) {
                PlayerConstants.mSongPlayback.stopServiceManually();
                startService(intent);
            }
            else
                mHandler1.sendEmptyMessage(0);
        }
        else
        {
            mHandler.post(lastPosition);
            startService(intent);
        }

        SystemClock.sleep(TimeUnit.SECONDS.toMillis(2));
    }
    void setHandler(){
        mHandler1=new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Log.e("call",String.valueOf(counter));
                counter++;
                runAction();
                return false;
            }
        });
    }

    private Runnable lastPosition=new Runnable() {
        @Override
        public void run() {
            lastPosition();
        }
    };

    void lastPosition(){
        int lastSong= UtilFunctions.getSharedPreferenceint(mContext,UtilFunctions.LastSongNumber,-1);
        if (lastSong>-1)
        {
            PlayerConstants.SONG_NUMBER=lastSong;
            PlayerConstants.lastDuration=UtilFunctions.getSharedPreferenceint(mContext,UtilFunctions.LastDuration,0);
        }
    }

    void runAction(){
        startActivity(intent1);
        activity.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onStartBuildingLibrary() {
    }

    @Override
    public void onFinishBuildingLibrary(AsyncGetSongList task) {
        try {
            new CountDownTimer(200, 100) {
                @Override
                public void onTick(long millisUntilFinished) {
                }

                @Override
                public void onFinish() {
                    try {
                        mHandler1.sendEmptyMessage(0);
                    }
                    catch (Exception e)
                    {}
                }
            }.start();
        }
        catch (Exception e)
        {
        }
    }
}
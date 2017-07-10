package com.surajsararf.musicoplayer.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.surajsararf.musicoplayer.AsyncTasks.AsyncGetSongList;
import com.surajsararf.musicoplayer.WelcomeActivity;

/**
 * Created by surajsararf on 1/3/16.
 */
public class LoadSongsService extends Service implements AsyncGetSongList.OnBuildLibraryProgressUpdate {
    public Service mService;
    public Context mContext;

    @Override
    public void onCreate() {
        mService=this;
        mContext=this.getApplicationContext();
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        AsyncGetSongList task=new AsyncGetSongList(mContext);
        task.setOnBuildLibraryProgressUpdate(this);
        task.setOnBuildLibraryProgressUpdate(new WelcomeActivity());
        task.execute();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStartBuildingLibrary() {
    }

    @Override
    public void onFinishBuildingLibrary(AsyncGetSongList task) {
        mService.stopSelf();
    }

}

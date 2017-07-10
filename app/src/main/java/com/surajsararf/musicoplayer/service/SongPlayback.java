package com.surajsararf.musicoplayer.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.squareup.picasso.Picasso;
import com.surajsararf.musicoplayer.Custom.MediaStoreAccessHelper;
import com.surajsararf.musicoplayer.MainActivity;
import com.surajsararf.musicoplayer.R;
import com.surajsararf.musicoplayer.receiver.NotificationBroadcast;
import com.surajsararf.musicoplayer.util.AudioManagerHelper;
import com.surajsararf.musicoplayer.util.MediaItem;
import com.surajsararf.musicoplayer.util.PlayerConstants;
import com.surajsararf.musicoplayer.util.UtilFunctions;

import java.io.FileDescriptor;
import java.io.IOException;


public class SongPlayback extends Service {

    public static final int mNotificationId = 1180;
    public static final String NOTIFY_LaunchNowPlaying = "com.surajsararf.musicoplayer.launchnowplaying";
    public static final String NOTIFY_PREVIOUS = "com.surajsararf.musicoplayer.previous";
    public static final String NOTIFY_Close = "com.surajsararf.musicoplayer.close";
    public static final String NOTIFY_PAUSE = "com.surajsararf.musicoplayer.pause";
    public static final String NOTIFY_PLAY = "com.surajsararf.musicoplayer.play";
    public static final String NOTIFY_NEXT = "com.surajsararf.musicoplayer.next";

    public static final String isStartFromMain="isStartFromMain";
    public static final String isPlayFromMain="isPlayFromMain";
    public static final String RestartServiceAction="RestartServiceAction";

    public static final int isPlayFromMainFalse=0;
    public static final int isPlayFromMainTrue=1;

    private Context mContext;
    private Service mService;
    private MediaPlayer mMediaPlayer;
    private MediaPlayer mMediaPlayer2;
    public int mCurrentMediaPlayer = 1;
    private boolean mFirstRun = true;
    private AudioManager mAudioManager;
    private AudioManagerHelper mAudioManagerHelper;
    private Handler mHandler;
    private boolean mMediaPlayerPrepared = false;
    private boolean mMediaPlayer2Prepared = false;
    private int mCrossfadeDuration;

    private float mFadeOutVolume = 0f;
    private float mFadeInVolume = 1.0f;
    private NotificationCompat.Builder mNotificationBuilder;

    private int isPlayOnStart=0;
    private int getAction=3;

    public static boolean isPlay=false;

    private ComponentName remoteComponentName;
    private RemoteControlClient remoteControlClient;

    public SongPlayback() {
        super();
    }

    @Override
    public void onCreate() {
    }

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            isPlayOnStart=intent.getExtras().getInt(SongPlayback.isPlayFromMain, SongPlayback.isPlayFromMainFalse);
            getAction=intent.getExtras().getInt(SongPlayback.RestartServiceAction, MainActivity.forNothingKey);
        }
        catch (Exception e){}

        mContext = getApplicationContext();
        mService = this;
        Boolean get=true;
        try {
            get=intent.getBooleanExtra(SongPlayback.isStartFromMain,true);
        }
        catch (Exception e){}
        LoadSongList(get);

        PlayerConstants.mSongPlayback=this;

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        mHandler = new Handler();

        initMediaPlayers();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        //mSongData= PlayerConstants.SONGS_LIST.get(PlayerConstants.SONG_NUMBER);
        mAudioManagerHelper = new AudioManagerHelper();

        if (UtilFunctions.currentVersionSupportLockScreenControls())
        {
            RegisterRemoteClient();
        }
        if (!getCurrentMediaPlayer().isPlaying())
            getIntentAction();

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void getIntentAction(){
        if (getAction==MainActivity.forNextKey) {
            skipToNextTrack();
        }
        else if (getAction==MainActivity.forPreviousKey){
            skipToPreviousTrack(true);
        }
        else if (getAction==MainActivity.forSkipTrack){
            skipToTrack(PlayerConstants.SONG_NUMBER);
        }
        else if (getAction==MainActivity.forNothingKey) {
            mHandler.post(setLastposition);
        }
    }

    private Runnable setLastposition=new Runnable() {
        @Override
        public void run() {
            setLastposition();
        }
    };

    public void setLastposition(){
        if (PlayerConstants.SONG_NUMBER > -1) {
            if (prepareMediaPlayer(PlayerConstants.SONG_NUMBER))
            {
                mHandler.postDelayed(setSeekto, 300);
            }
            else
            {
                mHandler.post(checkTotalSong);
            }
        }
    }

    private Runnable checkTotalSong =new Runnable() {
        @Override
        public void run() {
            Cursor cursor= MediaStoreAccessHelper.getAllSongs(mContext, null, null);
            if (cursor.getCount()>0)
            {
                if (PlayerConstants.SONG_NUMBER+1>cursor.getCount())
                    PlayerConstants.SONG_NUMBER=-1;
                else
                    mHandler.postDelayed(setLastposition,200);
            }
            else
            {
                PlayerConstants.SONG_NUMBER=-1;
            }
        }
    };

    private Runnable setSeekto=new Runnable() {
        @Override
        public void run() {
            if (isMediaPlayerPrepared()) {
                {
                    if (SongPlayback.isPlayFromMainFalse==isPlayOnStart)
                        pausePlayback();
                    else {
                        MainActivity.changeButton();
                        MainActivity.updateUI();
                    }
                }
                mHandler.removeCallbacks(this);
                getCurrentMediaPlayer().seekTo(PlayerConstants.lastDuration);
                PlayerConstants.lastDuration = 0;
            }
            else {
                mHandler.postDelayed(this, 300);
            }
        }
    };

    public void LoadSongList(Boolean isFromMain) {
        Cursor cursor= MediaStoreAccessHelper.getAllSongs(mContext, null, null);
        if (isFromMain)
        {
            if (cursor.getCount()!=PlayerConstants.SONGS_LIST.size()) {
                if (!UtilFunctions.isServiceRunning(LoadSongsService.class.getName(), getApplicationContext())) {
                    Intent intent = new Intent(getApplicationContext(), LoadSongsService.class);
                    startService(intent);
                }
            }
        }
        else {
            if (!UtilFunctions.isServiceRunning(LoadSongsService.class.getName(), getApplicationContext())) {
                Intent intent = new Intent(getApplicationContext(), LoadSongsService.class);
                startService(intent);
            }
        }
    }



    private void initMediaPlayers() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        if (mMediaPlayer2 != null) {
            getMediaPlayer2().release();
            mMediaPlayer2 = null;
        }

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer2 = new MediaPlayer();
        setCurrentMediaPlayer(1);

        getMediaPlayer().reset();
        getMediaPlayer2().reset();

        try {
            mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
            getMediaPlayer2().setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);
        } catch (Exception e) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer2 = new MediaPlayer();
            setCurrentMediaPlayer(1);
        }
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        getMediaPlayer2().setAudioStreamType(AudioManager.STREAM_MUSIC);

    }

    private Runnable startMediaPlayerIfPrepared = new Runnable() {

        @Override
        public void run() {
            if (isMediaPlayerPrepared())
                startMediaPlayer();
            else
                mHandler.postDelayed(this, 100);
        }

    };

    private void startMediaPlayer() throws IllegalStateException {
        isPlay=true;
        setCurrentMediaPlayer(1);
        getMediaPlayer().start();

        if (!mFirstRun == false) {
            if (mHandler != null) {
                mHandler.post(startCrossFadeRunnable);
            }
        }

        updateNotification();
//        setCurrentSong(getCurrentSong());
        int nextsong=PlayerConstants.SONG_NUMBER+1;

        prepareMediaPlayer2(nextsong);

    }

    public boolean prepareMediaPlayer(int songIndex) {
        try {
            if (songIndex==-1 || songIndex+1>PlayerConstants.SONGS_LIST.size())
            {
                return false;
            }


            getMediaPlayer().reset();
            if (mFirstRun) {
                startForeground(mNotificationId, buildNotification());
            }

            getMediaPlayer().setDataSource(mContext, Uri.parse(PlayerConstants.SONGS_LIST.get(songIndex).getFilepath()));
            getMediaPlayer().setOnPreparedListener(mediaPlayerPrepared);
            getMediaPlayer().setOnErrorListener(onErrorListener);
            getMediaPlayer().prepareAsync();

        } catch (Exception e) {
        }

        return true;
    }

    public MediaPlayer.OnPreparedListener mediaPlayerPrepared = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {

            setIsMediaPlayerPrepared(true);

            getMediaPlayer().setOnCompletionListener(onMediaPlayerCompleted);

            if (checkAndRequestAudioFocus() == true) {

                if (mFirstRun) {
                    startMediaPlayer();
                    mFirstRun = false;
                }

            } else {
                return;
            }

        }

    };

    private MediaPlayer.OnCompletionListener onMediaPlayerCompleted = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {

            //PlayerConstants.SONG_NUMBER=determineNextSongIndex();

            mHandler.removeCallbacks(startCrossFadeRunnable);
            mHandler.removeCallbacks(crossFadeRunnable);

            if (mHandler != null) {
                mHandler.post(startCrossFadeRunnable);
            }

            mFadeInVolume = 0.0f;
            mFadeOutVolume = 1.0f;

            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);

            try {
                if (isAtEndOfQueue()) {
                    MainActivity.setSongPlay(PlayerConstants.SONG_NUMBER,0);
                    IflastSong();
                } else if (isMediaPlayer2Prepared()) {
                    MainActivity.setSongPlay(PlayerConstants.SONG_NUMBER,PlayerConstants.SONG_NUMBER+1);
                    PlayerConstants.SONG_NUMBER++;
                    startMediaPlayer2();
                } else {
                    MainActivity.setSongPlay(PlayerConstants.SONG_NUMBER,PlayerConstants.SONG_NUMBER+1);
                    PlayerConstants.SONG_NUMBER++;
                    mHandler.post(startMediaPlayer2IfPrepared);
                }

                MainActivity.changeUI();
                UpdateMetadata();
                MainActivity.updateImage();

            } catch (IllegalStateException e) {
                mHandler.post(startMediaPlayer2IfPrepared);
            }

        }

    };

    private Runnable startMediaPlayer2IfPrepared = new Runnable() {

        @Override
        public void run() {
            if (isMediaPlayer2Prepared())
                startMediaPlayer2();
            else
                mHandler.postDelayed(this, 100);
        }

    };

    private void startMediaPlayer2() throws IllegalStateException {
        isPlay=true;
        setCurrentMediaPlayer(2);
        getMediaPlayer2().start();

        updateNotification();
        int nextsong=PlayerConstants.SONG_NUMBER+1;
        prepareMediaPlayer(nextsong);
    }

    public boolean prepareMediaPlayer2(int songIndex) {

        try {
            //Stop here if we're at the end of the queue.
            if (songIndex==-1 || songIndex+1>PlayerConstants.SONGS_LIST.size())
                return true;

            //Reset mMediaPlayer2 to its uninitialized state.
            getMediaPlayer2().reset();

    		/*
    		 * Set the data source for mMediaPlayer and start preparing it
    		 * asynchronously.
    		 */
            getMediaPlayer2().setDataSource(mContext, Uri.parse(PlayerConstants.SONGS_LIST.get(songIndex).getFilepath()));
            getMediaPlayer2().setOnPreparedListener(mediaPlayer2Prepared);
            getMediaPlayer2().setOnErrorListener(onErrorListener);
            getMediaPlayer2().prepareAsync();

        } catch (Exception e) {
            if (!isAtEndOfQueue())
                prepareMediaPlayer2(songIndex+1);
            else
                return false;

            return false;
        }

        return true;
    }

    public MediaPlayer.OnPreparedListener mediaPlayer2Prepared = new MediaPlayer.OnPreparedListener() {

        @Override
        public void onPrepared(MediaPlayer mediaPlayer) {

            //Update the prepared flag.
            setIsMediaPlayer2Prepared(true);

            //Set the completion listener for mMediaPlayer2.
            getMediaPlayer2().setOnCompletionListener(onMediaPlayer2Completed);

            //Check to make sure we have AudioFocus.
            if (checkAndRequestAudioFocus()==true) {


            } else {
                return;
            }

        }

    };

    private MediaPlayer.OnCompletionListener onMediaPlayer2Completed = new MediaPlayer.OnCompletionListener() {

        @Override
        public void onCompletion(MediaPlayer mp) {
            //PlayerConstants.SONG_NUMBER=determineNextSongIndex();

            //Remove the crossfade playback.
            mHandler.removeCallbacks(startCrossFadeRunnable);
            mHandler.removeCallbacks(crossFadeRunnable);

            //Set the track position handler (notifies the handler when the track should start being faded).
            if (mHandler!=null) {
                mHandler.post(startCrossFadeRunnable);
            }

            //Reset the fadeVolume variables.
            mFadeInVolume = 0.0f;
            mFadeOutVolume = 1.0f;

            //Reset the volumes for both mediaPlayers.
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);



            try {
                if (isAtEndOfQueue()) {
                    MainActivity.setSongPlay(PlayerConstants.SONG_NUMBER,0);
                    IflastSong();
                } else if (isMediaPlayerPrepared()) {
                    MainActivity.setSongPlay(PlayerConstants.SONG_NUMBER,PlayerConstants.SONG_NUMBER+1);
                    PlayerConstants.SONG_NUMBER++;
                    startMediaPlayer();
                } else {
                    MainActivity.setSongPlay(PlayerConstants.SONG_NUMBER,PlayerConstants.SONG_NUMBER+1);
                    PlayerConstants.SONG_NUMBER++;
                    //Check every 100ms if mMediaPlayer is prepared.
                    mHandler.post(startMediaPlayerIfPrepared);
                }

                MainActivity.changeUI();
                UpdateMetadata();
                MainActivity.updateImage();

            } catch (IllegalStateException e) {
                //mMediaPlayer isn't prepared yet.
                mHandler.post(startMediaPlayerIfPrepared);
            }

        }

    };

    public MediaPlayer.OnErrorListener onErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mMediaPlayer, int what, int extra) {
            return true;
        }
    };

    public Runnable startCrossFadeRunnable = new Runnable() {

        @Override
        public void run() {

            //Check if we're in the last part of the current song.
            try {
                if (getCurrentMediaPlayer().isPlaying()) {

                    int currentTrackDuration = getCurrentMediaPlayer().getDuration();
                    int currentTrackFadePosition = currentTrackDuration - (mCrossfadeDuration * 1000);
                    if (getCurrentMediaPlayer().getCurrentPosition() >= currentTrackFadePosition) {
                        //Launch the next runnable that will handle the cross fade effect.
                        mHandler.postDelayed(crossFadeRunnable, 100);

                    } else {
                        mHandler.postDelayed(startCrossFadeRunnable, 1000);
                    }

                } else {
                    mHandler.postDelayed(startCrossFadeRunnable, 1000);
                }

            } catch (Exception e) {
            }

        }

    };

    public Runnable crossFadeRunnable = new Runnable() {

        @Override
        public void run() {
            try {

                if (PlayerConstants.SONGS_LIST.size() > (PlayerConstants.SONG_NUMBER + 1)) {

                    //Set the next mMediaPlayer's volume and raise it incrementally.
                    if (getCurrentMediaPlayer() == getMediaPlayer()) {

                        getMediaPlayer2().setVolume(mFadeInVolume, mFadeInVolume);
                        getMediaPlayer().setVolume(mFadeOutVolume, mFadeOutVolume);

                        //If the mMediaPlayer is already playing or it hasn't been prepared yet, we can't use crossfade.
                        if (!getMediaPlayer2().isPlaying()) {

                            if (mMediaPlayer2Prepared == true) {

                                if (checkAndRequestAudioFocus() == true) {
                                    getMediaPlayer2().start();
                                } else {
                                    return;
                                }

                            }

                        }

                    } else {

                        getMediaPlayer().setVolume(mFadeInVolume, mFadeInVolume);
                        getMediaPlayer2().setVolume(mFadeOutVolume, mFadeOutVolume);

                        //If the mMediaPlayer is already playing or it hasn't been prepared yet, we can't use crossfade.
                        if (!getMediaPlayer().isPlaying()) {

                            if (mMediaPlayerPrepared == true) {

                                if (checkAndRequestAudioFocus() == true) {
                                    getMediaPlayer().start();
                                } else {
                                    return;
                                }

                            }

                        }

                    }

                    mFadeInVolume = mFadeInVolume + (float) (1.0f / (((float) mCrossfadeDuration) * 10.0f));
                    mFadeOutVolume = mFadeOutVolume - (float) (1.0f / (((float) mCrossfadeDuration) * 10.0f));

                    mHandler.postDelayed(crossFadeRunnable, 100);
                }

            } catch (Exception e) {

            }
        }

    };

    private Runnable duckDownVolumeRunnable = new Runnable() {

        @Override
        public void run() {
            if (mAudioManagerHelper.getCurrentVolume() > mAudioManagerHelper.getTargetVolume()) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        (mAudioManagerHelper.getCurrentVolume() - mAudioManagerHelper.getStepDownIncrement()),
                        0);

                mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mHandler.postDelayed(this, 50);
            }

        }

    };

    private Runnable duckUpVolumeRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAudioManagerHelper.getCurrentVolume() < mAudioManagerHelper.getTargetVolume()) {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        (mAudioManagerHelper.getCurrentVolume() + mAudioManagerHelper.getStepUpIncrement()),
                        0);

                mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mHandler.postDelayed(this, 50);
            }

        }

    };

    private boolean checkAndRequestAudioFocus() {
        if (mAudioManagerHelper.hasAudioFocus() == false) {
            if (requestAudioFocus() == true) {
                return true;
            } else {
                return false;
            }

        } else {
            return true;
        }

    }

    private boolean requestAudioFocus() {
        int result = mAudioManager.requestAudioFocus(audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);

        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //Stop the service.
            mService.stopSelf();
            return false;
        } else {
            return true;
        }

    }

    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {

        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                //We've temporarily lost focus, so pause the mMediaPlayer, wherever it's at.
                try {
                    isPlay=false;
                    getCurrentMediaPlayer().pause();

                    //updateWidgets();
                    mAudioManagerHelper.setHasAudioFocus(false);
                    if(UtilFunctions.currentVersionSupportLockScreenControls()){
                        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                    }
                    MainActivity.changeButton();
                    updateNotification();

                } catch (Exception e) {
                }

            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                //Lower the current mMediaPlayer volume.
                mAudioManagerHelper.setAudioDucked(true);
                mAudioManagerHelper.setTargetVolume(5);
                mAudioManagerHelper.setStepDownIncrement(1);
                mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mAudioManagerHelper.setOriginalVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
                mHandler.post(duckDownVolumeRunnable);

            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if(UtilFunctions.currentVersionSupportLockScreenControls()){
                    remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
                }
                if (mAudioManagerHelper.isAudioDucked()) {
                    //Crank the volume back up again.
                    mAudioManagerHelper.setTargetVolume(mAudioManagerHelper.getOriginalVolume());
                    mAudioManagerHelper.setStepUpIncrement(1);
                    mAudioManagerHelper.setCurrentVolume(mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC));

                    mHandler.post(duckUpVolumeRunnable);
                    mAudioManagerHelper.setAudioDucked(false);
                } else {
                    //We've regained focus. Update the audioFocus tag, but don't start the mMediaPlayer.
                    mAudioManagerHelper.setHasAudioFocus(true);
                    MainActivity.changeButton();
                    updateNotification();
                    UpdateMetadata();
                }



            } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                //We've lost focus permanently so pause the service. We'll have to request focus again later.
                getCurrentMediaPlayer().pause();
                if(UtilFunctions.currentVersionSupportLockScreenControls()){
                    remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
                }
                isPlay=false;
                mAudioManagerHelper.setHasAudioFocus(false);

                MainActivity.changeButton();
                updateNotification();
            }

        }

    };


    public void updateNotification() {
        Notification notification = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            notification = buildJBNotification();
        else
            notification = buildICSNotification();

        NotificationManager notifManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notifManager.notify(mNotificationId, notification);
    }

    @SuppressLint("NewApi")
    private Notification buildJBNotification() {

        MediaItem mSongData=PlayerConstants.SONGS_LIST.get(PlayerConstants.SONG_NUMBER);

        mNotificationBuilder = new NotificationCompat.Builder(mContext);
        mNotificationBuilder.setOngoing(true);
        mNotificationBuilder.setAutoCancel(false);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_music);

        //Open up the player screen when the user taps on the notification.
        Intent launchNowPlayingIntent = new Intent();
        launchNowPlayingIntent.setAction(SongPlayback.NOTIFY_LaunchNowPlaying);
        PendingIntent launchNowPlayingPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, launchNowPlayingIntent, 0);
        mNotificationBuilder.setContentIntent(launchNowPlayingPendingIntent);

        //Grab the notification layouts.
        RemoteViews notificationView = new RemoteViews(mContext.getPackageName(), R.layout.custom_notification);

        //Initialize the notification layout buttons.
        Intent previousTrackIntent = new Intent();
        previousTrackIntent.setAction(SongPlayback.NOTIFY_PREVIOUS);
        PendingIntent previousTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, previousTrackIntent, 0);

        Intent play = new Intent();
        play.setAction(SongPlayback.NOTIFY_PLAY);
        PendingIntent playTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, play, 0);

        Intent pause = new Intent();
        pause.setAction(SongPlayback.NOTIFY_PAUSE);
        PendingIntent pauseTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, pause, 0);

        Intent nextTrackIntent = new Intent();
        nextTrackIntent.setAction(SongPlayback.NOTIFY_NEXT);
        PendingIntent nextTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, nextTrackIntent, 0);

        Intent stopServiceIntent = new Intent();
        stopServiceIntent.setAction(SongPlayback.NOTIFY_Close);
        PendingIntent stopServicePendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, stopServiceIntent, 0);

        if (isPlay) {
            notificationView.setViewVisibility(R.id.play, View.GONE);
            notificationView.setViewVisibility(R.id.pause, View.VISIBLE);
        } else {
            notificationView.setViewVisibility(R.id.play, View.VISIBLE);
            notificationView.setViewVisibility(R.id.pause, View.GONE);
        }

        notificationView.setTextViewText(R.id.songname, mSongData.getTitle());
        notificationView.setTextViewText(R.id.artist_album_name, mSongData.getArtist() + " | " + mSongData.getAlbum());

        notificationView.setOnClickPendingIntent(R.id.close, stopServicePendingIntent);
        notificationView.setImageViewBitmap(R.id.album_art, getAlbumArtBitmap(mSongData.getAlbumArtPath()));

        notificationView.setOnClickPendingIntent(R.id.play, playTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.pause, pauseTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.next, nextTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.previous, previousTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.close, stopServicePendingIntent);

        mNotificationBuilder.setContent(notificationView);

        Notification notification = mNotificationBuilder.build();

        notification.flags = Notification.FLAG_FOREGROUND_SERVICE |
                Notification.FLAG_NO_CLEAR |
                Notification.FLAG_ONGOING_EVENT;

        return notification;
    }

    private Notification buildICSNotification() {

        MediaItem mSongData=PlayerConstants.SONGS_LIST.get(PlayerConstants.SONG_NUMBER);

        mNotificationBuilder = new NotificationCompat.Builder(mContext);
        mNotificationBuilder.setOngoing(true);
        mNotificationBuilder.setAutoCancel(false);
        mNotificationBuilder.setSmallIcon(R.drawable.ic_music);

        //Open up the player screen when the user taps on the notification.
        Intent launchNowPlayingIntent = new Intent();
        launchNowPlayingIntent.setAction(SongPlayback.NOTIFY_LaunchNowPlaying);
        PendingIntent launchNowPlayingPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, launchNowPlayingIntent, 0);
        mNotificationBuilder.setContentIntent(launchNowPlayingPendingIntent);

        //Grab the notification layout.
        RemoteViews notificationView = new RemoteViews(mContext.getPackageName(), R.layout.custom_notification);

        //Initialize the notification layout buttons.
        Intent previousTrackIntent = new Intent();
        previousTrackIntent.setAction(SongPlayback.NOTIFY_PREVIOUS);
        PendingIntent previousTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, previousTrackIntent, 0);

        Intent playTrackIntent = new Intent();
        playTrackIntent.setAction(SongPlayback.NOTIFY_PLAY);
        PendingIntent playTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, playTrackIntent, 0);

        Intent pauseTrackIntent = new Intent();
        pauseTrackIntent.setAction(SongPlayback.NOTIFY_PLAY);
        PendingIntent pauseTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, pauseTrackIntent, 0);

        Intent nextTrackIntent = new Intent();
        nextTrackIntent.setAction(SongPlayback.NOTIFY_NEXT);
        PendingIntent nextTrackPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, nextTrackIntent, 0);

        Intent stopServiceIntent = new Intent();
        stopServiceIntent.setAction(SongPlayback.NOTIFY_Close);
        PendingIntent stopServicePendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(), 0, stopServiceIntent, 0);

        notificationView.setOnClickPendingIntent(R.id.play, playTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.pause, pauseTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.next, nextTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.previous, previousTrackPendingIntent);
        notificationView.setOnClickPendingIntent(R.id.close, stopServicePendingIntent);

        //Check if audio is playing and set the appropriate play/pause button.
        if (isPlay) {
            notificationView.setViewVisibility(R.id.play, View.GONE);
            notificationView.setViewVisibility(R.id.pause, View.VISIBLE);
        } else {
            notificationView.setViewVisibility(R.id.play, View.VISIBLE);
            notificationView.setViewVisibility(R.id.pause, View.GONE);
        }

        notificationView.setTextViewText(R.id.songname, mSongData.getTitle());
        notificationView.setTextViewText(R.id.artist_album_name, mSongData.getArtist() + " | " + mSongData.getAlbum());


        //Set the "Stop Service" pending intent.
        notificationView.setOnClickPendingIntent(R.id.close, stopServicePendingIntent);

        //Set the album art.
        notificationView.setImageViewBitmap(R.id.album_art, getAlbumArtBitmap(mSongData.getAlbumArtPath()));

        //Attach the shrunken layout to the notification.
        mNotificationBuilder.setContent(notificationView);

        //Build the notification object and set its flags.
        Notification notification = mNotificationBuilder.build();
        notification.flags = Notification.FLAG_FOREGROUND_SERVICE |
                Notification.FLAG_NO_CLEAR |
                Notification.FLAG_ONGOING_EVENT;

        return notification;
    }

    private Notification buildNotification() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            return buildJBNotification();}
        else
            return buildICSNotification();
    }

    @SuppressLint("NewApi")
    private void RegisterRemoteClient(){
        remoteComponentName = new ComponentName(getApplicationContext(), new NotificationBroadcast().ComponentName());
        try {
            if(remoteControlClient == null) {
                mAudioManager.registerMediaButtonEventReceiver(remoteComponentName);
                Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                mediaButtonIntent.setComponent(remoteComponentName);
                PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
                remoteControlClient = new RemoteControlClient(mediaPendingIntent);
                mAudioManager.registerRemoteControlClient(remoteControlClient);
            }
            remoteControlClient.setTransportControlFlags(
                    RemoteControlClient.FLAG_KEY_MEDIA_PLAY |
                            RemoteControlClient.FLAG_KEY_MEDIA_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE |
                            RemoteControlClient.FLAG_KEY_MEDIA_STOP |
                            RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS |
                            RemoteControlClient.FLAG_KEY_MEDIA_NEXT);
        }catch(Exception ex) {
        }
    }


    @SuppressLint("NewApi")
    private void UpdateMetadata(){
        MediaItem data=PlayerConstants.SONGS_LIST.get(PlayerConstants.SONG_NUMBER);
        if (remoteControlClient == null)
            return;
        RemoteControlClient.MetadataEditor metadataEditor = remoteControlClient.editMetadata(true);
        metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, data.getAlbum());
        metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, data.getArtist());
        metadataEditor.putString(MediaMetadataRetriever.METADATA_KEY_TITLE, data.getTitle());

        metadataEditor.putBitmap(RemoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, getAlbumArtBitmap(data.getAlbumArtPath()));
        metadataEditor.apply();
        mAudioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    public Bitmap getAlbumArtBitmap(Uri uri){
        Bitmap bm=getAlbumArt(uri);
        if (bm==null)
        {
            bm=BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art);
        }
        return bm;
    }
    public Bitmap getAlbumArt(final Uri uri){
        Bitmap bm = null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        try {
            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd, null, options);
                pfd = null;
                fd = null;
            }
        }
        catch (Exception e){}
        return bm;
    }

    private Runnable changeUI=new Runnable() {
        @Override
        public void run() {
            MainActivity.changeUI();
        }
    };

    private Runnable changeButton=new Runnable() {
        @Override
        public void run() {
            MainActivity.changeButton();
        }
    };

    private Runnable updateUI=new Runnable() {
        @Override
        public void run() {
            MainActivity.updateUI();
            MainActivity.setupMusicPlayer();
        }
    };

    private Runnable updateImage=new Runnable() {
        @Override
        public void run() {
            MainActivity.updateImage();
        }
    };

    public boolean startPlayback() {

        try {
            isPlay=true;
            if (checkAndRequestAudioFocus()) {
                getCurrentMediaPlayer().start();
                updateNotification();
                UpdateMetadata();
                //updateWidgets();
            } else {
                return false;
            }

            if(UtilFunctions.currentVersionSupportLockScreenControls()){
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            }

            mHandler.postDelayed(changeButton, 100);

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean pausePlayback() {

        try {
            isPlay=false;
            getCurrentMediaPlayer().pause();

            updateNotification();
            UpdateMetadata();

            if(UtilFunctions.currentVersionSupportLockScreenControls()){
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED);
            }

            mHandler.postDelayed(changeButton, 100);
            //updateWidgets();
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean stopServiceManually(){
        try {
            isPlay=false;
            if(UtilFunctions.currentVersionSupportLockScreenControls()){
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            saveLastPosition();
            MainActivity.changeButton();

            mService.stopSelf();
        }
        catch (Exception e){}
        return true;
    }

    public boolean stopPlayback() {

        try {
            getCurrentMediaPlayer().stop();
            updateNotification();
            MainActivity.changeUI();
            //updateWidgets();

        } catch (Exception e) {
            return false;
        }

        return true;
    }

    public boolean skipToNextTrack() {
        try {
            int lastSong=PlayerConstants.SONG_NUMBER;
            final boolean lastisPlay=isPlay;
            isPlay=true;

            incrementCurrentSongIndex();

            getMediaPlayer().reset();
            getMediaPlayer2().reset();
            clearCrossfadeCallbacks();

            getHandler().removeCallbacks(crossFadeRunnable);
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);

            mFirstRun = true;
            prepareMediaPlayer(PlayerConstants.SONG_NUMBER);
            MainActivity.setSongPlay(lastSong, PlayerConstants.SONG_NUMBER);
            updateNotification();
            UpdateMetadata();

            if (!lastisPlay)
            {
                mHandler.postDelayed(changeButton,100);
            }

            mHandler.postDelayed(updateUI, 200);
            mHandler.postDelayed(updateImage, 200);

            if(UtilFunctions.currentVersionSupportLockScreenControls()){
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean skipToPreviousTrack(Boolean isgetAction) {
        boolean lastisPlay=isPlay;
        isPlay=true;
        try {
            if (getCurrentMediaPlayer().getCurrentPosition() > 8000 && !isgetAction) {
                getCurrentMediaPlayer().seekTo(0);
                return true;
            }

        } catch (Exception e) {
            return false;
        }


        try {

            getMediaPlayer().reset();
            getMediaPlayer2().reset();
            clearCrossfadeCallbacks();

            getHandler().removeCallbacks(crossFadeRunnable);
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);
            int lastSong=0;
            if (isgetAction && PlayerConstants.lastDuration>8000) {}
            else
            {
                lastSong=PlayerConstants.SONG_NUMBER;
                decrementCurrentSongIndex();
            }

            mFirstRun = true;
            prepareMediaPlayer(PlayerConstants.SONG_NUMBER);
            MainActivity.setSongPlay(lastSong, PlayerConstants.SONG_NUMBER);
            updateNotification();
            UpdateMetadata();
            if (!lastisPlay)
            {
                mHandler.postDelayed(changeButton,100);
            }

            mHandler.postDelayed(updateUI, 200);
            mHandler.postDelayed(updateImage, 200);

            if(UtilFunctions.currentVersionSupportLockScreenControls()){
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            }
        } catch (Exception e) {

            return false;
        }

        return true;
    }

    public boolean skipToTrack(int trackIndex) {
        try {
            boolean lastisPlay=isPlay;
            isPlay=true;
            getMediaPlayer().reset();
            getMediaPlayer2().reset();
            clearCrossfadeCallbacks();

            getHandler().removeCallbacks(crossFadeRunnable);
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);


            mFirstRun = true;
            prepareMediaPlayer(trackIndex);
            UpdateMetadata();
            if (!lastisPlay)
            {

                mHandler.postDelayed(changeButton, 100);
            }

            mHandler.postDelayed(updateUI, 200);
            mHandler.postDelayed(updateImage, 200);

            if(UtilFunctions.currentVersionSupportLockScreenControls()){
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING);
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void IflastSong(){
        try {
            isPlay=false;
            if(UtilFunctions.currentVersionSupportLockScreenControls()){
                remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED);
            }

            getMediaPlayer().reset();
            getMediaPlayer2().reset();

            PlayerConstants.SONG_NUMBER=0;

            mHandler.post(updateImage);
            mHandler.post(changeButton);
            mHandler.post(updateUI);

        }
        catch (Exception e){}
    }

    private void clearCrossfadeCallbacks() {
        if (mHandler==null)
            return;

        mHandler.removeCallbacks(startCrossFadeRunnable);
        mHandler.removeCallbacks(crossFadeRunnable);

        try {
            getMediaPlayer().setVolume(1.0f, 1.0f);
            getMediaPlayer2().setVolume(1.0f, 1.0f);
        } catch (IllegalStateException e) {

        }

    }

    public int incrementCurrentSongIndex() {
        if ((PlayerConstants.SONG_NUMBER+1) < PlayerConstants.SONGS_LIST.size())
            PlayerConstants.SONG_NUMBER++;

        return PlayerConstants.SONG_NUMBER;
    }

    public int decrementCurrentSongIndex() {
        if ((PlayerConstants.SONG_NUMBER-1) > -1)
            PlayerConstants.SONG_NUMBER--;

        return PlayerConstants.SONG_NUMBER;
    }

    private int determineNextSongIndex() {
        if (isAtEndOfQueue())
            return -1;
        else
            return (PlayerConstants.SONG_NUMBER + 1);
    }

    private Boolean isAtEndOfQueue() {
        if (PlayerConstants.SONGS_LIST.size() == (PlayerConstants.SONG_NUMBER+1)) {
            return true;
        }
        return false;
    }

    public boolean isMediaPlayerPrepared() {
        return mMediaPlayerPrepared;
    }

    public boolean isMediaPlayer2Prepared() {
        return mMediaPlayer2Prepared;
    }

    public boolean isPlayingMusic() {
        try {
            if (getCurrentMediaPlayer().isPlaying())
                return true;
            else
                return false;

        } catch (Exception e) {

            return false;
        }

    }

    public MediaPlayer getCurrentMediaPlayer() {
        if (mCurrentMediaPlayer == 1)
            return mMediaPlayer;
        else
            return mMediaPlayer2;
    }

    public MediaPlayer getMediaPlayer() {
        return mMediaPlayer;
    }

    public MediaPlayer getMediaPlayer2() {
        return mMediaPlayer2;
    }

    public Handler getHandler() {
        return mHandler;
    }

    public void setCurrentMediaPlayer(int currentMediaPlayer) {
        mCurrentMediaPlayer = currentMediaPlayer;
    }

    public void setIsMediaPlayerPrepared(boolean prepared) {
        mMediaPlayerPrepared = prepared;
    }

    public void setIsMediaPlayer2Prepared(boolean prepared) {
        mMediaPlayer2Prepared = prepared;
    }

    public void saveLastPosition(){
        if (PlayerConstants.SONG_NUMBER>-1) {
            UtilFunctions.saveSharedPreferenceint(getApplicationContext(), UtilFunctions.LastDuration, getCurrentMediaPlayer().getCurrentPosition());
            UtilFunctions.saveSharedPreferenceint(getApplicationContext(), UtilFunctions.LastSongNumber, PlayerConstants.SONG_NUMBER);
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        saveLastPosition();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        saveLastPosition();

        mFadeInVolume = 0.0f;
        mFadeOutVolume = 1.0f;

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(mNotificationId);

        if (mMediaPlayer!=null)
            mMediaPlayer.release();

        if (mMediaPlayer2!=null)
            getMediaPlayer2().release();

        mMediaPlayer = null;
        mMediaPlayer2 = null;

        mAudioManagerHelper.setHasAudioFocus(false);
        mAudioManager.abandonAudioFocus(audioFocusChangeListener);
        mAudioManager = null;
    }
}

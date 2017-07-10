package com.surajsararf.musicoplayer.AsyncTasks;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;

import com.surajsararf.musicoplayer.Custom.MediaStoreAccessHelper;
import com.surajsararf.musicoplayer.util.MediaItem;
import com.surajsararf.musicoplayer.util.PlayerConstants;

import java.util.ArrayList;


/**
 * Created by surajsararf on 28/2/16.
 */
public class AsyncGetSongList extends AsyncTask<Void,Void,ArrayList<MediaItem>> {
    private Context mContext;
    public ArrayList<OnBuildLibraryProgressUpdate> mBuildLibraryProgressUpdate;
    public AsyncGetSongList(Context context){
        mContext=context;
        mBuildLibraryProgressUpdate = new ArrayList<OnBuildLibraryProgressUpdate>();
    }

    @Override
    protected void onPreExecute() {
        if (mBuildLibraryProgressUpdate!=null)
            for (int i=0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i)!=null)
                    mBuildLibraryProgressUpdate.get(i).onStartBuildingLibrary();
    }

    public interface OnBuildLibraryProgressUpdate {

        public void onStartBuildingLibrary();

        public void onFinishBuildingLibrary(AsyncGetSongList task);

    }
	
    @Override
    protected ArrayList<MediaItem> doInBackground(Void... params) {
        final String sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC";
        Cursor mCursor = MediaStoreAccessHelper.getAllSongs(mContext,null,sortOrder);
        ArrayList<MediaItem> listOfSongs = new ArrayList<MediaItem>();
        mCursor.moveToFirst();
        final int titleColIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        final int artistColIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        final int albumColIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        final int durationColIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        final int filePathColIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        final int albumIdColIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");

        do {
            MediaItem songData = new MediaItem();

            String title = mCursor.getString(titleColIndex);
            String artist = mCursor.getString(artistColIndex);
            String album = mCursor.getString(albumColIndex);
            long duration = mCursor.getLong(durationColIndex);
            String filePath= mCursor.getString(filePathColIndex);
            long albumId=mCursor.getLong(albumIdColIndex);
            Uri uri = ContentUris.withAppendedId(sArtworkUri, albumId);

            songData.setTitle(title);
            songData.setAlbum(album);
            songData.setArtist(artist);
            songData.setDuration(duration);
            songData.setFilepath(filePath);
            songData.setAlbumId(albumId);
            songData.setAlbumArtPath(uri);
            listOfSongs.add(songData);
        }while(mCursor.moveToNext());

        mCursor.close();
        return listOfSongs;
    }


    @Override
    protected void onPostExecute(ArrayList<MediaItem> mediaItems) {
        PlayerConstants.SONGS_LIST=mediaItems;

        //PlayerConstants.SONGSLOADING_HANDLER.sendMessage(PlayerConstants.SONGSLOADING_HANDLER.obtainMessage(0, "Complete"));
        if (mBuildLibraryProgressUpdate!=null)
            for (int i=0; i < mBuildLibraryProgressUpdate.size(); i++)
                if (mBuildLibraryProgressUpdate.get(i)!=null)
                    mBuildLibraryProgressUpdate.get(i).onFinishBuildingLibrary(this);
    }

    public void setOnBuildLibraryProgressUpdate(OnBuildLibraryProgressUpdate
                                                        buildLibraryProgressUpdate) {
        if (buildLibraryProgressUpdate!=null)
            mBuildLibraryProgressUpdate.add(buildLibraryProgressUpdate);
    }

}

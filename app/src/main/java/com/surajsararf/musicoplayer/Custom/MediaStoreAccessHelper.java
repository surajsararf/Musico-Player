
package com.surajsararf.musicoplayer.Custom;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;


public class MediaStoreAccessHelper {


	public static final String ALBUM_ARTIST = "album_artist";


	public static Cursor getAllSongsWithSelection(Context context, 
												  String selection, 
												  String[] projection, 
												  String sortOrder) {
		
		ContentResolver contentResolver = context.getContentResolver();
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		
		return contentResolver.query(uri, projection, selection, null, sortOrder);
		
	}
	

	public static Cursor getAllSongs(Context context, 
									 String[] projection, 
									 String sortOrder) {
		
		ContentResolver contentResolver = context.getContentResolver();
		Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
		String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
		return contentResolver.query(uri, null, selection, null, sortOrder);
		
	}
	

	public static Cursor getAllUniqueArtists(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		String[] projection = { MediaStore.Audio.Artists._ID, 
							    MediaStore.Audio.Artists.ARTIST, 
							    MediaStore.Audio.Artists.NUMBER_OF_ALBUMS };
		
		return contentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, 
									 projection, 
									 null, 
									 null, 
									 MediaStore.Audio.Artists.ARTIST + " ASC");
		
	}
	

	public static Cursor getAllUniqueAlbums(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		String[] projection = { MediaStore.Audio.Albums._ID, 
							    MediaStore.Audio.Albums.ALBUM, 
							    MediaStore.Audio.Albums.NUMBER_OF_SONGS };
		
		return contentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, 
									 projection, 
									 null, 
									 null, 
									 MediaStore.Audio.Albums.ALBUM + " ASC");
		
	}
	

	public static Cursor getAllUniqueGenres(Context context) {
		ContentResolver contentResolver = context.getContentResolver();
		String[] projection = { MediaStore.Audio.Genres._ID, 
							    MediaStore.Audio.Genres.NAME };
		
		return contentResolver.query(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, 
									 projection, 
									 null, 
									 null, 
									 MediaStore.Audio.Genres.NAME + " ASC");
		
	}


    public static Cursor getAllUniquePlaylists(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        String[] projection = { MediaStore.Audio.Playlists._ID,
                                MediaStore.Audio.Playlists.NAME };

        return contentResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                                     projection,
                                     null,
                                     null,
                                     MediaStore.Audio.Playlists.NAME + " ASC");

    }
	
}

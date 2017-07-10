package com.surajsararf.musicoplayer.util;

import com.surajsararf.musicoplayer.service.SongPlayback;

import java.util.ArrayList;

public class PlayerConstants {
	//List of Songs
	public static ArrayList<MediaItem> SONGS_LIST = new ArrayList<MediaItem>();
	//song number which is playing right now from SONGS_LIST
	public static int SONG_NUMBER = -1;

	public static SongPlayback mSongPlayback;

	public static int lastDuration=0;

}

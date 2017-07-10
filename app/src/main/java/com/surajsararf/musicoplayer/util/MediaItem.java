package com.surajsararf.musicoplayer.util;


import android.net.Uri;

public class MediaItem {
	String title;
	String artist;
	String album;
	String filepath;
	long duration;
	long albumId;
	Uri AlbumArtPath;

	@Override
	public String toString() {
		return title;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public String getFilepath() {
		return filepath;
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public long getAlbumId() {
		return albumId;
	}

	public void setAlbumId(long albumId) {
		this.albumId = albumId;
	}

	public void setAlbumArtPath(Uri albumArtPath) {
		AlbumArtPath = albumArtPath;
	}

	public Uri getAlbumArtPath() {
		return AlbumArtPath;
	}
}

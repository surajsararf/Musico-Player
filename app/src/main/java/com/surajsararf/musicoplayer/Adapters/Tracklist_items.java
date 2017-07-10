package com.surajsararf.musicoplayer.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.surajsararf.musicoplayer.Custom.GetValues;
import com.surajsararf.musicoplayer.R;
import com.surajsararf.musicoplayer.util.MediaItem;
import com.surajsararf.musicoplayer.util.PlayerConstants;

import java.util.ArrayList;

/**
 * Created by surajsararf on 17/2/16.
 */
public class Tracklist_items extends RecyclerView.Adapter<Tracklist_items.MyViewHolder> {
    private GetValues getValues;
    private Context context;
    private ArrayList<MediaItem> mItemsList;

    public Tracklist_items(Context context,ArrayList<MediaItem> items){
        getValues=new GetValues(context);
        mItemsList=items;
        this.context=context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.tracklist_items,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        MediaItem detail=mItemsList.get(position);
        holder.SongName.setText(detail.getTitle());
        holder.ArtistAlbumName.setText(detail.getArtist() + " | " + detail.getAlbum());
        if (PlayerConstants.mSongPlayback.isPlay){
            holder.isPlayImage.setImageResource(R.drawable.pause);
        }
        else
        {
            holder.isPlayImage.setImageResource(R.drawable.play);
        }

        holder.isPlayImage.setVisibility(View.INVISIBLE);
        if (PlayerConstants.SONG_NUMBER>-1 && PlayerConstants.SONG_NUMBER==position)
        {
            holder.isPlayImage.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return mItemsList.size();
    }
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView SongName, ArtistAlbumName;
        public ImageView isPlayImage;
        public MyViewHolder(View itemView) {
            super(itemView);
            SongName= (TextView) itemView.findViewById(R.id.songname);
            ArtistAlbumName = (TextView) itemView.findViewById(R.id.artist_album_name);
            isPlayImage= (ImageView) itemView.findViewById(R.id.isplay);
        }
    }
}

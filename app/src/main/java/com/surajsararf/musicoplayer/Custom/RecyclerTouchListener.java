package com.surajsararf.musicoplayer.Custom;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by surajsararf on 23/1/16.
 */
public class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {
    private GestureDetector mGestureDetector;
    private ClickListener clickListener;
    public RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener){
        this.clickListener=clickListener;
        mGestureDetector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener(){
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                View child=recyclerView.findChildViewUnder(e.getX(),e.getY());
                if(child!=null && clickListener!=null)
                {
                    clickListener.onLongClick(child,recyclerView.getChildAdapterPosition(child));
                }
            }
        });
    }
    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        View child=rv.findChildViewUnder(e.getX(),e.getY());
        if(child!=null && clickListener!=null && mGestureDetector.onTouchEvent(e))
        {
            clickListener.onClick(child,rv.getChildAdapterPosition(child));
        }
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }
    public static interface ClickListener{
        public void onClick(View view, int position);
        public void onLongClick(View view, int position);
    }
}

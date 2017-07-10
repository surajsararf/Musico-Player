package com.surajsararf.musicoplayer.Custom;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

/**
 * Created by surajsararf on 13/2/16.
 */
public class GetValues {
    private Context context;
    private DisplayMetrics metrics;
    private int[] location={0,1};
    public GetValues(Context context)
    {
        this.context=context;
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
    }
    public int GetStatusBarHeight() {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    public int dpToPx(int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int px = Math.round(dp * (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT));
        return px;
    }
    public int ScreenWidth(){
        return metrics.widthPixels;
    }
    public float ScreenHeight(){
        return metrics.heightPixels;
    }
    public int getRelativeLeft(View view){
//        if (view.getParent()==view.getRootView())
//            return view.getLeft();
//        else
//            return view.getLeft()+getRelativeLeft((View) view.getParent());
        view.getLocationOnScreen(location);
        return location[0];
    }
    public int getRelativeTop(View view){
        if (view.getParent()==view.getRootView())
            return view.getTop();
        else
            return view.getTop()+getRelativeTop((View) view.getParent());
//        view.getLocationOnScreen(location);
//        return location[1];
    }
}

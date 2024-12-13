package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;
import android.view.View;

public interface TouchEventProcessor {
    boolean processTouchEvent(MotionEvent motionEvent, View view);
    void cancelPendingActions();
}

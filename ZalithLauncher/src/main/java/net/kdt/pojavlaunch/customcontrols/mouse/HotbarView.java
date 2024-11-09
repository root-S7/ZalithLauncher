package net.kdt.pojavlaunch.customcontrols.mouse;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.movtery.zalithlauncher.event.single.MCOptionChangeEvent;
import com.movtery.zalithlauncher.event.single.RefreshHotbarEvent;
import com.movtery.zalithlauncher.setting.AllSettings;

import net.kdt.pojavlaunch.LwjglGlfwKeycode;
import net.kdt.pojavlaunch.utils.MCOptionUtils;
import net.kdt.pojavlaunch.utils.MathUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.lwjgl.glfw.CallbackBridge;

public class HotbarView extends View implements View.OnLayoutChangeListener, Runnable {
    private final TapDetector mDoubleTapDetector = new TapDetector(2, TapDetector.DETECTION_METHOD_DOWN);
    private View mParentView;
    private static final int[] HOTBAR_KEYS = {
            LwjglGlfwKeycode.GLFW_KEY_1, LwjglGlfwKeycode.GLFW_KEY_2,   LwjglGlfwKeycode.GLFW_KEY_3,
            LwjglGlfwKeycode.GLFW_KEY_4, LwjglGlfwKeycode.GLFW_KEY_5,   LwjglGlfwKeycode.GLFW_KEY_6,
            LwjglGlfwKeycode.GLFW_KEY_7, LwjglGlfwKeycode.GLFW_KEY_8, LwjglGlfwKeycode.GLFW_KEY_9};
    private final DropGesture mDropGesture = new DropGesture(new Handler(Looper.getMainLooper()));
    private float mScaleFactor = AllSettings.getResolutionRatio() / 100f;
    private int mWidth;
    private int mLastIndex;
    private int mGuiScale;

    public HotbarView(Context context) {
        super(context);
    }

    public HotbarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressWarnings("unused") // You suggested me this constructor, Android
    public HotbarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void refreshScaleFactor(float scaleFactor) {
        this.mScaleFactor = scaleFactor;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        EventBus.getDefault().register(this);
        ViewParent parent = getParent();
        if(parent == null) return;
        if(parent instanceof View) {
            mParentView = (View) parent;
            mParentView.addOnLayoutChangeListener(this);
        }
        mGuiScale = MCOptionUtils.getMcScale();
        repositionView();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        EventBus.getDefault().unregister(this);
    }

    /**
     * 在Hotbar刷新事件被监听到时，会刷新判定
     * @param event 刷新事件
     */
    @Subscribe
    public void event(RefreshHotbarEvent event) {
        post(this);
    }

    /**
     * 当options.txt文件变更时，会刷新判定，因为需要检查gui尺寸
     * @param event 刷新事件
     */
    @Subscribe
    public void event(MCOptionChangeEvent event) {
        post(this);
    }

    private void repositionView() {
        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if(!(layoutParams instanceof ViewGroup.MarginLayoutParams))
            throw new RuntimeException("Incorrect LayoutParams type, expected ViewGroup.MarginLayoutParams");
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) layoutParams;
        int height;
        marginLayoutParams.width = mWidth = mcScale(180);
        marginLayoutParams.height = height = mcScale(20);
        marginLayoutParams.leftMargin = (CallbackBridge.physicalWidth / 2) - (mWidth / 2);
        marginLayoutParams.topMargin = CallbackBridge.physicalHeight - height;
        setLayoutParams(marginLayoutParams);
    }

    @SuppressWarnings("ClickableViewAccessibility") // performClick does not report coordinates.
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(!CallbackBridge.isGrabbing()) return false;
        boolean hasDoubleTapped = mDoubleTapDetector.onTouchEvent(event);

        // Check if we need to cancel the drop event
        int actionMasked = event.getActionMasked();
        if(isLastEventInGesture(actionMasked)) mDropGesture.cancel();
        else mDropGesture.submit();
        // Determine the hotbar slot
        float x = event.getX();
        // Ignore positions equal to mWidth because they would translate into an out-of-bounds hotbar index
        if(x < 0 || x >= mWidth) {
            // If out of bounds, cancel the hotbar gesture to avoid dropping items on last hotbar slots
            mDropGesture.cancel();
            return true;
        }
        int hotbarIndex = (int)MathUtils.map(x, 0, mWidth, 0, HOTBAR_KEYS.length);
        // Check if the slot changed and we need to make a key press
        if(hotbarIndex == mLastIndex) {
            // Only check for doubletapping if the slot has not changed
            if(hasDoubleTapped && !AllSettings.getDisableDoubleTap()) CallbackBridge.sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_F);
            return true;
        }
        mLastIndex = hotbarIndex;
        int hotbarKey = HOTBAR_KEYS[hotbarIndex];
        CallbackBridge.sendKeyPress(hotbarKey);
        // Cancel the event since we changed hotbar slots.
        mDropGesture.cancel();
        // Only resubmit the gesture only if it isn't the last event we will receive.
        if(!isLastEventInGesture(actionMasked)) mDropGesture.submit();
        return true;
    }

    private boolean isLastEventInGesture(int actionMasked) {
        return actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL;
    }

    private int mcScale(int input) {
        return (int)((mGuiScale * input)/ mScaleFactor);
    }

    @Override
    public void run() {
        if(getParent() == null) return;
        int scale = MCOptionUtils.getMcScale();
        if(scale == mGuiScale) return;
        mGuiScale = scale;
        repositionView();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        // We need to check whether dimensions match or not because here we are looking specifically for changes of dimensions
        // and Android keeps calling this without dimensions actually changing for some reason.
        if(v.equals(mParentView) && (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom)) {
            // Need to post this, because it is not correct to resize the view
            // during a layout pass.
            post(this::repositionView);
        }
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(Color.argb(128, 0, 0, 0));
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), paint);
    }
}

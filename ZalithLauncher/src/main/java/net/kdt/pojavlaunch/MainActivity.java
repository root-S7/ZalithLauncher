package net.kdt.pojavlaunch;

import static net.kdt.pojavlaunch.Tools.currentDisplayMetrics;
import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;
import static org.lwjgl.glfw.CallbackBridge.windowHeight;
import static org.lwjgl.glfw.CallbackBridge.windowWidth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.databinding.ActivityGameBinding;
import com.movtery.zalithlauncher.databinding.ViewControlMenuBinding;
import com.movtery.zalithlauncher.databinding.ViewGameMenuBinding;
import com.movtery.zalithlauncher.event.single.RefreshHotbarEvent;
import com.movtery.zalithlauncher.event.value.HotbarChangeEvent;
import com.movtery.zalithlauncher.feature.ProfileLanguageSelector;
import com.movtery.zalithlauncher.feature.background.BackgroundManager;
import com.movtery.zalithlauncher.feature.background.BackgroundType;
import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.feature.version.Version;
import com.movtery.zalithlauncher.feature.version.VersionInfo;
import com.movtery.zalithlauncher.launch.LaunchGame;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.setting.AllStaticSettings;
import com.movtery.zalithlauncher.setting.Settings;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.activity.BaseActivity;
import com.movtery.zalithlauncher.ui.dialog.ControlSettingsDialog;
import com.movtery.zalithlauncher.ui.dialog.KeyboardDialog;
import com.movtery.zalithlauncher.ui.dialog.SelectControlsDialog;
import com.movtery.zalithlauncher.ui.dialog.SelectMouseDialog;
import com.movtery.zalithlauncher.ui.fragment.settings.VideoSettingsFragment;
import com.movtery.zalithlauncher.ui.subassembly.adapter.ObjectSpinnerAdapter;
import com.movtery.zalithlauncher.ui.subassembly.hotbar.HotbarType;
import com.movtery.zalithlauncher.ui.subassembly.hotbar.HotbarUtils;
import com.movtery.zalithlauncher.ui.subassembly.view.GameMenuViewWrapper;
import com.movtery.zalithlauncher.utils.PathAndUrlManager;
import com.movtery.zalithlauncher.utils.ZHTools;
import com.movtery.zalithlauncher.utils.anim.AnimUtils;
import com.movtery.zalithlauncher.utils.file.FileTools;
import com.movtery.zalithlauncher.utils.stringutils.StringUtils;
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener;

import net.kdt.pojavlaunch.customcontrols.ControlButtonMenuListener;
import net.kdt.pojavlaunch.customcontrols.ControlData;
import net.kdt.pojavlaunch.customcontrols.ControlDrawerData;
import net.kdt.pojavlaunch.customcontrols.ControlJoystickData;
import net.kdt.pojavlaunch.customcontrols.ControlLayout;
import net.kdt.pojavlaunch.customcontrols.CustomControls;
import net.kdt.pojavlaunch.customcontrols.EditorExitable;
import net.kdt.pojavlaunch.customcontrols.keyboard.LwjglCharSender;
import net.kdt.pojavlaunch.customcontrols.keyboard.TouchCharInput;
import net.kdt.pojavlaunch.customcontrols.mouse.GyroControl;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.services.GameService;
import net.kdt.pojavlaunch.utils.MCOptionUtils;

import org.greenrobot.eventbus.EventBus;
import org.lwjgl.glfw.CallbackBridge;

import java.io.File;
import java.io.IOException;

public class MainActivity extends BaseActivity implements ControlButtonMenuListener, EditorExitable, ServiceConnection {
    public static volatile ClipboardManager GLOBAL_CLIPBOARD;
    public static final String INTENT_VERSION = "intent_version";

    volatile public static boolean isInputStackCall;

    @SuppressLint("StaticFieldLeak")
    private static ActivityGameBinding binding = null;
    private GameMenuViewWrapper mGameMenuWrapper;
    private GyroControl mGyroControl;
    private KeyboardDialog keyboardDialog;
    public static TouchCharInput touchCharInput;
    public static ControlLayout mControlLayout;

    Version minecraftVersion;

    private ViewGameMenuBinding mGameMenuBinding;
    private ViewControlMenuBinding mControlSettingsBinding;
    private GameService.LocalBinder mServiceBinder;
    boolean isInEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        minecraftVersion = getIntent().getParcelableExtra(INTENT_VERSION);
        if (minecraftVersion == null) throw new RuntimeException("The game version is not selected!");

        MCOptionUtils.load(minecraftVersion.getGameDir().getAbsolutePath());
        if (AllSettings.getAutoSetGameLanguage()) ProfileLanguageSelector.setGameLanguage(minecraftVersion, AllSettings.getGameLanguageOverridden());

        Intent gameServiceIntent = new Intent(this, GameService.class);
        // Start the service a bit early
        ContextCompat.startForegroundService(this, gameServiceIntent);
        initLayout();
        CallbackBridge.addGrabListener(binding.mainTouchpad);
        CallbackBridge.addGrabListener(binding.mainGameRenderView);
        mGyroControl = new GyroControl(this);

        Window window = getWindow();
        // Enabling this on TextureView results in a broken white result
        if(AllSettings.getAlternateSurface()) window.setBackgroundDrawable(null);
        else window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        // Set the sustained performance mode for available APIs
        window.setSustainedPerformanceMode(AllSettings.getSustainedPerformance());

        // 防止系统息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ControlLayout controlLayout = binding.mainControlLayout;
        mControlSettingsBinding = ViewControlMenuBinding.inflate(getLayoutInflater());
        new ControlSettingsClickListener(mControlSettingsBinding, controlLayout);
        mControlSettingsBinding.export.setVisibility(View.GONE);

        binding.mainControlLayout.setModifiable(false);

        //Now, attach to the service. The game will only start when this happens, to make sure that we know the right state.
        bindService(gameServiceIntent, this, 0);
    }

    protected void initLayout() {
        binding = ActivityGameBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mGameMenuWrapper = new GameMenuViewWrapper(this, v -> onClickedMenu());
        touchCharInput = binding.mainTouchCharInput;
        mControlLayout = binding.mainControlLayout;

        BackgroundManager.setBackgroundImage(this, BackgroundType.IN_GAME, findViewById(R.id.background_view));

        keyboardDialog = new KeyboardDialog(this).setShowSpecialButtons(false);

        binding.mainControlLayout.setMenuListener(this);

        binding.mainDrawerOptions.setScrimColor(Color.TRANSPARENT);
        binding.mainDrawerOptions.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        try {
            File latestLogFile = new File(PathAndUrlManager.DIR_GAME_HOME, "latestlog.txt");
            if(!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw new IOException("Failed to create a new log file");
            Logger.begin(latestLogFile.getAbsolutePath());
            // FIXME: is it safe for multi thread?
            GLOBAL_CLIPBOARD = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            binding.mainTouchCharInput.setCharacterSender(new LwjglCharSender());

            Logging.i("RdrDebug","__P_renderer=" + minecraftVersion.getRenderer());
            Tools.LOCAL_RENDERER = minecraftVersion.getRenderer();

            setTitle("Minecraft " + minecraftVersion.getVersionName());

            // Minecraft 1.13+
            JMinecraftVersionList.Version mVersionInfo = Tools.getVersionInfo(minecraftVersion);
            isInputStackCall = mVersionInfo.arguments != null;
            CallbackBridge.nativeSetUseInputStackQueue(isInputStackCall);

            Tools.getDisplayMetrics(this);
            windowWidth = Tools.getDisplayFriendlyRes(currentDisplayMetrics.widthPixels, 1f);
            windowHeight = Tools.getDisplayFriendlyRes(currentDisplayMetrics.heightPixels, 1f);

            // Menu
            mGameMenuBinding = ViewGameMenuBinding.inflate(getLayoutInflater());
            MenuSettingsInitListener menuSettingsInitListener = new MenuSettingsInitListener(mGameMenuBinding);

            binding.mainNavigationView.removeAllViews();
            binding.mainNavigationView.addView(mGameMenuBinding.getRoot());

            binding.mainDrawerOptions.addDrawerListener(menuSettingsInitListener);
            binding.mainDrawerOptions.closeDrawers();

            binding.mainGameRenderView.setSurfaceReadyListener(() -> {
                try {
                    // Setup virtual mouse right before launching
                    if (AllSettings.getVirtualMouseStart()) {
                        binding.mainTouchpad.post(() -> binding.mainTouchpad.switchState());
                    }
                    LaunchGame.runGame(this, mServiceBinder, minecraftVersion, mVersionInfo);
                } catch (Throwable e) {
                    Tools.showErrorRemote(e);
                }
            });

            if (AllSettings.getEnableLogOutput()) openLogOutput();

            String mcInfo = "";
            VersionInfo versionInfo = minecraftVersion.getVersionInfo();
            if (versionInfo != null) {
                mcInfo = versionInfo.getInfoString();
            }
            String tipString = StringUtils.insertNewline(
                    binding.gameTip.getText(),
                    StringUtils.insertSpace(getString(R.string.game_tip_version), minecraftVersion.getVersionName())
            );
            if (!mcInfo.isEmpty()) {
                tipString = StringUtils.insertNewline(tipString, StringUtils.insertSpace(getString(R.string.game_tip_mc_info), mcInfo));
            }
            binding.gameTip.setText(tipString);
            AnimUtils.setVisibilityAnim(binding.gameTipView, 1000, true, 300, new AnimUtils.AnimationListener() {
                @Override
                public void onStart() {
                }
                @Override
                public void onEnd() {
                    AnimUtils.setVisibilityAnim(binding.gameTipView, 15000, false, 300, null);
                }
            });
        } catch (Throwable e) {
            Tools.showError(this, e, true);
        }
    }

    private void loadControls() {
        try {
            // Load keys
            binding.mainControlLayout.loadLayout(minecraftVersion.getControl());
        } catch(IOException e) {
            try {
                Logging.w("MainActivity", "Unable to load the control file, loading the default now", e);
                binding.mainControlLayout.loadLayout((String) null);
            } catch (IOException ioException) {
                Tools.showError(this, ioException);
            }
        } catch (Throwable th) {
            Tools.showError(this, th);
        }
        mGameMenuWrapper.setVisibility(!binding.mainControlLayout.hasMenuButton());
        binding.mainControlLayout.toggleControlVisible();
    }

    private void openLogOutput() {
        MainActivity.binding.mainLoggerView.setVisibilityWithAnim(true);
    }

    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
        loadControls();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set the activity for the executor. Must do this here, or else Tools.showErrorRemote() may not
        // execute the correct method
        ContextExecutor.setActivity(this);
        if (AllStaticSettings.enableGyro) mGyroControl.enable();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1);
    }

    @Override
    protected void onPause() {
        mGyroControl.disable();
        if (CallbackBridge.isGrabbing()){
            sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
        }
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 0);
        super.onPause();
    }

    @Override
    protected void onStart() {
        super.onStart();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 1);
    }

    @Override
    protected void onStop() {
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_VISIBLE, 0);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        CallbackBridge.removeGrabListener(binding.mainTouchpad);
        CallbackBridge.removeGrabListener(binding.mainGameRenderView);
        ContextExecutor.clearActivity();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        mGyroControl.updateOrientation();
        Tools.updateWindowSize(this);
        binding.mainGameRenderView.refreshSize();
        runOnUiThread(() -> binding.mainControlLayout.refreshControlButtonPositions());
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        TaskExecutors.getUIHandler().postDelayed(() -> binding.mainGameRenderView.refreshSize(), 500);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            try {
                binding.mainControlLayout.loadLayout((String) null);
            } catch (IOException e) {
                Logging.e("LoadLayout", Tools.printToString(e));
            }
        }
    }

    @Override
    public boolean shouldIgnoreNotch() {
        return AllSettings.getIgnoreNotch();
    }

    public static void toggleMouse(Context ctx) {
        if (CallbackBridge.isGrabbing()) return;

        if (binding != null) {
            Toast.makeText(ctx, binding.mainTouchpad.switchState()
                            ? R.string.control_mouseon : R.string.control_mouseoff,
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(isInEditor) {
            if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                if(event.getAction() == KeyEvent.ACTION_DOWN) binding.mainControlLayout.askToExit(this);
                return true;
            }
            return super.dispatchKeyEvent(event);
        }
        boolean handleEvent;
        if(!(handleEvent = binding.mainGameRenderView.processKeyEvent(event))) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && !binding.mainTouchCharInput.isEnabled()) {
                if(event.getAction() != KeyEvent.ACTION_UP) return true; // We eat it anyway
                sendKeyPress(LwjglGlfwKeycode.GLFW_KEY_ESCAPE);
                return true;
            }
        }
        return handleEvent;
    }

    public static void switchKeyboardState() {
        if (binding != null) binding.mainTouchCharInput.switchKeyboardState();
    }

    private static void setUri(Context context, String input) {
        if(input.startsWith("file:")) {
            int truncLength = 5;
            if(input.startsWith("file://")) truncLength = 7;
            input = input.substring(truncLength);
            Logging.i("MainActivity", input);

            File inputFile = new File(input);
            FileTools.shareFile(context, inputFile);
            Logging.i("In-game Share File/Folder", "Start!");
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(input), "*/*");
            context.startActivity(intent);
        }
    }

    public static void openLink(String link) {
        Context ctx = binding.mainTouchpad.getContext(); // no more better way to obtain a context statically
        ((Activity)ctx).runOnUiThread(() -> {
            try {
                setUri(ctx, link);
            } catch (Throwable th) {
                Tools.showError(ctx, th);
            }
        });
    }

    public static void querySystemClipboard() {
        TaskExecutors.runInUIThread(()->{
            ClipData clipData = GLOBAL_CLIPBOARD.getPrimaryClip();
            if(clipData == null) {
                AWTInputBridge.nativeClipboardReceived(null, null);
                return;
            }
            ClipData.Item firstClipItem = clipData.getItemAt(0);
            //TODO: coerce to HTML if the clip item is styled
            CharSequence clipItemText = firstClipItem.getText();
            if(clipItemText == null) {
                AWTInputBridge.nativeClipboardReceived(null, null);
                return;
            }
            AWTInputBridge.nativeClipboardReceived(clipItemText.toString(), "plain");
        });
    }

    public static void putClipboardData(String data, String mimeType) {
        TaskExecutors.runInUIThread(()-> {
            ClipData clipData = null;
            switch(mimeType) {
                case "text/plain":
                    clipData = ClipData.newPlainText("AWT Paste", data);
                    break;
                case "text/html":
                    clipData = ClipData.newHtmlText("AWT Paste", data, data);
            }
            if(clipData != null) GLOBAL_CLIPBOARD.setPrimaryClip(clipData);
        });
    }

    @Override
    public void onClickedMenu() {
        DrawerLayout drawerLayout = binding.mainDrawerOptions;
        View navigationView = binding.mainNavigationView;

        boolean open = drawerLayout.isDrawerOpen(navigationView);
        if (open) drawerLayout.closeDrawer(navigationView);
        else drawerLayout.openDrawer(navigationView);

        navigationView.requestLayout();
    }

    @Override
    public void exitEditor() {
        try {
            MainActivity.binding.mainControlLayout.loadLayout((CustomControls)null);
            MainActivity.binding.mainControlLayout.setModifiable(false);
            System.gc();
            MainActivity.binding.mainControlLayout.loadLayout(minecraftVersion.getControl());
            mGameMenuWrapper.setVisibility(!binding.mainControlLayout.hasMenuButton());
        } catch (IOException e) {
            Tools.showError(this,e);
        }
        binding.mainNavigationView.removeAllViews();
        binding.mainNavigationView.addView(mGameMenuBinding.getRoot());
        isInEditor = false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        GameService.LocalBinder localBinder = (GameService.LocalBinder) service;
        mServiceBinder = localBinder;
        binding.mainGameRenderView.start(localBinder.isActive, binding.mainTouchpad);
        localBinder.isActive = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    /*
     * Android 14 (or some devices, at least) seems to dispatch the the captured mouse events as trackball events
     * due to a bug(?) somewhere(????)
     */
    private boolean checkCaptureDispatchConditions(MotionEvent event) {
        int eventSource = event.getSource();
        // On my device, the mouse sends events as a relative mouse device.
        // Not comparing with == here because apparently `eventSource` is a mask that can
        // sometimes indicate multiple sources, like in the case of InputDevice.SOURCE_TOUCHPAD
        // (which is *also* an InputDevice.SOURCE_MOUSE when controlling a cursor)
        return (eventSource & InputDevice.SOURCE_MOUSE_RELATIVE) != 0 ||
                (eventSource & InputDevice.SOURCE_MOUSE) != 0;
    }

    @Override
    public boolean dispatchTrackballEvent(MotionEvent ev) {
        if(checkCaptureDispatchConditions(ev))
            return binding.mainGameRenderView.dispatchCapturedPointerEvent(ev);
        else return super.dispatchTrackballEvent(ev);
    }

    private class MenuSettingsInitListener implements View.OnClickListener, SeekBar.OnSeekBarChangeListener, CompoundButton.OnCheckedChangeListener, OnSpinnerItemSelectedListener<HotbarType>, DrawerLayout.DrawerListener {
        private final ViewGameMenuBinding binding;

        public MenuSettingsInitListener(ViewGameMenuBinding binding) {
            this.binding = binding;
            //初始化状态
            this.binding.hotbarWidth.setMax(currentDisplayMetrics.widthPixels / 2);
            this.binding.hotbarHeight.setMax(currentDisplayMetrics.heightPixels / 2);

            //初始化Seekbar的值
            initSeekBarValue(this.binding.resolutionScaler, AllSettings.getResolutionRatio(), this.binding.resolutionScalerValue, "%");
            binding.resolutionScalerPreview.setText(VideoSettingsFragment.getResolutionRatioPreview(getResources(), AllSettings.getResolutionRatio()));
            initSeekBarValue(this.binding.timeLongPressTrigger, AllSettings.getTimeLongPressTrigger(), this.binding.timeLongPressTriggerValue, "ms");
            initSeekBarValue(this.binding.mouseSpeed, AllSettings.getMouseSpeed(), this.binding.mouseSpeedValue, "%");
            initSeekBarValue(this.binding.gyroSensitivity, AllSettings.getGyroSensitivity(), this.binding.gyroSensitivityValue, "%");
            initSeekBarValue(this.binding.hotbarHeight, AllSettings.getHotbarHeight(), this.binding.hotbarHeightValue, "px");
            initSeekBarValue(this.binding.hotbarWidth, AllSettings.getHotbarWidth(), this.binding.hotbarWidthValue, "px");

            //初始化Switch的状态
            this.binding.disableGestures.setChecked(AllSettings.getDisableGestures());
            this.binding.disableDoubleTap.setChecked(AllSettings.getDisableDoubleTap());
            this.binding.enableGyro.setChecked(AllSettings.getEnableGyro());
            this.binding.gyroInvertX.setChecked(AllSettings.getGyroInvertX());
            this.binding.gyroInvertY.setChecked(AllSettings.getGyroInvertY());

            refreshLayoutVisible(this.binding.timeLongPressTriggerLayout, !AllSettings.getDisableGestures());
            refreshLayoutVisible(this.binding.gyroLayout, AllSettings.getEnableGyro());

            //初始化点击事件
            this.binding.forceClose.setOnClickListener(this);
            this.binding.logOutput.setOnClickListener(this);
            this.binding.sendCustomKey.setOnClickListener(this);

            this.binding.resolutionScaler.setOnSeekBarChangeListener(this);
            this.binding.resolutionScalerRemove.setOnClickListener(this);
            this.binding.resolutionScalerAdd.setOnClickListener(this);

            this.binding.disableGestures.setOnCheckedChangeListener(this);
            this.binding.disableGesturesLayout.setOnClickListener(this);

            this.binding.disableDoubleTap.setOnCheckedChangeListener(this);
            this.binding.disableDoubleTapLayout.setOnClickListener(this);

            this.binding.timeLongPressTrigger.setOnSeekBarChangeListener(this);
            this.binding.timeLongPressTriggerRemove.setOnClickListener(this);
            this.binding.timeLongPressTriggerAdd.setOnClickListener(this);

            this.binding.mouseSpeed.setOnSeekBarChangeListener(this);
            this.binding.mouseSpeedRemove.setOnClickListener(this);
            this.binding.mouseSpeedAdd.setOnClickListener(this);

            this.binding.customMouse.setOnClickListener(this);
            this.binding.replacementCustomcontrol.setOnClickListener(this);
            this.binding.editControl.setOnClickListener(this);

            this.binding.enableGyro.setOnCheckedChangeListener(this);
            this.binding.enableGyroLayout.setOnClickListener(this);

            this.binding.gyroSensitivity.setOnSeekBarChangeListener(this);
            this.binding.gyroSensitivityRemove.setOnClickListener(this);
            this.binding.gyroSensitivityAdd.setOnClickListener(this);

            this.binding.gyroInvertX.setOnCheckedChangeListener(this);
            this.binding.gyroInvertXLayout.setOnClickListener(this);

            this.binding.gyroInvertY.setOnCheckedChangeListener(this);
            this.binding.gyroInvertYLayout.setOnClickListener(this);

            ObjectSpinnerAdapter<HotbarType> hotbarTypeAdapter = new ObjectSpinnerAdapter<>(
                    this.binding.hotbarType,
                    hotbarType -> getString(hotbarType.getNameId())
            );
            hotbarTypeAdapter.setItems(HotbarType.getEntries());
            this.binding.hotbarType.setSpinnerAdapter(hotbarTypeAdapter);
            this.binding.hotbarType.setOnSpinnerItemSelectedListener(this);
            this.binding.hotbarType.selectItemByIndex(HotbarUtils.getCurrentTypeIndex());

            this.binding.hotbarHeight.setOnSeekBarChangeListener(this);
            this.binding.hotbarHeightRemove.setOnClickListener(this);
            this.binding.hotbarHeightAdd.setOnClickListener(this);

            this.binding.hotbarWidth.setOnSeekBarChangeListener(this);
            this.binding.hotbarWidthRemove.setOnClickListener(this);
            this.binding.hotbarWidthAdd.setOnClickListener(this);
        }

        private void dialogSendCustomKey() {
            keyboardDialog.setOnKeycodeSelectListener(EfficientAndroidLWJGLKeycode::execKeyIndex).show();
        }

        private void replacementCustomControls() {
            SelectControlsDialog dialog = new SelectControlsDialog(MainActivity.this);
            dialog.setOnSelectedListener(file -> {
                try {
                    MainActivity.binding.mainControlLayout.loadLayout(file.getAbsolutePath());
                    //刷新：是否隐藏菜单按钮
                    mGameMenuWrapper.setVisibility(!MainActivity.binding.mainControlLayout.hasMenuButton());
                } catch (IOException ignored) {}
                dialog.dismiss();
            });
            dialog.show();
        }

        private void openCustomControls() {
            MainActivity.binding.mainControlLayout.setModifiable(true);
            MainActivity.binding.mainNavigationView.removeAllViews();
            MainActivity.binding.mainNavigationView.addView(mControlSettingsBinding.getRoot());
            mGameMenuWrapper.setVisibility(true);
            isInEditor = true;
        }

        @Override public void onClick(View v) {
            if (v == binding.forceClose) ZHTools.dialogForceClose(MainActivity.this);
            else if (v == binding.logOutput) openLogOutput();
            else if (v == binding.sendCustomKey) dialogSendCustomKey();
            else if (v == binding.resolutionScalerRemove) adjustSeekbar(binding.resolutionScaler, -1);
            else if (v == binding.resolutionScalerAdd) adjustSeekbar(binding.resolutionScaler, 1);
            else if (v == binding.disableGesturesLayout) toggleSwitchState(binding.disableGestures);
            else if (v == binding.disableDoubleTapLayout) toggleSwitchState(binding.disableDoubleTap);
            else if (v == binding.timeLongPressTriggerRemove) adjustSeekbar(binding.timeLongPressTrigger, -1);
            else if (v == binding.timeLongPressTriggerAdd) adjustSeekbar(binding.timeLongPressTrigger, 1);
            else if (v == binding.mouseSpeedRemove) adjustSeekbar(binding.mouseSpeed, -1);
            else if (v == binding.mouseSpeedAdd) adjustSeekbar(binding.mouseSpeed, 1);
            else if (v == binding.customMouse) new SelectMouseDialog(MainActivity.this, () -> MainActivity.binding.mainTouchpad.updateMouseDrawable()).show();
            else if (v == binding.replacementCustomcontrol) replacementCustomControls();
            else if (v == binding.editControl) openCustomControls();
            else if (v == binding.enableGyroLayout) toggleSwitchState(binding.enableGyro);
            else if (v == binding.gyroSensitivityRemove) adjustSeekbar(binding.gyroSensitivity, -1);
            else if (v == binding.gyroSensitivityAdd) adjustSeekbar(binding.gyroSensitivity, 1);
            else if (v == binding.gyroInvertXLayout) toggleSwitchState(binding.gyroInvertX);
            else if (v == binding.gyroInvertYLayout) toggleSwitchState(binding.gyroInvertY);
            else if (v == binding.hotbarWidthRemove) adjustSeekbar(binding.hotbarWidth, -1);
            else if (v == binding.hotbarWidthAdd) adjustSeekbar(binding.hotbarWidth, 1);
            else if (v == binding.hotbarHeightRemove) adjustSeekbar(binding.hotbarHeight, -1);
            else if (v == binding.hotbarHeightAdd) adjustSeekbar(binding.hotbarHeight, 1);
        }

        @Override
        @SuppressLint("SetTextI18n")
        public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
            if (s == binding.resolutionScaler) {
                Settings.Manager.put("resolutionRatio", progress).save();

                updateSeekbarValue(progress, binding.resolutionScalerValue, "%");
                binding.resolutionScalerPreview.setText(VideoSettingsFragment.getResolutionRatioPreview(getResources(), progress));

                AllStaticSettings.scaleFactor = progress / 100f;
                MainActivity.binding.mainGameRenderView.refreshSize();

                //当分辨率缩放的时候，需要刷新一下Hotbar的判定
                EventBus.getDefault().post(new RefreshHotbarEvent());
            } else if (s == binding.timeLongPressTrigger) {
                Settings.Manager.put("timeLongPressTrigger", progress).save();

                updateSeekbarValue(progress, binding.timeLongPressTriggerValue, "ms");
                AllStaticSettings.timeLongPressTrigger = progress;
            } else if (s == binding.mouseSpeed) {
                Settings.Manager.put("mousespeed", progress).save();

                updateSeekbarValue(progress, binding.mouseSpeedValue, "%");
            } else if (s == binding.gyroSensitivity) {
                Settings.Manager.put("gyroSensitivity", progress).save();

                updateSeekbarValue(progress, binding.gyroSensitivityValue, "%");
                AllStaticSettings.gyroSensitivity = progress;
            } else if (s == binding.hotbarWidth) {
                Settings.Manager.put("hotbarWidth", progress).save();

                updateSeekbarValue(progress, binding.hotbarWidthValue, "px");
                EventBus.getDefault().post(new HotbarChangeEvent(progress, binding.hotbarHeight.getProgress()));
            } else if (s == binding.hotbarHeight) {
                Settings.Manager.put("hotbarHeight", progress).save();

                updateSeekbarValue(progress, binding.hotbarHeightValue, "px");
                EventBus.getDefault().post(new HotbarChangeEvent(binding.hotbarWidth.getProgress(), progress));
            }
        }
        @Override public void onStartTrackingTouch(SeekBar s) {}
        @Override public void onStopTrackingTouch(SeekBar s) {}

        @Override public void onCheckedChanged(CompoundButton v, boolean isChecked) {
            if (v == binding.disableGestures) {
                refreshLayoutVisible(binding.timeLongPressTriggerLayout, !isChecked);
                Settings.Manager.put("disableGestures", isChecked).save();
            } else if (v == binding.disableDoubleTap) {
                Settings.Manager.put("disableDoubleTap", isChecked).save();
                AllStaticSettings.disableDoubleTap = isChecked;
            } else if (v == binding.enableGyro) {
                refreshLayoutVisible(binding.gyroLayout, isChecked);
                Settings.Manager.put("enableGyro", isChecked).save();
                //刷新陀螺仪的启用状态
                AllStaticSettings.enableGyro = isChecked;
                if (isChecked) mGyroControl.enable();
                else mGyroControl.disable();
            } else if (v == binding.gyroInvertX) {
                Settings.Manager.put("gyroInvertX", isChecked).save();
                AllStaticSettings.gyroInvertX = isChecked;
            } else if (v == binding.gyroInvertY) {
                Settings.Manager.put("gyroInvertY", isChecked).save();
                AllStaticSettings.gyroInvertY = isChecked;
            }
        }

        /**
         * 调整滑动条的值
         * @param seekBar 滑动条
         * @param v 需要调整的值的大小
         */
        private void adjustSeekbar(SeekBar seekBar, int v) {
            seekBar.setProgress(seekBar.getProgress() + v);
        }

        /**
         * 反转Switch当前的选中状态
         */
        @SuppressLint("UseSwitchCompatOrMaterialCode")
        private void toggleSwitchState(Switch switchView) {
            switchView.setChecked(!switchView.isChecked());
        }

        /**
         * 初始化Seekbar的值
         */
        private void initSeekBarValue(SeekBar seek, int value, TextView valueView, String suffix) {
            seek.setProgress(value);
            updateSeekbarValue(value, valueView, suffix);
        }

        /**
         * 更新Seekbar旁边数值的文本值
         */
        private void updateSeekbarValue(int value, TextView valueView, String suffix) {
            String valueText = value + " " + suffix;
            valueView.setText(valueText.trim());
        }

        /**
         * 刷新View的可见状态
         */
        private void refreshLayoutVisible(View view, boolean visible) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }

        @Override public void onItemSelected(int i, @Nullable HotbarType t, int i1, HotbarType t1) {
            if (t1 == HotbarType.AUTO) {
                binding.hotbarWidthLayout.setVisibility(View.GONE);
                binding.hotbarHeightLayout.setVisibility(View.GONE);
            } else if (t1 == HotbarType.MANUALLY) {
                binding.hotbarWidthLayout.setVisibility(View.VISIBLE);
                binding.hotbarHeightLayout.setVisibility(View.VISIBLE);
                binding.hotbarWidth.setProgress(AllSettings.getHotbarWidth());
                binding.hotbarHeight.setProgress(AllSettings.getHotbarHeight());
            }

            Settings.Manager.put("hotbarType", t1.getValueName()).save();
            EventBus.getDefault().post(new RefreshHotbarEvent());
        }
        @Override public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {}
        @Override public void onDrawerOpened(@NonNull View drawerView) {}
        @Override public void onDrawerClosed(@NonNull View drawerView) {}
        @Override public void onDrawerStateChanged(int newState) {
            //需要在菜单状态改变的时候，关闭Hotbar类型的Spinner，这个库并没有自动关闭的功能，所以需要这么做
            //关掉！关掉！一定要关掉！
            binding.hotbarType.dismiss();
        }
    }

    private class ControlSettingsClickListener implements View.OnClickListener {
        private final ViewControlMenuBinding binding;
        private final ControlLayout controlLayout;

        public ControlSettingsClickListener(ViewControlMenuBinding binding, ControlLayout controlLayout) {
            this.binding = binding;
            this.controlLayout = controlLayout;
            this.binding.addButton.setOnClickListener(this);
            this.binding.addDrawer.setOnClickListener(this);
            this.binding.addJoystick.setOnClickListener(this);
            this.binding.controlsSettings.setOnClickListener(this);
            this.binding.load.setOnClickListener(this);
            this.binding.save.setOnClickListener(this);
            this.binding.saveAndExit.setOnClickListener(this);
            this.binding.selectDefault.setOnClickListener(this);
            this.binding.exit.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == binding.addButton) controlLayout.addControlButton(new ControlData(getString(R.string.controls_add_control_button)));
            else if (v == binding.addDrawer) controlLayout.addDrawer(new ControlDrawerData());
            else if (v == binding.addJoystick) controlLayout.addJoystickButton(new ControlJoystickData());
            else if (v == binding.controlsSettings) new ControlSettingsDialog(MainActivity.this).show();
            else if (v == binding.load) controlLayout.openLoadDialog();
            else if (v == binding.save) controlLayout.openSaveDialog();
            else if (v == binding.saveAndExit) controlLayout.openSaveAndExitDialog(MainActivity.this);
            else if (v == binding.selectDefault) controlLayout.openSetDefaultDialog();
            else if (v == binding.exit) controlLayout.openExitDialog(MainActivity.this);
        }
    }
}

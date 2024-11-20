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
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.databinding.ActivityGameBinding;
import com.movtery.zalithlauncher.databinding.ViewControlSettingsBinding;
import com.movtery.zalithlauncher.databinding.ViewGameSettingsBinding;
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
import com.movtery.zalithlauncher.setting.Settings;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.activity.BaseActivity;
import com.movtery.zalithlauncher.ui.dialog.ControlSettingsDialog;
import com.movtery.zalithlauncher.ui.dialog.KeyboardDialog;
import com.movtery.zalithlauncher.ui.dialog.MouseSettingsDialog;
import com.movtery.zalithlauncher.ui.dialog.SeekbarDialog;
import com.movtery.zalithlauncher.ui.dialog.SelectControlsDialog;
import com.movtery.zalithlauncher.ui.dialog.TipDialog;
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
    private GyroControl mGyroControl = null;
    private KeyboardDialog keyboardDialog;
    public static TouchCharInput touchCharInput;
    public static ControlLayout mControlLayout;

    Version minecraftVersion;

    private ViewGameSettingsBinding mGameSettingsBinding;
    private ViewControlSettingsBinding mControlSettingsBinding;
    private GameService.LocalBinder mServiceBinder;

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
        if(AllSettings.getEnableGyro()) mGyroControl = new GyroControl(this);

        Window window = getWindow();
        // Enabling this on TextureView results in a broken white result
        if(AllSettings.getAlternateSurface()) window.setBackgroundDrawable(null);
        else window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));

        // Set the sustained performance mode for available APIs
        window.setSustainedPerformanceMode(AllSettings.getSustainedPerformance());

        // 防止系统息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ControlLayout controlLayout = binding.mainControlLayout;
        mControlSettingsBinding = ViewControlSettingsBinding.inflate(getLayoutInflater());
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
            mGameSettingsBinding = ViewGameSettingsBinding.inflate(getLayoutInflater());
            MenuSettingsInitListener menuSettingsInitListener = new MenuSettingsInitListener(mGameSettingsBinding);

            binding.mainNavigationView.removeAllViews();
            binding.mainNavigationView.addView(mGameSettingsBinding.getRoot());

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

    private void openResolutionAdjuster() {
        new SeekbarDialog.Builder(this)
                .setTitle(R.string.setting_resolution_scaler_title)
                .setMessage(R.string.setting_resolution_scaler_desc)
                .setMin(25)
                .setMax(300)
                .setSuffix("%")
                .setValue(AllSettings.getResolutionRatio())
                .setPreviewTextContentGetter(value -> VideoSettingsFragment.getResolutionRatioPreview(getResources(), value))
                .setOnSeekbarChangeListener(value -> {
                    binding.mainGameRenderView.refreshSize(value);
                    binding.hotbarView.refreshScaleFactor(value / 100f);
                })
                .setOnSeekbarStopTrackingTouch(value -> {
                    Settings.Manager.put("resolutionRatio", value).save();
                    //当分辨率缩放的时候，需要刷新一下Hotbar的判定
                    EventBus.getDefault().post(new RefreshHotbarEvent());
                })
                .buildDialog();
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
        if(mGyroControl != null) mGyroControl.enable();
        CallbackBridge.nativeSetWindowAttrib(LwjglGlfwKeycode.GLFW_HOVERED, 1);
    }

    @Override
    protected void onPause() {
        if(mGyroControl != null) mGyroControl.disable();
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

        if(mGyroControl != null) mGyroControl.updateOrientation();
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

    private void dialogSendCustomKey() {
        keyboardDialog.setOnKeycodeSelectListener(EfficientAndroidLWJGLKeycode::execKeyIndex).show();
    }

    private void replacementCustomControls() {
        SelectControlsDialog dialog = new SelectControlsDialog(this);
        dialog.setOnSelectedListener(file -> {
            try {
                binding.mainControlLayout.loadLayout(file.getAbsolutePath());
                //刷新：是否隐藏菜单按钮
                mGameMenuWrapper.setVisibility(!binding.mainControlLayout.hasMenuButton());
            } catch (IOException ignored) {}
            dialog.dismiss();
        });
        dialog.show();
    }

    boolean isInEditor;
    private void openCustomControls() {
        binding.mainControlLayout.setModifiable(true);
        binding.mainNavigationView.removeAllViews();
        binding.mainNavigationView.addView(mControlSettingsBinding.getRoot());
        mGameMenuWrapper.setVisibility(true);
        isInEditor = true;
    }

    private void openLogOutput() {
        binding.mainLoggerView.setVisibilityWithAnim(true);
    }

    public static void toggleMouse(Context ctx) {
        if (CallbackBridge.isGrabbing()) return;

        if (binding != null) {
            Toast.makeText(ctx, binding.mainTouchpad.switchState()
                            ? R.string.control_mouseon : R.string.control_mouseoff,
                    Toast.LENGTH_SHORT).show();
        }
    }

    public static void dialogForceClose(Context ctx) {
        new TipDialog.Builder(ctx)
                .setMessage(R.string.force_exit_confirm)
                .setConfirmClickListener(() -> {
                    try {
                        ZHTools.killProcess();
                    } catch (Throwable th) {
                        Logging.w(Tools.APP_NAME, "Could not enable System.exit() method!", th);
                    }
                }).buildDialog();
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

    public void openMouseSettings() {
        new MouseSettingsDialog(this, (mouseSpeed, mouseScale) -> {
            Settings.Manager
                    .put("mousespeed", mouseSpeed)
                    .put("mousescale", mouseScale).save();
            binding.mainTouchpad.updateMouseScale();
        }, () -> binding.mainTouchpad.updateMouseDrawable()).show();
    }

    public void adjustGyroSensitivityLive() {
        if(!AllSettings.getEnableGyro()) {
            Toast.makeText(this, R.string.toast_turn_on_gyro, Toast.LENGTH_LONG).show();
            return;
        }
        new SeekbarDialog.Builder(this)
                .setTitle(R.string.setting_gyro_sensitivity_title)
                .setMin(25)
                .setMax(300)
                .setValue((int) (AllSettings.getGyroSensitivity() * 100))
                .setSuffix("%")
                .setOnSeekbarStopTrackingTouch(value -> Settings.Manager.put("gyroSensitivity", value).save())
                .buildDialog();
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
        binding.mainNavigationView.addView(mGameSettingsBinding.getRoot());
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

    private class MenuSettingsInitListener implements View.OnClickListener, OnSpinnerItemSelectedListener<HotbarType>, DrawerLayout.DrawerListener {
        private final ViewGameSettingsBinding binding;

        public MenuSettingsInitListener(ViewGameSettingsBinding binding) {
            this.binding = binding;
            this.binding.forceClose.setOnClickListener(this);
            this.binding.logOutput.setOnClickListener(this);
            this.binding.sendCustomKey.setOnClickListener(this);
            this.binding.mouseSettings.setOnClickListener(this);
            this.binding.resolutionScaler.setOnClickListener(this);
            this.binding.gyroSensitivity.setOnClickListener(this);
            this.binding.replacementCustomcontrol.setOnClickListener(this);
            this.binding.editControl.setOnClickListener(this);
            this.binding.hotbarHeightRemove.setOnClickListener(this);
            this.binding.hotbarHeightAdd.setOnClickListener(this);
            this.binding.hotbarWidthRemove.setOnClickListener(this);
            this.binding.hotbarWidthAdd.setOnClickListener(this);

            ObjectSpinnerAdapter<HotbarType> hotbarTypeAdapter = new ObjectSpinnerAdapter<>(
                    this.binding.hotbarType,
                    hotbarType -> getString(hotbarType.getNameId())
            );
            hotbarTypeAdapter.setItems(HotbarType.getEntries());
            this.binding.hotbarType.setSpinnerAdapter(hotbarTypeAdapter);
            this.binding.hotbarType.setOnSpinnerItemSelectedListener(this);
            this.binding.hotbarType.selectItemByIndex(HotbarUtils.getCurrentTypeIndex());

            binding.hotbarWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    binding.hotbarWidthValue.setText(String.valueOf(progress));
                    EventBus.getDefault().post(new HotbarChangeEvent(progress, binding.hotbarHeight.getProgress()));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Settings.Manager.put("hotbarWidth", seekBar.getProgress()).save();
                }
            });

            binding.hotbarHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    binding.hotbarHeightValue.setText(String.valueOf(progress));
                    EventBus.getDefault().post(new HotbarChangeEvent(binding.hotbarWidth.getProgress(), progress));
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    Settings.Manager.put("hotbarHeight", seekBar.getProgress()).save();
                }
            });

            binding.hotbarWidth.setMax(currentDisplayMetrics.widthPixels / 2);
            binding.hotbarWidth.setMin(40);
            binding.hotbarHeight.setMax(currentDisplayMetrics.heightPixels / 2);
            binding.hotbarHeight.setMin(20);

            refreshHotbarProgress();
        }

        @Override
        public void onClick(View v) {
            if (v == binding.forceClose) dialogForceClose(MainActivity.this);
            else if (v == binding.logOutput) openLogOutput();
            else if (v == binding.sendCustomKey) dialogSendCustomKey();
            else if (v == binding.mouseSettings) openMouseSettings();
            else if (v == binding.resolutionScaler) openResolutionAdjuster();
            else if (v == binding.gyroSensitivity) adjustGyroSensitivityLive();
            else if (v == binding.replacementCustomcontrol) replacementCustomControls();
            else if (v == binding.editControl) openCustomControls();
            else if (v == binding.hotbarWidthRemove) adjustSeekbar(binding.hotbarWidth, -1);
            else if (v == binding.hotbarWidthAdd) adjustSeekbar(binding.hotbarWidth, 1);
            else if (v == binding.hotbarHeightRemove) adjustSeekbar(binding.hotbarHeight, -1);
            else if (v == binding.hotbarHeightAdd) adjustSeekbar(binding.hotbarHeight, 1);
        }

        /**
         * 调整滑动条的值
         * @param seekBar 滑动条
         * @param v 需要调整的值的大小
         */
        private void adjustSeekbar(SeekBar seekBar, int v) {
            seekBar.setProgress(seekBar.getProgress() + v);
        }

        private void refreshHotbarProgress() {
            binding.hotbarWidth.setProgress(AllSettings.getHotbarWidth());
            binding.hotbarHeight.setProgress(AllSettings.getHotbarHeight());
        }

        @Override
        public void onItemSelected(int i, @Nullable HotbarType t, int i1, HotbarType t1) {
            if (t1 == HotbarType.AUTO) {
                binding.hotbarWidthLayout.setVisibility(View.GONE);
                binding.hotbarHeightLayout.setVisibility(View.GONE);
            } else if (t1 == HotbarType.MANUALLY) {
                binding.hotbarWidthLayout.setVisibility(View.VISIBLE);
                binding.hotbarHeightLayout.setVisibility(View.VISIBLE);

                refreshHotbarProgress();
            }

            Settings.Manager.put("hotbarType", t1.getValueName()).save();
            EventBus.getDefault().post(new RefreshHotbarEvent());
        }

        @Override
        public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
        }

        @Override
        public void onDrawerOpened(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerClosed(@NonNull View drawerView) {
        }

        @Override
        public void onDrawerStateChanged(int newState) {
            //需要在菜单状态改变的时候，关闭Hotbar类型的Spinner，这个库并没有自动关闭的功能，所以需要这么做
            //关掉！关掉！一定要关掉！
            binding.hotbarType.dismiss();
        }
    }

    private class ControlSettingsClickListener implements View.OnClickListener {
        private final ViewControlSettingsBinding binding;
        private final ControlLayout controlLayout;

        public ControlSettingsClickListener(ViewControlSettingsBinding binding, ControlLayout controlLayout) {
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

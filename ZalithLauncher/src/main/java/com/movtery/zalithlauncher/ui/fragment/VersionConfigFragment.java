package com.movtery.zalithlauncher.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.movtery.anim.AnimPlayer;
import com.movtery.anim.animations.Animations;
import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.databinding.FragmentVersionConfigBinding;
import com.movtery.zalithlauncher.event.sticky.FileSelectorEvent;
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathManager;
import com.movtery.zalithlauncher.feature.version.NoVersionException;
import com.movtery.zalithlauncher.feature.version.Version;
import com.movtery.zalithlauncher.feature.version.VersionConfig;
import com.movtery.zalithlauncher.feature.version.VersionIconUtils;
import com.movtery.zalithlauncher.feature.version.VersionsManager;
import com.movtery.zalithlauncher.task.Task;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.dialog.TipDialog;
import com.movtery.zalithlauncher.utils.ZHTools;
import com.movtery.zalithlauncher.utils.file.FileTools;
import com.skydoves.powerspinner.DefaultSpinnerAdapter;
import com.skydoves.powerspinner.OnSpinnerItemSelectedListener;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.multirt.MultiRTUtils;
import net.kdt.pojavlaunch.multirt.Runtime;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class VersionConfigFragment extends FragmentWithAnim {
    public static final String TAG = "VersionConfigFragment";
    private static final String SELECT_CONTROL = "SELECT_CONTROL";
    private static final String SELECT_CUSTOM_PATH = "SELECT_CUSTOM_PATH";

    private FragmentVersionConfigBinding binding;
    private Version mTempVersion = null;
    private VersionConfig mTempConfig = null;
    private List<String> mRenderNames;
    private VersionIconUtils mVersionIconUtils;

    private final AnimPlayer resetIconAnimPlayer = new AnimPlayer();
    private final ActivityResultLauncher<String[]> openDocumentLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
                if (result != null) {
                    AlertDialog dialog = ZHTools.showTaskRunningDialog(requireActivity());
                    Task.runTask(() -> FileTools.copyFileInBackground(requireActivity(), result, VersionsManager.INSTANCE.getVersionIconFile(mTempVersion)))
                            .ended(TaskExecutors.getAndroidUI(), file -> refreshIcon(false))
                            .finallyTask(TaskExecutors.getAndroidUI(), dialog::dismiss)
                            .onThrowable(Tools::showErrorRemote)
                            .execute();
                }
            });
    private String mSelectPathMark = "";

    public VersionConfigFragment() {
        super(R.layout.fragment_version_config);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FileSelectorEvent fileSelectorEvent = EventBus.getDefault().getStickyEvent(FileSelectorEvent.class);

        if (mTempConfig != null) {
            if (fileSelectorEvent != null && fileSelectorEvent.getPath() != null) {
                String path = fileSelectorEvent.getPath();
                if (Objects.equals(mSelectPathMark, SELECT_CONTROL)) {
                    mTempConfig.setControl(path);
                }
                else if (Objects.equals(mSelectPathMark, SELECT_CUSTOM_PATH)) {
                    mTempConfig.setCustomPath(path);
                }
            }
        }

        if (fileSelectorEvent != null) EventBus.getDefault().removeStickyEvent(fileSelectorEvent);

        binding = FragmentVersionConfigBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Set up behaviors
        binding.cancelButton.setOnClickListener(v -> ZHTools.onBackPressed(requireActivity()));
        binding.saveButton.setOnClickListener(v -> {
            save();
            Tools.backToMainMenu(requireActivity());
        });
        binding.controlName.setOnClickListener(v -> {
            mSelectPathMark = SELECT_CONTROL;
            Bundle bundle = new Bundle();
            bundle.putBoolean(ControlButtonFragment.BUNDLE_SELECT_CONTROL, true);
            ZHTools.swapFragmentWithAnim(this, ControlButtonFragment.class, ControlButtonFragment.TAG, bundle);
        });
        binding.resetControl.setOnClickListener(v -> {
            binding.controlName.setText("");
            mTempConfig.setControl("");
        });
        binding.customPath.setOnClickListener(v -> {
            mSelectPathMark = SELECT_CUSTOM_PATH;
            Bundle bundle = new Bundle();
            bundle.putBoolean(FilesFragment.BUNDLE_SELECT_FOLDER_MODE, true);
            bundle.putBoolean(FilesFragment.BUNDLE_SHOW_FILE, false);
            bundle.putBoolean(FilesFragment.BUNDLE_REMOVE_LOCK_PATH, false);
            bundle.putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false);
            bundle.putString(FilesFragment.BUNDLE_LOCK_PATH, ProfilePathManager.getCurrentPath());
            bundle.putString(FilesFragment.BUNDLE_LIST_PATH, mTempVersion.getVersionsFolder());
            ZHTools.swapFragmentWithAnim(this, FilesFragment.class, FilesFragment.TAG, bundle);
        });
        binding.resetCustomPath.setOnClickListener(v -> {
            binding.customPath.setText("");
            mTempConfig.setCustomPath("");
        });
        binding.iconLayout.setOnClickListener(v -> openDocumentLauncher.launch(new String[]{"image/*"}));
        binding.iconReset.setOnClickListener(v -> resetIcon());

        Version version = VersionsManager.INSTANCE.getCurrentVersion();
        if (version == null) {
            Tools.showError(requireActivity(), getString(R.string.version_manager_no_installed_version), new NoVersionException("There is no installed version"));
            ZHTools.onBackPressed(requireActivity());
            return;
        }

        mTempVersion = version;
        mTempConfig = mTempVersion.getVersionConfig();
        mVersionIconUtils = new VersionIconUtils(mTempVersion);

        boolean isolation = mTempVersion.getVersionConfig().isIsolation();
        binding.isolation.setChecked(isolation);
        disableCustomPath(isolation);

        binding.isolation.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mTempConfig.setIsolation(isChecked);
            disableCustomPath(isChecked);
            closeSpinner();
        });

        loadValues(view.getContext());
        refreshIcon(true);
    }

    private void disableCustomPath(boolean disable) {
        binding.customPath.setEnabled(!disable);
        binding.resetCustomPath.setEnabled(!disable);
    }

    private void closeSpinner() {
        binding.runtimeSpinner.dismiss();
        binding.rendererSpinner.dismiss();
    }

    @Override
    public void onPause() {
        super.onPause();
        closeSpinner();
    }

    /**
     * 刷新图标，并对重置图标的按钮播放显示或隐藏的动画
     * @param init 首次刷新不需要对重置按钮播放动画
     */
    private void refreshIcon(boolean init) {
        boolean isCustomIcon = mVersionIconUtils.start(binding.icon);
        if (init) {
            binding.iconReset.setVisibility(isCustomIcon ? View.VISIBLE : View.GONE);
        } else {
            resetIconAnimPlayer.clearEntries();
            resetIconAnimPlayer.apply(new AnimPlayer.Entry(binding.iconReset, isCustomIcon ? Animations.BounceEnlarge : Animations.BounceShrink))
                    .setOnStart(() -> {
                        binding.iconReset.setVisibility(View.VISIBLE);
                        binding.iconReset.setEnabled(false);
                    })
                    .setOnEnd(() -> {
                        binding.iconReset.setVisibility(isCustomIcon ? View.VISIBLE : View.GONE);
                        binding.iconReset.setEnabled(isCustomIcon);
                    }).start();
        }
    }

    private void resetIcon() {
        new TipDialog.Builder(requireActivity())
                .setMessage(R.string.pedit_reset_icon)
                .setWarning()
                .setConfirmClickListener(checked -> {
                    mVersionIconUtils.resetIcon();
                    refreshIcon(false);
                }).buildDialog();
    }

    private void loadValues(@NonNull Context context) {
        // Runtime spinner
        List<Runtime> runtimes = MultiRTUtils.getRuntimes();
        List<String> runtimeNames = new ArrayList<>();
        runtimes.forEach(v -> runtimeNames.add(String.format("%s - %s", v.name, v.versionString == null ? getString(R.string.multirt_runtime_corrupt) : v.versionString)));
        runtimeNames.add(getString(R.string.install_auto_select));
        int jvmIndex = runtimeNames.size() - 1;
        if (!mTempConfig.getJavaDir().isEmpty()) {
            String selectedRuntime = mTempConfig.getJavaDir().substring(Tools.LAUNCHERPROFILES_RTPREFIX.length());
            int nindex = runtimes.indexOf(new Runtime(selectedRuntime));
            if (nindex != -1) jvmIndex = nindex;
        }
        DefaultSpinnerAdapter runtimeAdapter = new DefaultSpinnerAdapter(binding.runtimeSpinner);
        runtimeAdapter.setItems(runtimeNames);
        binding.runtimeSpinner.setSpinnerAdapter(runtimeAdapter);
        binding.runtimeSpinner.selectItemByIndex(jvmIndex);
        binding.runtimeSpinner.setOnSpinnerItemSelectedListener((OnSpinnerItemSelectedListener<String>) (i, s, i1, t1) -> {
            if (i1 == runtimeNames.size() - 1) mTempConfig.setJavaDir("");
            else {
                Runtime runtime = runtimes.get(i1);
                mTempConfig.setJavaDir(runtime.versionString == null ? "" : Tools.LAUNCHERPROFILES_RTPREFIX + runtime.name);
            }
        });

        // Renderer spinner
        Tools.RenderersList renderersList = Tools.getCompatibleRenderers(context);
        mRenderNames = renderersList.rendererIds;
        List<String> renderList = new ArrayList<>(renderersList.rendererDisplayNames.length + 1);
        renderList.addAll(Arrays.asList(renderersList.rendererDisplayNames));
        renderList.add(context.getString(R.string.generic_default));
        int rendererIndex = renderList.size() - 1;
        if (!mTempConfig.getRenderer().isEmpty()) {
            int nindex = mRenderNames.indexOf(mTempConfig.getRenderer());
            if (nindex != -1) rendererIndex = nindex;
        }
        DefaultSpinnerAdapter rendererAdapter = new DefaultSpinnerAdapter(binding.rendererSpinner);
        rendererAdapter.setItems(renderList);
        binding.rendererSpinner.setSpinnerAdapter(rendererAdapter);
        binding.rendererSpinner.selectItemByIndex(rendererIndex);
        binding.rendererSpinner.setOnSpinnerItemSelectedListener((OnSpinnerItemSelectedListener<String>) (i, s, i1, t1) -> {
            if (i1 == renderList.size() - 1) mTempConfig.setRenderer("");
            else mTempConfig.setRenderer(mRenderNames.get(i1));
        });

        binding.jvmArgsEdit.setText(mTempConfig.getJavaArgs());
        binding.controlName.setText(mTempConfig.getControl());
        binding.customPath.setText(mTempConfig.getCustomPath().replaceFirst(ProfilePathManager.getCurrentPath(), "."));
    }

    private void save() {
        //First, check for potential issues in the inputs
        mTempConfig.setControl(binding.controlName.getText().toString());
        Editable argsText = binding.jvmArgsEdit.getText();
        mTempConfig.setJavaArgs(argsText == null ? "" : argsText.toString());

        mTempConfig.save();
        VersionsManager.INSTANCE.refresh();
    }

    @Override
    public void slideIn(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.editorLayout, Animations.BounceInDown))
                .apply(new AnimPlayer.Entry(binding.operateLayout, Animations.BounceInLeft))
                .apply(new AnimPlayer.Entry(binding.iconLayout, Animations.Wobble));
    }

    @Override
    public void slideOut(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.editorLayout, Animations.FadeOutUp))
                .apply(new AnimPlayer.Entry(binding.operateLayout, Animations.FadeOutRight));
    }
}

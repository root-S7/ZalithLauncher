package net.kdt.pojavlaunch.fragments;

import static com.movtery.zalithlauncher.event.single.RefreshVersionsEvent.MODE.END;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.movtery.anim.AnimPlayer;
import com.movtery.anim.animations.Animations;
import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.databinding.FragmentLauncherBinding;
import com.movtery.zalithlauncher.event.single.AccountUpdateEvent;
import com.movtery.zalithlauncher.event.single.LaunchGameEvent;
import com.movtery.zalithlauncher.event.single.RefreshVersionsEvent;
import com.movtery.zalithlauncher.feature.version.Version;
import com.movtery.zalithlauncher.feature.version.utils.VersionIconUtils;
import com.movtery.zalithlauncher.feature.version.VersionInfo;
import com.movtery.zalithlauncher.feature.version.VersionsManager;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.fragment.FragmentWithAnim;
import com.movtery.zalithlauncher.ui.fragment.LauncherMenuFragment;
import com.movtery.zalithlauncher.ui.fragment.VersionManagerFragment;
import com.movtery.zalithlauncher.ui.fragment.VersionsListFragment;
import com.movtery.zalithlauncher.ui.subassembly.account.AccountViewWrapper;
import com.movtery.zalithlauncher.utils.ZHTools;
import com.movtery.zalithlauncher.utils.anim.ViewAnimUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainMenuFragment extends FragmentWithAnim {
    public static final String TAG = "MainMenuFragment";
    private FragmentLauncherBinding binding;
    private AccountViewWrapper accountViewWrapper;

    public MainMenuFragment() {
        super(R.layout.fragment_launcher);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentLauncherBinding.inflate(getLayoutInflater());
        accountViewWrapper = new AccountViewWrapper(this, binding.viewAccount);
        accountViewWrapper.refreshAccountInfo();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViewPager();

        binding.version.setOnClickListener(v -> {
            if (!isTaskRunning()) {
                ZHTools.swapFragmentWithAnim(this, VersionsListFragment.class, VersionsListFragment.TAG, null);
            } else {
                ViewAnimUtils.setViewAnim(binding.version, Animations.Shake);
                TaskExecutors.runInUIThread(() -> Toast.makeText(requireContext(), R.string.version_manager_task_in_progress, Toast.LENGTH_SHORT).show());
            }
        });
        binding.managerProfileButton.setOnClickListener(v -> {
            if (!isTaskRunning()) {
                ViewAnimUtils.setViewAnim(binding.managerProfileButton, Animations.Pulse);
                ZHTools.swapFragmentWithAnim(this, VersionManagerFragment.class, VersionManagerFragment.TAG, null);
            } else {
                ViewAnimUtils.setViewAnim(binding.managerProfileButton, Animations.Shake);
                TaskExecutors.runInUIThread(() -> Toast.makeText(requireContext(), R.string.version_manager_task_in_progress, Toast.LENGTH_SHORT).show());
            }
        });

        binding.playButton.setOnClickListener(v -> EventBus.getDefault().post(new LaunchGameEvent()));

        binding.versionName.setSelected(true);
        binding.versionInfo.setSelected(true);
    }

    private void initViewPager() {
        binding.launcherMenuPager.setAdapter(new ViewPagerAdapter(this, binding.launcherMenuPager));
        binding.launcherMenuPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        binding.launcherMenuPager.setOffscreenPageLimit(1);
    }

    @Override
    public void onResume() {
        super.onResume();
        VersionsManager.INSTANCE.refresh();
    }

    @Subscribe()
    public void event(RefreshVersionsEvent event) {
        if (event.getMode() == END) {
            TaskExecutors.runInUIThread(() -> {
                Version version = VersionsManager.INSTANCE.getCurrentVersion();

                int versionInfoVisibility;
                if (version != null) {
                    binding.versionName.setText(version.getVersionName());
                    VersionInfo versionInfo = version.getVersionInfo();
                    if (versionInfo != null) {
                        binding.versionInfo.setText(versionInfo.getInfoString());
                        versionInfoVisibility = View.VISIBLE;
                    } else versionInfoVisibility = View.GONE;

                    new VersionIconUtils(version).start(binding.versionIcon);
                    binding.managerProfileButton.setVisibility(View.VISIBLE);
                } else {
                    binding.versionName.setText(R.string.version_no_versions);
                    binding.managerProfileButton.setVisibility(View.GONE);
                    versionInfoVisibility = View.GONE;
                }
                binding.versionInfo.setVisibility(versionInfoVisibility);
            });
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void event(AccountUpdateEvent event) {
        if (accountViewWrapper != null) accountViewWrapper.refreshAccountInfo();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void slideIn(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.launcherMenu, Animations.BounceInDown))
                .apply(new AnimPlayer.Entry(binding.playLayout, Animations.BounceInLeft))
                .apply(new AnimPlayer.Entry(binding.playButtonsLayout, Animations.BounceEnlarge));
    }

    @Override
    public void slideOut(AnimPlayer animPlayer) {
        animPlayer.apply(new AnimPlayer.Entry(binding.launcherMenu, Animations.FadeOutUp))
                .apply(new AnimPlayer.Entry(binding.playLayout, Animations.FadeOutRight))
                .apply(new AnimPlayer.Entry(binding.playButtonsLayout, Animations.BounceShrink));
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {
        private final ViewPager2 mViewPager;

        public ViewPagerAdapter(@NonNull Fragment fragment, ViewPager2 viewPager) {
            super(fragment.requireActivity());
            mViewPager = viewPager;
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) return new LauncherMenuFragment();
            else return new LauncherMenuFragment.EmptyMenuFragment(mViewPager);
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }
}

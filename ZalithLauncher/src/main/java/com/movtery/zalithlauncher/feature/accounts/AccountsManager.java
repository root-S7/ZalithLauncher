package com.movtery.zalithlauncher.feature.accounts;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.event.single.AccountUpdateEvent;
import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.setting.Settings;
import com.movtery.zalithlauncher.utils.PathAndUrlManager;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.listener.DoneListener;
import net.kdt.pojavlaunch.authenticator.listener.ErrorListener;
import net.kdt.pojavlaunch.authenticator.listener.ProgressListener;
import net.kdt.pojavlaunch.authenticator.microsoft.PresentedException;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AccountsManager {
    private final static int MAX_LOGIN_STEP = 5;
    @SuppressLint("StaticFieldLeak")
    private static volatile AccountsManager accountsManager;
    private final List<MinecraftAccount> accounts = new ArrayList<>();
    private ObjectAnimator mLoginBarAnimator;
    private ProgressListener mProgressListener;
    private DoneListener mDoneListener;
    private ErrorListener mErrorListener;

    private AccountsManager() {
    }

    public static AccountsManager getInstance() {
        if (accountsManager == null) {
            synchronized (AccountsManager.class) {
                if (accountsManager == null) {
                    accountsManager = new AccountsManager();
                    //确保完全初始化，初始化完成之后，初始化监听器，然后执行刷新操作
                    accountsManager.initListener();
                    accountsManager.reload();
                }
                return accountsManager;
            }
        }
        return accountsManager;
    }

    @SuppressLint("ObjectAnimatorBinding")
    private void initListener() {
        mProgressListener = step -> {
            // Animate the login bar, cosmetic purposes only
            float mLoginBarWidth = -1;
            float value = (float) Tools.currentDisplayMetrics.widthPixels / MAX_LOGIN_STEP;
            if (mLoginBarAnimator != null) {
                mLoginBarAnimator.cancel();
                mLoginBarAnimator.setFloatValues(mLoginBarWidth, value * step);
            } else {
                mLoginBarAnimator = ObjectAnimator.ofFloat(this, "LoginBarWidth", mLoginBarWidth, value * step);
            }
            mLoginBarAnimator.start();
        };

        mDoneListener = account -> {
            ContextExecutor.showToast(R.string.account_login_done, Toast.LENGTH_SHORT);

            //检查账号是否已存在
            if (getAllAccount().contains(account)) {
                EventBus.getDefault().post(new AccountUpdateEvent());
                return;
            }

            reload();

            if (getAllAccount().isEmpty()) setCurrentAccount(account);
            else EventBus.getDefault().post(new AccountUpdateEvent());
        };

        mErrorListener = errorMessage -> {
            Activity activity = ContextExecutor.getActivity();
            if (errorMessage instanceof PresentedException) {
                PresentedException exception = (PresentedException) errorMessage;
                Throwable cause = exception.getCause();
                if (cause == null) {
                    Tools.dialog(activity, activity.getString(R.string.generic_error), exception.toString(activity));
                } else {
                    Tools.showError(activity, exception.toString(activity), exception.getCause());
                }
            } else {
                Tools.showError(activity, errorMessage);
            }
        };
    }

    public void performLogin(MinecraftAccount minecraftAccount) {
        performLogin(minecraftAccount, getDoneListener(), getErrorListener());
    }

    public void performLogin(MinecraftAccount minecraftAccount, DoneListener doneListener, ErrorListener errorListener) {
        if (AccountUtils.isNoLoginRequired(minecraftAccount)) {
            doneListener.onLoginDone(minecraftAccount);
            return;
        }

        if (AccountUtils.isOtherLoginAccount(minecraftAccount)) {
            AccountUtils.otherLogin(ContextExecutor.getApplication(), minecraftAccount, doneListener, errorListener);
            return;
        }

        if (AccountUtils.isMicrosoftAccount(minecraftAccount)) {
            AccountUtils.microsoftLogin(minecraftAccount, doneListener, errorListener);
        }
    }

    public void reload() {
        accounts.clear();
        File accountsPath = new File(PathAndUrlManager.DIR_ACCOUNT_NEW);
        if (accountsPath.exists() && accountsPath.isDirectory()) {
            File[] files = accountsPath.listFiles();
            if (files != null) {
                for (File accountFile : files) {
                    try {
                        String jsonString = Tools.read(accountFile);
                        MinecraftAccount account = MinecraftAccount.parse(jsonString);
                        if (account != null) accounts.add(account);
                    } catch (IOException e) {
                        Logging.e("AccountsManager", String.format("File %s is not recognized as a profile for an account", accountFile.getName()));
                    }
                }
            }
        }
        Logging.i("AccountsManager", "Reload complete.");
    }

    public MinecraftAccount getCurrentAccount() {
        MinecraftAccount account = MinecraftAccount.loadFromUniqueUUID(AllSettings.getCurrentAccount());
        if (account == null) {
            if (getAllAccount().isEmpty()) return null;
            MinecraftAccount account1 = getAllAccount().get(0);
            setCurrentAccount(account1);
            return account1;
        }
        return account;
    }

    public List<MinecraftAccount> getAllAccount() {
        return new ArrayList<>(accounts);
    }

    public boolean haveMicrosoftAccount() {
        for (MinecraftAccount account : accounts) {
            if (AccountUtils.isMicrosoftAccount(account)) {
                return true;
            }
        }
        return false;
    }

    public void setCurrentAccount(@NonNull MinecraftAccount account) {
        Settings.Manager.put("currentAccount", account.getUniqueUUID()).save();
        EventBus.getDefault().post(new AccountUpdateEvent());
    }

    public ProgressListener getProgressListener() {
        return mProgressListener;
    }

    public DoneListener getDoneListener() {
        return mDoneListener;
    }

    public ErrorListener getErrorListener() {
        return mErrorListener;
    }
}

package com.movtery.zalithlauncher.feature.accounts;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.context.ContextExecutor;
import com.movtery.zalithlauncher.event.single.AccountUpdateEvent;
import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.task.TaskExecutors;
import com.movtery.zalithlauncher.ui.dialog.TipDialog;
import com.movtery.zalithlauncher.utils.path.PathManager;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.listener.DoneListener;
import net.kdt.pojavlaunch.authenticator.listener.ErrorListener;
import net.kdt.pojavlaunch.authenticator.microsoft.PresentedException;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class AccountsManager {
    @SuppressLint("StaticFieldLeak")
    private static volatile AccountsManager accountsManager;
    private final List<MinecraftAccount> accounts = new ArrayList<>();
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
        mDoneListener = account -> {
            TaskExecutors.runInUIThread(() -> ContextExecutor.showToast(R.string.account_login_done, Toast.LENGTH_SHORT));

            //检查账号是否已存在
            if (getAllAccount().contains(account)) {
                EventBus.getDefault().post(new AccountUpdateEvent());
                return;
            }

            reload();

            if (getAllAccount().isEmpty()) setCurrentAccount(account);
            else EventBus.getDefault().post(new AccountUpdateEvent());
        };

        mErrorListener = errorMessage -> TaskExecutors.runInUIThread(() -> {
            Activity activity = ContextExecutor.getActivity();
            if (errorMessage instanceof PresentedException) {
                PresentedException exception = (PresentedException) errorMessage;
                Throwable cause = exception.getCause();
                if (cause == null) {
                    new TipDialog.Builder(activity)
                            .setTitle(R.string.generic_error)
                            .setMessage(exception.toString(activity))
                            .setWarning()
                            .setConfirm(android.R.string.ok)
                            .setShowCancel(false)
                            .buildDialog();
                } else {
                    Tools.showError(activity, exception.toString(activity), exception.getCause());
                }
            } else {
                Tools.showError(activity, errorMessage);
            }
        });
    }

    public void performLogin(final Context context, MinecraftAccount minecraftAccount) {
        performLogin(context, minecraftAccount, getDoneListener(), getErrorListener());
    }

    public void performLogin(final Context context, MinecraftAccount minecraftAccount, DoneListener doneListener, ErrorListener errorListener) {
        if (AccountUtils.isNoLoginRequired(minecraftAccount)) {
            doneListener.onLoginDone(minecraftAccount);
            return;
        }

        if (AccountUtils.isOtherLoginAccount(minecraftAccount)) {
            AccountUtils.otherLogin(context, minecraftAccount, doneListener, errorListener);
            return;
        }

        if (AccountUtils.isMicrosoftAccount(minecraftAccount)) {
            AccountUtils.microsoftLogin(context, minecraftAccount, doneListener, errorListener);
        }
    }

    public void reload() {
        accounts.clear();
        File accountsPath = new File(PathManager.DIR_ACCOUNT_NEW);
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
        MinecraftAccount account = MinecraftAccount.loadFromUniqueUUID(AllSettings.getCurrentAccount().getValue());
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
        AllSettings.getCurrentAccount().put(account.getUniqueUUID()).save();
        EventBus.getDefault().post(new AccountUpdateEvent());
    }

    public DoneListener getDoneListener() {
        return mDoneListener;
    }

    public ErrorListener getErrorListener() {
        return mErrorListener;
    }
}

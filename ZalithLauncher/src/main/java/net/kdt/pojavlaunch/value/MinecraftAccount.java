package net.kdt.pojavlaunch.value;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;
import com.movtery.zalithlauncher.feature.accounts.AccountUtils;
import com.movtery.zalithlauncher.feature.accounts.AccountsManager;
import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.utils.PathAndUrlManager;
import com.movtery.zalithlauncher.utils.skin.SkinFileDownloader;
import com.movtery.zalithlauncher.utils.stringutils.StringUtilsKt;

import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Keep
public class MinecraftAccount {
    public String accessToken = "0"; // access token
    public String clientToken = "0"; // clientID: refresh and invalidate
    public String profileId = "00000000-0000-0000-0000-000000000000"; // profile UUID, for obtaining skin
    public String username = "Steve";
    public String msaRefreshToken = "0";
    public String xuid;
    public String otherBaseUrl;
    public String otherAccount;
    public String otherPassword;
    public String accountType;
    private final String uniqueUUID = UUID.randomUUID().toString().toLowerCase(Locale.ROOT);

    public void updateMicrosoftSkin() {
        updateSkin("https://sessionserver.mojang.com");
    }

    public void updateOtherSkin() {
        updateSkin(StringUtilsKt.removeSuffix(otherBaseUrl, "/") + "/sessionserver/");
    }

    public void updateSkin(String url) {
        File skinFile = new File(PathAndUrlManager.DIR_USER_SKIN, uniqueUUID + ".png");
        if(skinFile.exists()) FileUtils.deleteQuietly(skinFile); //清除一次皮肤文件
        try {
            SkinFileDownloader.yggdrasil(url, skinFile, profileId);
            Logging.i("SkinLoader", "Update skin success");
        } catch (Exception e) {
            Logging.i("SkinLoader", "Could not update skin\n" + Tools.printToString(e));
        }
    }

    /**
     * 保存账号信息结束后，将开始获取皮肤，由于设计网络访问，请勿在UI线程运行！
     */
    public void save() throws IOException {
        Tools.write(PathAndUrlManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID, Tools.GLOBAL_GSON.toJson(this));
        if (AccountUtils.isMicrosoftAccount(this)) updateMicrosoftSkin();
        else if (AccountUtils.isOtherLoginAccount(this)) updateOtherSkin();
    }
    
    public static MinecraftAccount parse(String content) throws JsonSyntaxException {
        return Tools.GLOBAL_GSON.fromJson(content, MinecraftAccount.class);
    }

    public static MinecraftAccount loadFromProfileID(String profileID) {
        for (MinecraftAccount account : AccountsManager.getInstance().getAllAccount()) {
            if (Objects.equals(account.profileId, profileID)) return account;
        }
        return null;
    }

    public static MinecraftAccount loadFromUniqueUUID(String uniqueUUID) {
        if(!accountExists(uniqueUUID)) return null;
        try {
            MinecraftAccount acc = parse(Tools.read(PathAndUrlManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID));
            if (acc.accessToken == null) {
                acc.accessToken = "0";
            }
            if (acc.clientToken == null) {
                acc.clientToken = "0";
            }
            if (acc.profileId == null) {
                acc.profileId = "00000000-0000-0000-0000-000000000000";
            }
            if (acc.username == null) {
                acc.username = "0";
            }
            if (acc.msaRefreshToken == null) {
                acc.msaRefreshToken = "0";
            }
            return acc;
        } catch(IOException | JsonSyntaxException e) {
            Logging.e(MinecraftAccount.class.getName(), "Caught an exception while loading the profile",e);
            return null;
        }
    }

    private static boolean accountExists(String uniqueUUID) {
        return !uniqueUUID.isEmpty() && new File(PathAndUrlManager.DIR_ACCOUNT_NEW + "/" + uniqueUUID).exists();
    }

    public String getUniqueUUID() {
        return this.uniqueUUID;
    }

    @NonNull
    @Override
    public String toString() {
        return "MinecraftAccount{" +
                "username='" + username + '\'' +
                ", accountType=" + accountType +
                '}';
    }
}

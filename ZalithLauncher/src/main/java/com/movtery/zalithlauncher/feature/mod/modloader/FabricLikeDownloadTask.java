package com.movtery.zalithlauncher.feature.mod.modloader;

import com.kdt.mcgui.ProgressLayout;
import com.movtery.zalithlauncher.R;
import com.movtery.zalithlauncher.feature.customprofilepath.ProfilePathHome;
import com.movtery.zalithlauncher.feature.version.InstallTask;
import com.movtery.zalithlauncher.feature.version.VersionConfig;
import com.movtery.zalithlauncher.feature.version.VersionsManager;
import com.movtery.zalithlauncher.utils.PathAndUrlManager;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper;
import net.kdt.pojavlaunch.utils.DownloadUtils;
import net.kdt.pojavlaunch.utils.FileUtils;

import org.json.JSONObject;

import java.io.File;

public class FabricLikeDownloadTask implements InstallTask, Tools.DownloaderFeedback {
    private final FabricLikeUtils mUtils;
    private String mGameVersion = null;
    private String mLoaderVersion = null;

    public FabricLikeDownloadTask(FabricLikeUtils utils) {
        this.mUtils = utils;
    }

    public FabricLikeDownloadTask(FabricLikeUtils utils, String gameVersion, String loaderVersion) {
        this(utils);
        this.mGameVersion = gameVersion;
        this.mLoaderVersion = loaderVersion;
    }

    @Override
    public File run() throws Exception {
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, 0, R.string.mod_download_progress, mUtils.getName());
        File outputFile;
        if (mGameVersion == null && mLoaderVersion == null) {
            outputFile = downloadInstaller();
        }
        else {
            legacyInstall();
            outputFile = null;
        }
        ProgressLayout.clearProgress(ProgressLayout.INSTALL_RESOURCE);
        return outputFile;
    }

    private File downloadInstaller() throws Exception {
        File outputFile = new File(PathAndUrlManager.DIR_CACHE, "fabric-installer.jar");

        String installerDownloadUrl = mUtils.getInstallerDownloadUrl();
        byte[] buffer = new byte[8192];
        DownloadUtils.downloadFileMonitored(installerDownloadUrl, outputFile, buffer, this);

        return outputFile;
    }

    //因为Quilt要用Jre17去跑，跑完之后JVM不会自动退出
    //为了自动化处理，所以暂时这么做
    private void legacyInstall() throws Exception {
        String jsonString = DownloadUtils.downloadString(mUtils.createJsonDownloadUrl(mGameVersion, mLoaderVersion));

        JSONObject jsonObject = new JSONObject(jsonString);
        String versionId = jsonObject.getString("id");

        File versionJsonDir = new File(ProfilePathHome.getVersionsHome(), versionId);
        File versionJsonFile = new File(versionJsonDir, versionId+".json");
        FileUtils.ensureDirectory(versionJsonDir);
        Tools.write(versionJsonFile.getAbsolutePath(), jsonString);

        VersionConfig versionConfig = new VersionConfig(VersionsManager.INSTANCE.getVersionPath(versionId));
        versionConfig.save();
    }

    @Override
    public void updateProgress(int curr, int max) {
        int progress100 = (int)(((float)curr / (float)max)*100f);
        ProgressKeeper.submitProgress(ProgressLayout.INSTALL_RESOURCE, progress100, R.string.mod_download_progress, mUtils.getName());
    }
}

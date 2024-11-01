package com.movtery.zalithlauncher.feature.update;

import static com.movtery.zalithlauncher.task.TaskExecutors.Companion;
import static com.movtery.zalithlauncher.utils.file.FileTools.formatFileSize;
import static net.kdt.pojavlaunch.Architecture.ARCH_ARM;
import static net.kdt.pojavlaunch.Architecture.ARCH_ARM64;
import static net.kdt.pojavlaunch.Architecture.ARCH_X86;
import static net.kdt.pojavlaunch.Architecture.ARCH_X86_64;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.setting.AllSettings;
import com.movtery.zalithlauncher.ui.dialog.ProgressDialog;
import com.movtery.zalithlauncher.ui.dialog.TipDialog;
import com.movtery.zalithlauncher.ui.dialog.UpdateDialog;
import com.movtery.zalithlauncher.utils.PathAndUrlManager;
import com.movtery.zalithlauncher.utils.ZHTools;
import com.movtery.zalithlauncher.utils.http.CallUtils;
import com.movtery.zalithlauncher.utils.stringutils.StringUtils;

import net.kdt.pojavlaunch.BuildConfig;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public final class UpdateLauncher {
    private static final File sApkFile = new File(PathAndUrlManager.DIR_APP_CACHE, "cache.apk");
    public static long LAST_UPDATE_CHECK_TIME = 0;
    private final Context context;
    private final UpdateSource updateSource;
    private final LauncherVersion launcherVersion;
    private final String destinationFilePath;
    private final Call call;
    private ProgressDialog dialog;
    private Timer timer;

    public UpdateLauncher(Context context, LauncherVersion launcherVersion, UpdateSource updateSource) {
        this.context = context;
        this.updateSource = updateSource;
        this.launcherVersion = launcherVersion;

        this.destinationFilePath = sApkFile.getAbsolutePath();
        this.call = new OkHttpClient().newCall(
                PathAndUrlManager.createRequestBuilder(getDownloadUrl()).build()
        ); //获取请求对象
    }

    public static void CheckDownloadedPackage(Context context, boolean ignore) {
        if (!Objects.equals(BuildConfig.BUILD_TYPE, "release")) return;

        if (sApkFile.exists()) {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageArchiveInfo(sApkFile.getAbsolutePath(), 0);

            if (packageInfo != null) {
                String packageName = packageInfo.packageName;
                int versionCode = packageInfo.versionCode;
                int thisVersionCode = ZHTools.getVersionCode();

                if (Objects.equals(packageName, ZHTools.getPackageName()) && versionCode > thisVersionCode) {
                    installApk(context, sApkFile);
                } else {
                    FileUtils.deleteQuietly(sApkFile);
                }
            } else {
                FileUtils.deleteQuietly(sApkFile);
            }
        } else {
            //如果安装包不存在，那么将自动获取更新
            UpdateLauncher.updateCheckerMainProgram(context, ignore);
        }
    }

    private static void installApk(Context context, File outputFile) {
        Companion.runInUIThread(() -> new TipDialog.Builder(context)
                .setMessage(StringUtils.insertNewline(context.getString(R.string.update_success), outputFile.getAbsolutePath()))
                .setCenterMessage(false)
                .setCancelable(false)
                .setConfirmClickListener(() -> { //安装
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", outputFile);
                    intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                }).buildDialog());
    }

    public static synchronized void updateCheckerMainProgram(Context context, boolean ignore) {
        if (ZHTools.getCurrentTimeMillis() - LAST_UPDATE_CHECK_TIME <= 5000) return;
        LAST_UPDATE_CHECK_TIME = ZHTools.getCurrentTimeMillis();

        String token = context.getString(R.string.api_token);
        new CallUtils(new CallUtils.CallbackListener() {
            @Override
            public void onFailure(Call call) {
                showFailToast(context, context.getString(R.string.update_fail));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showFailToast(context, context.getString(R.string.update_fail_code, response.code()));
                    Logging.e("UpdateLauncher", "Unexpected code " + response.code());
                } else {
                    Objects.requireNonNull(response.body());
                    String responseBody = response.body().string(); //解析响应体
                    try {
                        JSONObject jsonObject = new JSONObject(responseBody);
                        String rawBase64 = jsonObject.getString("content");
                        String rawJson = StringUtils.decodeBase64(rawBase64);

                        LauncherVersion launcherVersion = Tools.GLOBAL_GSON.fromJson(rawJson, LauncherVersion.class);

                        String versionName = launcherVersion.getVersionName();
                        if (ignore && Objects.equals(versionName, AllSettings.Companion.getIgnoreUpdate()))
                            return; //忽略此版本

                        int versionCode = launcherVersion.getVersionCode();
                        if (ZHTools.getVersionCode() < versionCode) {
                            Companion.runInUIThread(() -> new UpdateDialog(context, launcherVersion).show());
                        } else if (!ignore) {
                            Companion.runInUIThread(() -> {
                                String nowVersionName = ZHTools.getVersionName();
                                Companion.runInUIThread(() -> Toast.makeText(context,
                                        StringUtils.insertSpace(context.getString(R.string.update_without), nowVersionName),
                                        Toast.LENGTH_SHORT).show());
                            });
                        }
                    } catch (Exception e) {
                        Logging.e("Check Update", Tools.printToString(e));
                    }
                }
            }
        }, PathAndUrlManager.URL_GITHUB_UPDATE, token.equals("DUMMY") ? null : token).enqueue();
    }

    private static void showFailToast(Context context, String resString) {
        Companion.runInUIThread(() -> Toast.makeText(context, resString, Toast.LENGTH_SHORT).show());
    }

    private static String getArchModel() {
        int arch = Tools.DEVICE_ARCHITECTURE;
        if (arch == ARCH_ARM64) return "arm64-v8a";
        if (arch == ARCH_ARM) return "armeabi-v7a";
        if (arch == ARCH_X86_64) return "x86_64";
        if (arch == ARCH_X86) return "x86";
        return null;
    }

    public static long getFileSize(LauncherVersion.FileSize fileSize) {
        int arch = Tools.DEVICE_ARCHITECTURE;
        if (arch == ARCH_ARM64) return fileSize.getArm64();
        if (arch == ARCH_ARM) return fileSize.getArm();
        if (arch == ARCH_X86_64) return fileSize.getX86_64();
        if (arch == ARCH_X86) return fileSize.getX86();
        return fileSize.getAll();
    }

    private String getDownloadUrl() {
        String fileUrl;
        String archModel = getArchModel();
        String githubUrl = "github.com/MovTery/ZalithLauncher/releases/download/" + launcherVersion.getVersionCode() + "/" + "ZalithLauncher-" + launcherVersion.getVersionName() +
                (archModel != null ? String.format("-%s", archModel) : "") + ".apk";
        switch (updateSource) {
            case GHPROXY:
                fileUrl = "https://mirror.ghproxy.com/" + githubUrl;
                break;
            case GITHUB_RELEASE:
            default:
                fileUrl = "https://" + githubUrl;
                break;
        }
        return fileUrl;
    }

    public void start() {
        this.call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                showFailToast(context, context.getString(R.string.update_fail));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    showFailToast(context, context.getString(R.string.update_fail_code, response.code()));
                    throw new IOException("Unexpected code " + response);
                } else {
                    File outputFile = new File(UpdateLauncher.this.destinationFilePath);
                    Objects.requireNonNull(response.body());
                    try (InputStream inputStream = response.body().byteStream();
                         OutputStream outputStream = Files.newOutputStream(outputFile.toPath())
                    ) {
                        byte[] buffer = new byte[1024 * 1024];
                        int bytesRead;

                        Companion.runInUIThread(() -> {
                            UpdateLauncher.this.dialog = new ProgressDialog(UpdateLauncher.this.context, () -> {
                                UpdateLauncher.this.stop();
                                return true;
                            });
                            UpdateLauncher.this.dialog.show();
                        });

                        final long[] downloadedSize = new long[1];
                        final long[] lastSize = {0};
                        final long[] lastTime = {ZHTools.getCurrentTimeMillis()};

                        //限制刷新速度
                        timer = new Timer();
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                long size = downloadedSize[0];
                                long currentTime = ZHTools.getCurrentTimeMillis();
                                double timeElapsed = (currentTime - lastTime[0]) / 1000.0;
                                long sizeChange = size - lastSize[0];
                                long rate = (long) (sizeChange / timeElapsed);

                                lastSize[0] = size;
                                lastTime[0] = currentTime;

                                Companion.runInUIThread(() -> {
                                    String formattedDownloaded = formatFileSize(size);
                                    String totalSize = formatFileSize(getFileSize(launcherVersion.getFileSize()));
                                    UpdateLauncher.this.dialog.updateProgress(size, getFileSize(launcherVersion.getFileSize()));
                                    UpdateLauncher.this.dialog.updateRate(rate > 0 ? rate : 0L);
                                    UpdateLauncher.this.dialog.updateText(String.format(context.getString(R.string.update_downloading), formattedDownloaded, totalSize));
                                });
                            }
                        }, 0, 120);

                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                            downloadedSize[0] += bytesRead;
                        }
                        finish(outputFile);
                    }
                }
            }
        });
    }

    private void finish(File outputFile) {
        Companion.runInUIThread(UpdateLauncher.this.dialog::dismiss);
        timer.cancel();

        installApk(context, outputFile);
    }

    private void stop() {
        this.call.cancel();
        this.timer.cancel();
        FileUtils.deleteQuietly(sApkFile);
    }

    public enum UpdateSource {
        GITHUB_RELEASE, GHPROXY
    }
}

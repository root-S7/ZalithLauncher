package com.root.direct.install.utils;

import static android.content.Intent.*;
import static android.os.Build.*;
import static android.widget.Toast.*;
import static com.root.direct.install.utils.StringUtils.*;
import static com.root.direct.install.utils.path.AssetsPathKt.*;

import static net.kdt.pojavlaunch.PojavApplication.getINSTANCE;
import static net.kdt.pojavlaunch.customcontrols.LayoutConverter.*;
import static java.nio.charset.StandardCharsets.*;
import static java.nio.file.Files.*;
import static java.util.Objects.requireNonNullElse;

import android.content.*;
import android.content.res.*;
import android.net.Uri;
import android.widget.*;

import net.kdt.pojavlaunch.customcontrols.CustomControls;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.*;

public class DIUtils {
    private static String SOC_NAME;
    public static String getSocName() {
        if(isEffective(SOC_NAME)) return SOC_NAME;
        Process process = null;

        try {
            process = Runtime.getRuntime().exec("getprop ro.soc.model");
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                SOC_NAME = reader.readLine();
            }
        }catch(Exception ignore) {
            SOC_NAME = HARDWARE;
        }finally {
            if(process != null) process.destroy();
        }
        return isEffective(SOC_NAME) ? SOC_NAME : HARDWARE;
    }

    public static void directOpenURL(Context context, String link) {
        if(!isEffective(context, link)) return;

        try {
            Intent intent = new Intent(ACTION_VIEW, Uri.parse(link));
            context.startActivity(intent);
        }catch(Exception ignored) {
            Toast.makeText(context, "无效的链接或未安装浏览器！", LENGTH_SHORT).show();
        }
    }

    public static void joinQQGroup(Context context, String key) {
        if(!isEffective(key, context)) return;

        Intent intent = new Intent(ACTION_VIEW, Uri.parse("mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D" + key));
        try {
            context.startActivity(intent);
        }catch(Exception ignored) {
            Toast.makeText(context, "无效的API或未安装QQ！", LENGTH_SHORT).show();
        }
    }

    public static void copyAssets(Context context, String src, String dest) throws IOException {
        if(context == null || src == null || dest == null) return;
        AssetManager assetManager = context.getAssets();
        String[] fileNames = assetManager.list(src);

        if(fileNames != null && fileNames.length > 0) {
            File destDir = new File(dest);
            if(!destDir.exists() && !destDir.mkdirs()) throw new IOException("无法创建“" + dest + "”目录");

            for(String fileName : fileNames) {
                String newSrc = src.isEmpty() ? fileName : src + "/" + fileName;
                String newDest = dest + File.separator + fileName;
                copyAssets(context, newSrc, newDest);
            }
        }else {
            File outFile = new File(dest);
            File parentDir = outFile.getParentFile();
            if(parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) throw new IOException("无法创建父目录『" + parentDir.getAbsolutePath() + '』');

            try(BufferedInputStream bis = new BufferedInputStream(assetManager.open(src), 8192);
                 BufferedOutputStream bos = new BufferedOutputStream(newOutputStream(outFile.toPath()), 8192)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while((bytesRead = bis.read(buffer)) != -1) bos.write(buffer, 0, bytesRead);
            }
        }
    }

    /**
     * 不使用context读取APK下assets内文件
     * 注意，若APK特别大请勿使用该方法，否则导致性能异常
     * @param assPath assets目录下对应文件
     * @return 数据流
     * @throws Exception 所有异常均抛出
    **/
    @Deprecated(since = "1.4.0.8", forRemoval = true)
    public static InputStream openAssets(String assPath) throws Exception {
        InputStream is = DIUtils.class.getResourceAsStream(addPrefix(assPath));
        if(is == null) throw new FileNotFoundException("『" + assPath + "』文件不存在");
        return is;
    }

    /**
     * 全新的方式读取APK下assets内文件
     * @param assPath assets目录下对应文件
     * @return 数据流
     * @throws Exception 所有异常均抛出
    **/
    public static InputStream openAssets(Context context, String assPath) throws Exception {
        try {
            return requireNonNullElse(context, getINSTANCE()).getAssets().open(assPath);
        }catch(FileNotFoundException e) {
            throw new FileNotFoundException("『" + assPath + "』文件不存在");
        }
    }

    /**
     * 读取APK的assets下指定控制器文件
     * @param jsonName 文件名称（对应assets目录下）
     * @return 解析成功的对象（若不成功返回null）
     * @throws FileNotFoundException 若文件不存在抛出异常
    **/
    public static CustomControls loadFromAssets(String jsonName) throws FileNotFoundException {
        try(InputStream is = openAssets(null, jsonName)) {
            String string = IOUtils.toString(is, UTF_8);

            JSONObject layoutObj = new JSONObject(string);
            return loadAndConvertIfNecessary(getINSTANCE(), layoutObj, string, null, true);
        }catch(FileNotFoundException e) {
            throw e;
        }catch(Exception ignore) {
            return null;
        }
    }
}
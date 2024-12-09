package net.kdt.pojavlaunch.utils;

import static org.lwjgl.glfw.CallbackBridge.windowHeight;
import static org.lwjgl.glfw.CallbackBridge.windowWidth;

import android.os.Build;
import android.os.FileObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.movtery.zalithlauncher.event.single.MCOptionChangeEvent;
import com.movtery.zalithlauncher.feature.log.Logging;
import com.movtery.zalithlauncher.feature.version.Version;
import com.movtery.zalithlauncher.event.sticky.RunningVersionEvent;

import net.kdt.pojavlaunch.Tools;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MCOptionUtils {
    private static final HashMap<String,String> sParameterMap = new HashMap<>();
    private static FileObserver sFileObserver;
    private static Version sVersion = null;

    public static void load(@NonNull Version version) {
        File optionFile = new File(version.getGameDir().getAbsolutePath() + "/options.txt");
        if(!optionFile.exists()) {
            try { // Needed for new instances I guess  :think:
                optionFile.createNewFile();
            } catch (IOException e) {
                Logging.e("MCOptionUtils", Tools.printToString(e));
            }
        }

        if(sFileObserver == null || !Objects.equals(sVersion, version)){
            sVersion = version;
            setupFileObserver();
        }
        sVersion = version; // Yeah I know, it may be redundant

        sParameterMap.clear();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(optionFile));
            String line;
            while ((line = reader.readLine()) != null) {
                int firstColonIndex = line.indexOf(':');
                if(firstColonIndex < 0) {
                    Logging.w(Tools.APP_NAME, "No colon on line \""+line+"\", skipping");
                    continue;
                }
                sParameterMap.put(line.substring(0,firstColonIndex), line.substring(firstColonIndex+1));
            }
            reader.close();
        } catch (IOException e) {
            Logging.w(Tools.APP_NAME, "Could not load options.txt", e);
        }
    }

    public static void set(String key, String value) {
        sParameterMap.put(key,value);
    }

    /** Set an array of String, instead of a simple value. Not supported on all options */
    public static void set(String key, List<String> values){
        sParameterMap.put(key, values.toString());
    }

    public static String get(String key){
        return sParameterMap.get(key);
    }

    public static boolean containsKey(String key) {
        return sParameterMap.containsKey(key);
    }

    /** @return A list of values from an array stored as a string */
    public static List<String> getAsList(String key){
        String value = get(key);

        // Fallback if the value doesn't exist
        if (value == null) return new ArrayList<>();

        // Remove the edges
        value = value.replace("[", "").replace("]", "");
        if (value.isEmpty()) return new ArrayList<>();

        return Arrays.asList(value.split(","));
    }

    public static void save() {
        StringBuilder result = new StringBuilder();
        for(String key : sParameterMap.keySet())
            result.append(key)
                    .append(':')
                    .append(sParameterMap.get(key))
                    .append('\n');

        try {
            sFileObserver.stopWatching();
            Tools.write(sVersion.getGameDir().getAbsolutePath() + "/options.txt", result.toString());
            sFileObserver.startWatching();
        } catch (IOException e) {
            Logging.w(Tools.APP_NAME, "Could not save options.txt", e);
        }
    }

    /** @return The stored Minecraft GUI scale, also auto-computed if on auto-mode or improper setting */
    public static int getMcScale() {
        String str = MCOptionUtils.get("guiScale");
        int guiScale = (str == null ? 0 :Integer.parseInt(str));

        int scale = Math.max(Math.min(windowWidth / 320, windowHeight / 240), 1);
        if(scale < guiScale || guiScale == 0){
            guiScale = scale;
        }

        return guiScale;
    }

    /** Add a file observer to reload options on file change
     * Listeners get notified of the change */
    private static void setupFileObserver() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            sFileObserver = new FileObserver(new File(sVersion.getGameDir().getAbsolutePath() + "/options.txt"), FileObserver.MODIFY) {
                @Override
                public void onEvent(int i, @Nullable String s) {
                    Version version = loadVersionFromEvent();
                    if (version == null) return;
                    MCOptionUtils.load(version);
                    EventBus.getDefault().post(new MCOptionChangeEvent());
                }
            };
        } else {
            sFileObserver = new FileObserver(sVersion.getGameDir().getAbsolutePath() + "/options.txt", FileObserver.MODIFY) {
                @Override
                public void onEvent(int i, @Nullable String s) {
                    Version version = loadVersionFromEvent();
                    if (version == null) return;
                    MCOptionUtils.load(version);
                    EventBus.getDefault().post(new MCOptionChangeEvent());
                }
            };
        }

        sFileObserver.startWatching();
    }

    private static Version loadVersionFromEvent() {
        RunningVersionEvent event = EventBus.getDefault().getStickyEvent(RunningVersionEvent.class);
        if (event != null) {
            return event.getVersion();
        } else return null;
    }
}

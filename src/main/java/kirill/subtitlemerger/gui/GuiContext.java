package kirill.subtitlemerger.gui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import kirill.subtitlemerger.logic.ffmpeg.Ffmpeg;
import kirill.subtitlemerger.logic.ffmpeg.FfmpegException;
import kirill.subtitlemerger.logic.ffmpeg.Ffprobe;
import kirill.subtitlemerger.logic.settings.SettingType;
import kirill.subtitlemerger.logic.settings.Settings;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.Set;

import static kirill.subtitlemerger.logic.settings.SettingType.*;

@CommonsLog
@Getter
public class GuiContext {
    private Settings settings;

    /**
     * Settings required for merging in videos.
     */
    private ObservableSet<SettingType> missingSettings;

    @Setter
    private Ffprobe ffprobe;

    @Setter
    private Ffmpeg ffmpeg;

    @Getter(value = AccessLevel.NONE)
    private BooleanProperty videosInProgress;

    public GuiContext() {
        settings = new Settings();
        missingSettings = getMissingSettings(settings);

        ffprobe = getPackedFfprobe();
        ffmpeg = getPackedFfmpegFile();
        videosInProgress = new SimpleBooleanProperty(false);
    }

    private static ObservableSet<SettingType> getMissingSettings(Settings settings) {
        Set<SettingType> result = EnumSet.noneOf(SettingType.class);

        if (settings.getUpperLanguage() == null) {
            result.add(UPPER_LANGUAGE);
        }

        if (settings.getLowerLanguage() == null) {
            result.add(LOWER_LANGUAGE);
        }

        if (settings.getMergeMode() == null) {
            result.add(MERGE_MODE);
        }

        return FXCollections.observableSet(result);
    }

    private static Ffprobe getPackedFfprobe() {
        File directoryWithFfmpeg = getDirectoryWithFfmpeg();

        File ffprobeFile;
        if (SystemUtils.IS_OS_LINUX) {
            ffprobeFile = new File(directoryWithFfmpeg, "ffprobe");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ffprobeFile = new File(directoryWithFfmpeg, "bin/ffprobe.exe");
        } else {
            log.error("operating system is not supported: " + SystemUtils.OS_NAME);
            throw new IllegalStateException();
        }

        if (!ffprobeFile.exists()) {
            log.error("ffprobe file " + ffprobeFile.getAbsolutePath() + " doesn't exist");
            throw new IllegalStateException();
        }

        try {
            return new Ffprobe(ffprobeFile.getAbsoluteFile());
        } catch (FfmpegException e) {
            log.warn("incorrect path to ffprobe: " + e.getCode() + ", console output " + e.getConsoleOutput());
            throw new IllegalStateException();
        } catch (InterruptedException e) {
            log.error("the process can't be interrupted, probably a bug");
            throw new IllegalStateException();
        }
    }

    private static File getDirectoryWithFfmpeg() {
        File jar;
        try {
            jar = new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            log.error("failed to get jar location: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        File directoryWithJar = jar.getParentFile();
        if (directoryWithJar == null) {
            log.error("directory with jar is null, that shouldn't happen");
            throw new NullPointerException();
        }

        File result = new File(directoryWithJar, "ffmpeg");
        if (result.isDirectory()) {
            return result;
        } else {
            /*
             * If we got here it means that either folder with the application is corrupted (folder with ffmpeg has been
             * deleted) or the application is launched from IntelliJ IDEA.
             */
            result = getDirectoryWithFfmpegIdea(directoryWithJar);
            if (result == null) {
                log.error("directory with ffmpeg and ffprobe doesn't exist");
                throw new IllegalStateException();
            }

            return result;
        }
    }

    @Nullable
    private static File getDirectoryWithFfmpegIdea(File directoryWithJar) {
        File sourceDirectory = directoryWithJar.getParentFile();
        if (!sourceDirectory.isDirectory()) {
            return null;
        }

        String subDirectory;
        if (SystemUtils.IS_OS_LINUX) {
            subDirectory = "linux_64";
        } else if (SystemUtils.IS_OS_WINDOWS) {
            subDirectory = "win_64";
        } else {
            log.error("operating system is not supported: " + SystemUtils.OS_NAME);
            throw new IllegalStateException();
        }

        File result = new File(sourceDirectory, "build_parts/downloads/ffmpeg/" + subDirectory);
        if (result.isDirectory()) {
            return result;
        }

        return null;
    }

    private static Ffmpeg getPackedFfmpegFile() {
        File directoryWithFfmpeg = getDirectoryWithFfmpeg();

        File ffmpegFile;
        if (SystemUtils.IS_OS_LINUX) {
            ffmpegFile = new File(directoryWithFfmpeg, "ffmpeg");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ffmpegFile = new File(directoryWithFfmpeg, "bin/ffmpeg.exe");
        } else {
            log.error("operating system is not supported: " + SystemUtils.OS_NAME);
            throw new IllegalStateException();
        }

        if (!ffmpegFile.exists()) {
            log.error("ffmpeg file " + ffmpegFile.getAbsolutePath() + " doesn't exist");
            throw new IllegalStateException();
        }

        try {
            return new Ffmpeg(ffmpegFile.getAbsoluteFile());
        } catch (FfmpegException e) {
            log.warn("incorrect path to ffmpeg: " + e.getCode() + ", console output " + e.getConsoleOutput());
            throw new IllegalStateException();
        } catch (InterruptedException e) {
            log.error("the process can't be interrupted, most likely a bug");
            throw new IllegalStateException();
        }
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public boolean getVideosInProgress() {
        return videosInProgress.get();
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public BooleanProperty videosInProgressProperty() {
        return videosInProgress;
    }

    @SuppressWarnings({"unused", "WeakerAccess", "RedundantSuppression"})
    public void setVideosInProgress(boolean videosInProgress) {
        this.videosInProgress.set(videosInProgress);
    }
}

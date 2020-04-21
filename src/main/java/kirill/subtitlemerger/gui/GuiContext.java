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
        missingSettings = generateMissingSettings(settings);

        ffprobe = getPackedFfprobe();
        ffmpeg = getPackedFfmpegFile();
        videosInProgress = new SimpleBooleanProperty(false);
    }

    private static ObservableSet<SettingType> generateMissingSettings(Settings settings) {
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
        File directoryWithJar = getDirectoryWithJar();

        File ffprobeFile;
        if (SystemUtils.IS_OS_LINUX) {
            ffprobeFile = new File(directoryWithJar, "ffmpeg/ffprobe");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ffprobeFile = new File(directoryWithJar, "ffmpeg/bin/ffprobe.exe");
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
            log.error("something's not right, process can't be interrupted");
            throw new IllegalStateException();
        }
    }

    /**
     * @return the directory containing jar file that is running.
     * @throws IllegalStateException if directory can't be located for some reason
     */
    private static File getDirectoryWithJar() {
        File jar;
        try {
            jar = new File(Settings.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            log.error("failed to get jar location: " + ExceptionUtils.getStackTrace(e));
            throw new IllegalStateException();
        }

        File result = jar.getParentFile();
        if (result == null) {
            log.error("folder with jar is null, that shouldn't happen");
            throw new NullPointerException();
        }

        return result;
    }

    private static Ffmpeg getPackedFfmpegFile() {
        File directoryWithJar = getDirectoryWithJar();

        File ffmpegFile;
        if (SystemUtils.IS_OS_LINUX) {
            ffmpegFile = new File(directoryWithJar, "ffmpeg/ffmpeg");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            ffmpegFile = new File(directoryWithJar, "ffmpeg/bin/ffmpeg.exe");
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
            log.error("something's not right, process can't be interrupted");
            throw new IllegalStateException();
        }
    }

    public boolean getVideosInProgress() {
        return videosInProgress.get();
    }

    public BooleanProperty videosInProgressProperty() {
        return videosInProgress;
    }

    public void setVideosInProgress(boolean videosInProgress) {
        this.videosInProgress.set(videosInProgress);
    }
}

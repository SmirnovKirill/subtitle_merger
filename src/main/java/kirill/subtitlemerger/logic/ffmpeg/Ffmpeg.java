package kirill.subtitlemerger.logic.ffmpeg;

import com.neovisionaries.i18n.LanguageAlpha3Code;
import kirill.subtitlemerger.logic.core.SubRipWriter;
import kirill.subtitlemerger.logic.core.entities.Subtitles;
import kirill.subtitlemerger.logic.utils.process.ProcessException;
import kirill.subtitlemerger.logic.utils.process.ProcessRunner;
import kirill.subtitlemerger.logic.file_info.entities.FfmpegSubtitleStream;
import kirill.subtitlemerger.logic.file_info.entities.FileInfo;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@CommonsLog
public class Ffmpeg {
    private static final File TEMP_SUBTITLE_FILE = new File(
            System.getProperty("java.io.tmpdir"),
            "subtitle_merger_temp.srt"
    );

    private File ffmpegFile;

    public Ffmpeg(File ffmpegFile) throws FfmpegException, InterruptedException {
        validate(ffmpegFile);

        this.ffmpegFile = ffmpegFile;
    }

    public static void validate(File ffmpegFile) throws FfmpegException, InterruptedException {
        try {
            List<String> arguments = Arrays.asList(
                    ffmpegFile.getAbsolutePath(),
                    "-version"
            );

            log.debug("run ffmpeg " + StringUtils.join(arguments, " "));
            String consoleOutput = ProcessRunner.run(arguments);
            log.debug("ffmpeg console output: " + consoleOutput);

            if (!consoleOutput.startsWith("ffmpeg version")) {
                log.info("console output doesn't start with the ffmpeg version");
                throw new FfmpegException(FfmpegException.Code.INCORRECT_FFMPEG_PATH, consoleOutput);
            }
        } catch (ProcessException e) {
            log.warn("failed to check ffmpeg: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.INCORRECT_FFMPEG_PATH, e.getConsoleOutput());
        }
    }

    /*
     * Synchronized because we use one temporary file with subtitles.
     */
    public synchronized String getSubtitleText(
            int ffmpegStreamIndex,
            File videoFile
    ) throws FfmpegException, InterruptedException {
        String consoleOutput;
        try {
            /*
             * We have to pass -y to agree with file overwriting, it's always required
             * because java will have created temporary file by the time ffmpeg is called.
             */
            List<String> arguments = Arrays.asList(
                    ffmpegFile.getAbsolutePath(),
                    "-y",
                    "-i",
                    videoFile.getAbsolutePath(),
                    "-map",
                    "0:" + ffmpegStreamIndex,
                    TEMP_SUBTITLE_FILE.getAbsolutePath()
            );

            log.debug("run ffmpeg " + StringUtils.join(arguments, " "));
            consoleOutput = ProcessRunner.run(arguments);
            log.debug("ffmpeg console output: " + consoleOutput);
        } catch (ProcessException e) {
            log.warn("failed to extract subtitles with ffmpeg: " + e.getCode());
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, e.getConsoleOutput());
        }

        try {
            return FileUtils.readFileToString(TEMP_SUBTITLE_FILE, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("failed to read subtitles from the file: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, consoleOutput);
        }
    }

    /*
     * Synchronized because we use one temporary file with subtitles.
     */
    public synchronized void injectSubtitlesToFile(
            Subtitles subtitles,
            String title,
            LanguageAlpha3Code mainLanguage,
            boolean makeDefault,
            File directoryForTempFile,
            FileInfo fileInfo
    ) throws FfmpegException, InterruptedException {
        /*
         * Ffmpeg can't add subtitles on the fly. So we need to add subtitles to some temporary file
         * and then rename it. Later we'll also check that the size of the new file is bigger than the size of the
         * original one because it's important not to spoil the original video file, it may be valuable.
         */
        File outputTemp = new File(directoryForTempFile, "temp_" + fileInfo.getFile().getName());

        try {
            FileUtils.writeStringToFile(
                    TEMP_SUBTITLE_FILE,
                    SubRipWriter.toText(subtitles),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            log.warn("failed to write merged subtitles to the temp file: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, null);
        }

        try {
            String consoleOutput;
            try {
                List<String> arguments = getArgumentsInjectToFile(
                        title,
                        mainLanguage,
                        makeDefault,
                        fileInfo,
                        outputTemp
                );

                log.debug("run ffmpeg " + StringUtils.join(arguments, " "));
                consoleOutput = ProcessRunner.run(arguments);
                log.debug("ffmpeg console output: " + consoleOutput);
            } catch (ProcessException e) {
                log.warn("failed to inject subtitles with ffmpeg: " + e.getCode());
                throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, e.getConsoleOutput());
            }

            if (outputTemp.length() <= fileInfo.getFile().length()) {
                log.error("resulting file size is less than the original one");
                throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, consoleOutput);
            }

            overwriteOriginalVideo(outputTemp, fileInfo.getFile(), consoleOutput);
        } finally {
            if (outputTemp.exists() && !outputTemp.delete()) {
                log.warn("failed to delete temp video file " + outputTemp.getAbsolutePath());
            }
        }
    }

    private List<String> getArgumentsInjectToFile(
            String title,
            LanguageAlpha3Code mainLanguage,
            boolean makeDefault,
            FileInfo fileInfo,
            File outputTemp
    ) {
        List<String> result = new ArrayList<>();

        int newStreamIndex = fileInfo.getFfmpegSubtitleStreams().size();

        result.add(ffmpegFile.getAbsolutePath());
        result.add("-y");

        /*
         * Haven't seen such scenario in practice but I've looked this argument up in the ffmpeg documentation
         * and decided that it's worth adding. Because if there are some unknown streams in the file it's better
         * to add them as is, our program is designed just for injecting subtitles, leave everything else untouched.
         */
        result.add("-copy_unknown");

        result.addAll(Arrays.asList("-i", fileInfo.getFile().getAbsolutePath()));
        result.addAll(Arrays.asList("-i", Ffmpeg.TEMP_SUBTITLE_FILE.getAbsolutePath()));
        result.addAll(Arrays.asList("-c", "copy"));

        /*
         * Very important! Without this argument many videos won't work:
         * https://video.stackexchange.com/questions/28719/srt-subtitles-added-to-mkv-with-ffmpeg-are-not-displayed
         */
        result.addAll(Arrays.asList("-max_interleave_delta", "0"));

        if (mainLanguage != null) {
            result.addAll(Arrays.asList("-metadata:s:s:" + newStreamIndex, "language=" + mainLanguage));
        }

        result.addAll(Arrays.asList("-metadata:s:s:" + newStreamIndex, "title=" + title));

        if (makeDefault) {
            if (!CollectionUtils.isEmpty(fileInfo.getFfmpegSubtitleStreams())) {
                for (FfmpegSubtitleStream ffmpegStream : fileInfo.getFfmpegSubtitleStreams()) {
                    if (ffmpegStream.isDefaultDisposition()) {
                        result.addAll(Arrays.asList("-disposition:" + ffmpegStream.getFfmpegIndex(), "0"));
                    }
                }
            }

            result.addAll(Arrays.asList("-disposition:s:" + newStreamIndex, "default"));
        }

        result.addAll(Arrays.asList("-map", "0"));
        result.addAll(Arrays.asList("-map", "1"));
        result.add(outputTemp.getAbsolutePath());

        return result;
    }

    private static void overwriteOriginalVideo(
            File outputTemp,
            File videoFile,
            String consoleOutput
    ) throws FfmpegException {
        /*
         * Save this flag here to restore it at the end of the method. Because otherwise if the file has had only
         * read access initially we will give it write access as well before renaming, and leave it like that.
         */
        boolean originallyWritable = videoFile.canWrite();

        if (!videoFile.setWritable(true, true)) {
            log.warn("failed to make video file " + videoFile.getAbsolutePath() + " writable");
            throw new FfmpegException(FfmpegException.Code.GENERAL_ERROR, consoleOutput);
        }

        try {
            Files.move(outputTemp.toPath(), videoFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("failed to move temp video: " + ExceptionUtils.getStackTrace(e));
            throw new FfmpegException(FfmpegException.Code.FAILED_TO_MOVE_TEMP_VIDEO, consoleOutput);
        }

        if (!originallyWritable) {
            if (!videoFile.setWritable(false, true)) {
                log.warn("failed to make video file " + videoFile.getAbsolutePath() + " not writable");
            }
        }
    }
}

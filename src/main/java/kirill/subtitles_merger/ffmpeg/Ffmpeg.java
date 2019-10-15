package kirill.subtitles_merger.ffmpeg;

import kirill.subtitles_merger.FfmpegException;
import kirill.subtitles_merger.ffprobe.FfpProbeSubtitleStream;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@CommonsLog
public class Ffmpeg {
    private static final File TEMP_SUBTITLE_FILE = new File(System.getProperty("java.io.tmpdir"), "subtitles_merger_temp.srt");
    private String path;

    public Ffmpeg(String path) {
        this.path = path;
        //todo валидация
    }

    public synchronized String getSubtitlesText(FfpProbeSubtitleStream ffpProbeSubtitleStream, File videoFile) throws IOException, FfmpegException, InterruptedException {
        File result = File.createTempFile("subtitles_merger_", ".srt");

        /*
         * -y нужно передавать чтобы дать согласие на перезаписывание файла, это всегда нужно делать потому что временный
         * файл уже будет создан джавой на момент вызова ffmpeg.
         */
        ProcessBuilder processBuilder = new ProcessBuilder(
                Arrays.asList(
                        path,
                        "-i",
                        videoFile.getAbsolutePath(),
                        "-y",
                        "-map",
                        "0:" + ffpProbeSubtitleStream.getIndex(),
                        TEMP_SUBTITLE_FILE.getAbsolutePath()
                )
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        } finally {
            process.destroy();
        }

        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            log.error("ffmpeg exits for too long, that's weird");
            throw new FfmpegException("ffmpeg exits for too long");
        }

        if (process.exitValue() != 0) {
            log.error("ffmpeg exited with code " + process.exitValue() + ": " + output.toString());
            throw new FfmpegException("ffmpeg exited with code " + process.exitValue());
        }

        return FileUtils.readFileToString(TEMP_SUBTITLE_FILE);
    }

    //todo переделать, пока наспех
    public void addSubtitleToFile(String text, File videoFile) throws IOException, FfmpegException, InterruptedException {
        File temp = File.createTempFile("subtitles_merger_", ".srt");

        FileUtils.writeStringToFile(temp, text);

        ProcessBuilder processBuilder = new ProcessBuilder(
                Arrays.asList(
                        path,
                        "-i",
                        videoFile.getAbsolutePath(),
                        "-i",
                        temp.getAbsolutePath(),
                        "-y",
                        "-c",
                        "copy",
                        "-c:s",
                        "mov_text",
                        videoFile.getAbsolutePath()
                )
        );

        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        StringBuilder output = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        } finally {
            process.destroy();
        }

        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            log.error("ffmpeg exits for too long, that's weird");
            throw new FfmpegException("ffmpeg exits for too long");
        }

        if (process.exitValue() != 0) {
            log.error("ffmpeg exited with code " + process.exitValue() + ": " + output.toString());
            throw new FfmpegException("ffmpeg exited with code " + process.exitValue());
        }
    }
}

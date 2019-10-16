package kirill.subtitles_merger.ffmpeg;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
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

    public synchronized String getSubtitlesText(int streamIndex, File videoFile) throws FfmpegException, InterruptedException {
        try {
            /*
             * -y нужно передавать чтобы дать согласие на перезаписывание файла, это всегда нужно делать потому что временный
             * файл уже будет создан джавой на момент вызова ffmpeg.
             */
            ProcessBuilder processBuilder = new ProcessBuilder(
                    Arrays.asList(
                            path,
                            "-y",
                            "-i",
                            videoFile.getAbsolutePath(),
                            "-map",
                            "0:" + streamIndex, //todo возможно добавить тут явное указание кодека
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
                throw new IllegalStateException();
            }

            if (process.exitValue() != 0) {
                log.error("ffmpeg exited with code " + process.exitValue() + ": " + output.toString());
                throw new IllegalStateException();
            }

            return FileUtils.readFileToString(TEMP_SUBTITLE_FILE);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new FfmpegException(e);
        }
    }

   /* //todo переделать, пока наспех
    public void addSubtitleToFile(String text, File videoFile) throws IOException, FfmpegException, InterruptedException {
        File subtitlesTemp = File.createTempFile("subtitles_merger_", ".srt");

        *//*
         * Ffmpeg не может добавить к файлу субтитры и записать это в тот же файл.
         * Поэтому нужно сначала записать результат во временный файл а потом его переименовать.
         * На всякий случай еще сделаем проверку что новый файл больше чем старый, а то нехорошо будет если испортим
         * видео, его могли долго качать.
         *//*
        File outputTemp = new File(videoFile.getParentFile(), "temp_" + videoFile.getName());

        FileUtils.writeStringToFile(subtitlesTemp, text);

        ProcessBuilder processBuilder = new ProcessBuilder(
                Arrays.asList(
                        path,
                        "-y",
                        "-i",
                        videoFile.getAbsolutePath(),
                        "-i",
                        subtitlesTemp.getAbsolutePath(),
                        "-map",
                        "0",
                        "-c",
                        "copy",
                        "-map",
                        "1",
                        "-c:s",
                        "copy",
                        outputTemp.getAbsolutePath()
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

        if (outputTemp.length() <= videoFile.length()) {
            log.error("resulting file size is less than original one");
            throw new IllegalStateException();
        }

        //Files.move(outputTemp.toPath(), videoFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }*/
}

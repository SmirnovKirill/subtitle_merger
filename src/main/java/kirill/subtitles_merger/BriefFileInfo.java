package kirill.subtitles_merger;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.util.List;

/**
 * В классе содержится основная информация о файле, в общем-то вся информация кроме той которую можно получить с
 * запуском ffmpeg (потому что это долго). В частности, тут хранится список субтитров и информация о них из ffprobe.
 * Для заполнения объектов данного класса как следует из вышенаписанного нужно вызывать ffprobe.
 */
@AllArgsConstructor
@Getter
public class BriefFileInfo {
    private File file;

    /**
     * Информация о всех файлах в выбранной папке будет храниться, даже для тех которые не подходят для использования
     * в данной программе. В этом енуме содержатся причины непригодности к использованию для лучшей диагностики.
     */
    private BriefFileUnavailabilityReason unavailabilityReason;

    private VideoFormat videoContainer;

    private List<BriefSingleSubtitlesInfo> allSubtitles;
}

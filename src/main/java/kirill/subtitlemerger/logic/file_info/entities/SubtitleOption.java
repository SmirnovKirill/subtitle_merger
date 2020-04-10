package kirill.subtitlemerger.logic.file_info.entities;

import kirill.subtitlemerger.logic.core.entities.Subtitles;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

@AllArgsConstructor
@Getter
public abstract class SubtitleOption {
    private String id;

    protected Subtitles subtitles;

    protected Charset encoding;

    /**
     * We will keep track of all options for the file even if they can't be used for subtitle merging (for better
     * diagnostics). Enum contains the reason why this option can't be used for subtitle merging.
     */
    private UnavailabilityReason unavailabilityReason;

    private boolean selectedAsUpper;

    private boolean selectedAsLower;

    public void selectAsUpper() {
        selectedAsUpper = true;
        selectedAsLower = false;
    }

    public void selectAsLower() {
        selectedAsUpper = false;
        selectedAsLower = true;
    }

    public void unselect() {
        selectedAsUpper = false;
        selectedAsLower = false;
    }

    public static <T extends SubtitleOption> T getById(String id, List<T> subtitleOptions) {
        return subtitleOptions.stream()
                .filter(option -> Objects.equals(option.getId(), id))
                .findFirst().orElseThrow(IllegalAccessError::new);
    }

    public enum UnavailabilityReason {
        NOT_ALLOWED_CODEC
    }
}
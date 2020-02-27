package kirill.subtitlemerger.gui.utils;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

@AllArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class MultiPartResult {
    public static final MultiPartResult EMPTY = new MultiPartResult(null, null, null);

    private String success;

    private String warn;

    private String error;

    public static MultiPartResult onlySuccess(String text) {
        return new MultiPartResult(text, null, null);
    }

    public static MultiPartResult onlyWarn(String text) {
        return new MultiPartResult(null, text, null);
    }

    public static MultiPartResult onlyError(String text) {
        return new MultiPartResult(null, null, text);
    }

    public boolean empty() {
        return StringUtils.isBlank(success) && StringUtils.isBlank(warn) && StringUtils.isBlank(error);
    }
}

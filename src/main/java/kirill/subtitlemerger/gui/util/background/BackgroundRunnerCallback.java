package kirill.subtitlemerger.gui.util.background;

@FunctionalInterface
public interface BackgroundRunnerCallback<T> {
    void run(T backgroundRunnerResult);
}

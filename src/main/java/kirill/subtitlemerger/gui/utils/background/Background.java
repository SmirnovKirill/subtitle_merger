package kirill.subtitlemerger.gui.utils.background;

public class Background {
    /**
     * Runs the runner in background and after it's completed calls the callback in the main thread. Runners should
     * always return the result and never throw any exceptions, that also means that they should handle interruptions
     * without throwing exceptions.
     */
    public static <T> BackgroundManager run(BackgroundRunner<T> runner, BackgroundCallback<T> callback) {
        HelperTask helperTask = new HelperTask<>(runner, callback);

        Thread thread = new Thread(helperTask);
        thread.setDaemon(true);
        thread.start();

        return helperTask.getManager();
    }
}

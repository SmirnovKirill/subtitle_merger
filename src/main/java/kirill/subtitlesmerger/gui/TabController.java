package kirill.subtitlesmerger.gui;

interface TabController {
    void initialize();

    TabView getTabView();

    /**
     * Method is called when the tab becomes active and takes focus from the previous tab. Method IS NOT called
     * if the tab is the first to show and is displayed for the first time.
     */
    void tabClicked();
}

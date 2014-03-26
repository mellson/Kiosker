package dk.itu.kiosker.utils;

public class WebHelper {
    /**
     * Translate a layout int to a float weight value used to distribute web views on the screen.
     *
     * @param layout      the layout to transform. 0 equals full screen.
     * @param mainWebPage is this a translation for the main view?
     * @return a float value indicating how much screen estate a view should take. 0.1f for 10% etc.
     */
    public static float layoutTranslator(int layout, boolean mainWebPage) {
        switch (layout) {
            case 1:
                return 0.5f;
            case 2:
                if (mainWebPage)
                    return 0.6f;
                else
                    return 0.4f;
            case 3:
                if (mainWebPage)
                    return 0.7f;
                else
                    return 0.3f;
            case 4:
                if (mainWebPage)
                    return 0.8f;
                else
                    return 0.2f;
            default: // Our default is the fullscreen layout
                if (mainWebPage)
                    return 1.0f;
                else
                    return 0f;
        }
    }
}

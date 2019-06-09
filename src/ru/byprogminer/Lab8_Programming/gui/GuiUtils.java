package ru.byprogminer.Lab8_Programming.gui;

import ru.byprogminer.Lab7_Programming.logging.Loggers;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GuiUtils {

    public interface BorderFactoryMethod {

        Border createBorder(int top, int right, int bottom, int left);
    }

    private static final String DEFAULT_FONT_PATH = "/resources/Helvetica.otf";
    private static final float DEFAULT_FONT_SIZE = 17;

    private static final Logger log = Loggers.getClassLogger(GuiUtils.class);

    public static final String BASE_APP_NAME = "Lab8_Programming";

    public static final Font DEFAULT_FONT;
    public static final Insets DEFAULT_MARGIN = new Insets(2, 3, 2, 3);
    public static final Border DEFAULT_MARGIN_BORDER = createDefaultMarginBorder(BorderFactory::createEmptyBorder);

    static {
        try {
            DEFAULT_FONT = Font
                    .createFont(Font.TRUETYPE_FONT, GuiUtils.class.getResourceAsStream(DEFAULT_FONT_PATH))
                    .deriveFont(DEFAULT_FONT_SIZE);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "cannot load default GUI font", e);
            throw new RuntimeException("cannot load default GUI font");
        }
    }

    public static Border createDefaultMarginBorder(BorderFactoryMethod factoryMethod) {
        return factoryMethod.createBorder(
                DEFAULT_MARGIN.top,
                DEFAULT_MARGIN.right,
                DEFAULT_MARGIN.bottom,
                DEFAULT_MARGIN.left
        );
    }

    public static String getWindowTitle(String loggedAs) {
        if (loggedAs == null) {
            return BASE_APP_NAME;
        }

        return BASE_APP_NAME + " (" + loggedAs + ")";
    }
}

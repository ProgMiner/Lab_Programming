package ru.byprogminer.Lab8_Programming.gui;

import ru.byprogminer.Lab7_Programming.Credentials;
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
    private static final float DEFAULT_FONT_SIZE = 15;

    private static final Logger log = Loggers.getClassLogger(GuiUtils.class);

    public static final Font DEFAULT_FONT;
    public static final Font DEFAULT_BUTTON_FONT;
    public static final Insets DEFAULT_MARGIN = new Insets(4, 3, 2, 3);
    public static final Insets DEFAULT_BUTTON_MARGIN = new Insets(5, 30, 4, 30);
    public static final Border DEFAULT_MARGIN_BORDER =
            createMarginBorder(DEFAULT_MARGIN, BorderFactory::createEmptyBorder);

    static {
        try {
            DEFAULT_FONT = Font
                    .createFont(Font.TRUETYPE_FONT, GuiUtils.class.getResourceAsStream(DEFAULT_FONT_PATH))
                    .deriveFont(DEFAULT_FONT_SIZE);

            DEFAULT_BUTTON_FONT = DEFAULT_FONT.deriveFont(Font.BOLD);
        } catch (Throwable e) {
            log.log(Level.SEVERE, "cannot load default GUI font", e);
            throw new RuntimeException("cannot load default GUI font");
        }
    }

    public static Border createMarginBorder(Insets margin, BorderFactoryMethod factoryMethod) {
        return factoryMethod.createBorder(margin.top, margin.right, margin.bottom, margin.left);
    }

    public static String getWindowTitle(String name, Credentials currentUser) {
        if (currentUser == null) {
            return name;
        }

        return name + " (" + currentUser.username + ")";
    }
}

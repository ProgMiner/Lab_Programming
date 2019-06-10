package ru.byprogminer.Lab7_Programming.frontends;

import ru.byprogminer.Lab7_Programming.Frontend;
import ru.byprogminer.Lab7_Programming.renderers.GuiRenderer;

import javax.swing.*;

public class GuiFrontend implements Frontend {

    private final GuiRenderer renderer;

    public GuiFrontend(GuiRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void exec() throws IllegalStateException {
        SwingUtilities.invokeLater(() -> renderer.mainWindow.setVisible(true));

        while (!renderer.mainWindow.isVisible()) {
            Thread.yield();
        }

        while (renderer.mainWindow.isVisible()) {
            Thread.yield();
        }
    }

    @Override
    public void stop() {
        //
    }
}

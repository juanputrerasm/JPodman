package com.mtm2.jpodman;

import com.mtm2.jpodman.ui.LoadingDialog;
import com.mtm2.jpodman.ui.MainWindow;

import javax.swing.UIDefaults;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

/** Application entry point for JPodman. */
public final class JPodmanApp {
    private static final int SYSTEM_FONT_SIZE = 12;

    private JPodmanApp() {}

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The default Swing look-and-feel is acceptable if the native one fails.
        }
        installSystemFontSize();

        SwingUtilities.invokeLater(() -> {
            LoadingDialog loadingDialog = new LoadingDialog(null);
            loadingDialog.setVisible(true);
            MainWindow window = new MainWindow();
            window.initializeAfterStartup(() -> {
                loadingDialog.dispose();
                window.setVisible(true);
            });
        });
    }

    private static void installSystemFontSize() {
        UIDefaults defaults = UIManager.getDefaults();
        List<Object> fontKeys = new ArrayList<>();
        for (Object key : defaults.keySet()) {
            if (defaults.get(key) instanceof Font) {
                fontKeys.add(key);
            }
        }
        for (Object key : fontKeys) {
            Font font = (Font) defaults.get(key);
            UIManager.put(key, new FontUIResource(font.deriveFont((float) SYSTEM_FONT_SIZE)));
        }
    }
}

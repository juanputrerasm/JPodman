package com.mtm2.jpodman;

import com.mtm2.jpodman.ui.LoadingDialog;
import com.mtm2.jpodman.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Application entry point for JPodman. */
public final class JPodmanApp {
    private JPodmanApp() {}

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // The default Swing look-and-feel is acceptable if the native one fails.
        }

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
}

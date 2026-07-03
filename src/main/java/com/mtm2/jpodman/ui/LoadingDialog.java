package com.mtm2.jpodman.ui;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

/** Small startup dialog shown while the initial POD lists are loaded. */
public final class LoadingDialog extends JDialog {
    private final JLabel messageLabel = new JLabel("Loading POD lists...", SwingConstants.CENTER);

    public LoadingDialog(Frame owner) {
        super(owner, "Loading JPodman", false);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        content.add(messageLabel, BorderLayout.CENTER);
        content.add(progress, BorderLayout.SOUTH);

        add(content);
        pack();
        setSize(new Dimension(360, Math.max(getHeight(), 120)));
        setMinimumSize(getSize());
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public void setMessage(String message) {
        messageLabel.setText(message);
    }
}

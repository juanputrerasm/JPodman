package com.mtm2.jpodman.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

/** About dialog for JPodman. */
public final class AboutDialog extends JDialog {
    public AboutDialog(Frame owner) {
        super(owner, "About JPodman", true);

        JLabel title = new JLabel("JPodman v1.0.3c", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JLabel subtitle = new JLabel(
                "<html><div style='text-align:center'>"
                        + "POD mounting utility for TRI games<br>"
                        + "By Kmaster<br><br>"
                        + "<tt>www.mtm2.com</tt>"
                        + "</div></html>",
                SwingConstants.CENTER);

        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBorder(BorderFactory.createEmptyBorder(16, 24, 12, 24));
        content.add(title, BorderLayout.NORTH);
        content.add(subtitle, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(close);
        content.add(buttons, BorderLayout.SOUTH);

        add(content);
        pack();
        setSize(new Dimension(380, Math.max(getHeight(), 200)));
        setMinimumSize(getSize());
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}

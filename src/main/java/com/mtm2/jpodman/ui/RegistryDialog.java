package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.GameInstall;
import com.mtm2.jpodman.io.windows.MtmRegistryProfile;
import com.mtm2.jpodman.io.windows.MtmRegistryService;
import com.mtm2.jpodman.io.windows.RegistrySnapshot;
import com.sun.jna.platform.win32.Win32Exception;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.nio.file.Path;

/** Windows-only MTM registry info and reset dialog. */
public final class RegistryDialog extends JDialog {
    private final MtmRegistryService registryService = new MtmRegistryService();
    private final GameInstall gameInstall;
    private final JTextArea output = new JTextArea(18, 72);

    public RegistryDialog(Frame owner, GameInstall gameInstall) {
        super(owner, "Registry Info", true);
        this.gameInstall = gameInstall;

        output.setEditable(false);
        output.setLineWrap(false);
        output.setText(snapshotText(MtmRegistryProfile.MTM2));

        JPanel resetButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resetButtons.add(resetButton(MtmRegistryProfile.MTM1));
        resetButtons.add(resetButton(MtmRegistryProfile.MTM2));
        resetButtons.add(resetButton(MtmRegistryProfile.TRIAL));

        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> output.setText(snapshotText(MtmRegistryProfile.MTM2)));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(refresh);
        buttons.add(close);

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setBorder(BorderFactory.createEmptyBorder(8, 8, 4, 8));
        top.add(new JLabel("32-bit Windows registry view for the selected MTM folder:"), BorderLayout.NORTH);
        top.add(resetButtons, BorderLayout.CENTER);

        setLayout(new BorderLayout(4, 4));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(output), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setSize(new Dimension(760, Math.max(getHeight(), 460)));
        setMinimumSize(getSize());
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private JButton resetButton(MtmRegistryProfile profile) {
        JButton button = new JButton("Reset as " + profile.displayName());
        button.addActionListener(e -> reset(profile));
        return button;
    }

    private void reset(MtmRegistryProfile profile) {
        int result = JOptionPane.showConfirmDialog(
                this,
                "This will modify HKLM registry keys for " + profile.displayName()
                        + ".\nAdministrator access may be required.",
                "JPodman Registry Reset",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Path executable = gameInstall.executable().orElse(gameInstall.rootFolder().resolve("monster.exe"));
            registryService.applyProfile(profile, gameInstall.rootFolder(), executable);
            output.setText(snapshotText(profile));
            JOptionPane.showMessageDialog(this, "Registry reset completed.", "JPodman", JOptionPane.INFORMATION_MESSAGE);
        } catch (Win32Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Registry reset failed. Run JPodman as Administrator and try again.\n" + ex.getMessage(),
                    "JPodman",
                    JOptionPane.WARNING_MESSAGE);
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Registry reset failed:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private String snapshotText(MtmRegistryProfile profile) {
        RegistrySnapshot snapshot = registryService.readSnapshot(profile);
        StringBuilder sb = new StringBuilder();
        for (RegistrySnapshot.KeyValues group : snapshot.groups()) {
            sb.append(group.title()).append(System.lineSeparator());
            sb.append(group.keyPath()).append(System.lineSeparator());
            for (RegistrySnapshot.Value value : group.values()) {
                sb.append("  ").append(value.name()).append(" = ").append(value.data()).append(System.lineSeparator());
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }
}

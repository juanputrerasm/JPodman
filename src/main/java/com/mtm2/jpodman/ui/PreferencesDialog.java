package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.AppPreferences;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Preferences dialog for POD limit, extra folders, and display options. */
public class PreferencesDialog extends JDialog {
    private final JSpinner podLimitSpinner = new JSpinner(new SpinnerNumberModel(
            AppPreferences.DEFAULT_POD_LIMIT,
            AppPreferences.MIN_POD_LIMIT,
            AppPreferences.MAX_POD_LIMIT,
            1));
    private final JSpinner folderDepthSpinner = new JSpinner(new SpinnerNumberModel(-1, -1, 99, 1));
    private final JCheckBox sortCheck = new JCheckBox("Sort mounted PODs");
    private final JCheckBox keepOnTopCheck = new JCheckBox("Keep window on top");
    private final DefaultListModel<Path> foldersModel = new DefaultListModel<>();
    private final JTextArea systemPodsArea = new JTextArea(6, 30);
    private boolean confirmed;
    private AppPreferences preferences;

    public PreferencesDialog(Frame owner, AppPreferences preferences) {
        super(owner, "Preferences", true);
        this.preferences = preferences;
        podLimitSpinner.setValue(preferences.podLimit());
        folderDepthSpinner.setValue(preferences.folderDepth());
        sortCheck.setSelected(preferences.sortMountedPods());
        keepOnTopCheck.setSelected(preferences.keepWindowOnTop());
        preferences.extraPodFolders().forEach(foldersModel::addElement);
        systemPodsArea.setText(String.join(System.lineSeparator(), preferences.systemPodFiles()));

        JList<Path> foldersList = new JList<>(foldersModel);
        JScrollPane foldersScroll = new JScrollPane(foldersList);
        foldersScroll.setPreferredSize(new Dimension(420, 120));
        JScrollPane systemPodsScroll = new JScrollPane(systemPodsArea);
        systemPodsScroll.setPreferredSize(new Dimension(420, 120));

        JButton addFolder = new JButton("Add Folder...");
        addFolder.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose POD Folder");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                Path folder = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();
                if (!containsFolder(folder)) {
                    foldersModel.addElement(folder);
                }
            }
        });

        JButton removeFolder = new JButton("Remove");
        removeFolder.addActionListener(e -> {
            int selected = foldersList.getSelectedIndex();
            if (selected >= 0) {
                foldersModel.remove(selected);
            }
        });

        JPanel podLimitPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        podLimitPanel.add(podLimitSpinner);
        podLimitPanel.add(limitPresetButton("MTM1", 15));
        podLimitPanel.add(limitPresetButton("MTM2 trial/retail", 30));
        podLimitPanel.add(limitPresetButton("MTM2 patched", 99));
        podLimitPanel.add(limitPresetButton("Community patch", 199));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("POD limit:"), c);
        c.gridx = 1;
        form.add(podLimitPanel, c);

        c.gridx = 0;
        c.gridy = 1;
        form.add(new JLabel("Folder depth (-1 = all):"), c);
        c.gridx = 1;
        form.add(folderDepthSpinner, c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        form.add(sortCheck, c);
        c.gridy = 3;
        form.add(keepOnTopCheck, c);

        c.gridy = 4;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel("Extra POD folders:"), c);
        c.gridy = 5;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        form.add(foldersScroll, c);

        JPanel folderButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        folderButtons.add(addFolder);
        folderButtons.add(removeFolder);
        c.gridy = 6;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weighty = 0;
        form.add(folderButtons, c);

        c.gridy = 7;
        c.fill = GridBagConstraints.HORIZONTAL;
        form.add(new JLabel("System POD files:"), c);
        c.gridy = 8;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.5;
        form.add(systemPodsScroll, c);

        JButton ok = new JButton("Save");
        ok.addActionListener(e -> onSave());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(ok);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        pack();
        setSize(new Dimension(760, Math.max(getHeight(), 520)));
        setMinimumSize(getSize());
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public boolean wasConfirmed() {
        return confirmed;
    }

    public AppPreferences preferences() {
        return preferences;
    }

    private JButton limitPresetButton(String label, int value) {
        JButton button = new JButton(label);
        button.addActionListener(e -> podLimitSpinner.setValue(value));
        return button;
    }

    private void onSave() {
        int limit = (Integer) podLimitSpinner.getValue();
        if (limit < AppPreferences.MIN_POD_LIMIT || limit > AppPreferences.MAX_POD_LIMIT) {
            JOptionPane.showMessageDialog(this, "POD limit must be between 1 and 999.", "JPodman", JOptionPane.WARNING_MESSAGE);
            return;
        }
        preferences = new AppPreferences(
                limit,
                folders(),
                (Integer) folderDepthSpinner.getValue(),
                sortCheck.isSelected(),
                keepOnTopCheck.isSelected(),
                preferences.viewMode(),
                systemPodFiles(),
                preferences.savedPodLists());
        confirmed = true;
        dispose();
    }

    private List<Path> folders() {
        List<Path> folders = new ArrayList<>();
        for (int i = 0; i < foldersModel.size(); i++) {
            folders.add(foldersModel.get(i));
        }
        return folders;
    }

    private List<String> systemPodFiles() {
        List<String> files = new ArrayList<>();
        for (String line : systemPodsArea.getText().split("\\R")) {
            if (!line.isBlank()) {
                files.add(line.trim());
            }
        }
        return files;
    }

    private boolean containsFolder(Path folder) {
        for (int i = 0; i < foldersModel.size(); i++) {
            if (foldersModel.get(i).equals(folder)) {
                return true;
            }
        }
        return false;
    }
}

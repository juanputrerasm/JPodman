package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.GameInstall;
import com.mtm2.jpodman.MonsterIniSettings;
import com.mtm2.jpodman.MonsterIniSettings.FontSetting;
import com.mtm2.jpodman.io.MonsterIniService;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** English Monster.ini editor for Cowpod's Fonts & Things settings. */
public final class FontsAndSettingsDialog extends JDialog {
    private final GameInstall gameInstall;
    private final Runnable afterSave;
    private final JCheckBox useCustomFonts = new JCheckBox("Use custom fonts");
    private final List<JCheckBox> fontEnabled = new ArrayList<>();
    private final List<JTextField> fontNames = new ArrayList<>();
    private final List<JTextField> fontSizes = new ArrayList<>();
    private final JComboBox<String> localizationCombo = new JComboBox<>();
    private final JCheckBox extraHorn = new JCheckBox("Extra horn");
    private final JCheckBox numericLatency = new JCheckBox("Numeric latency display");
    private final JCheckBox hiddenTrack = new JCheckBox("Secret track");
    private MonsterIniSettings settings;

    public FontsAndSettingsDialog(Frame owner, GameInstall gameInstall, Runnable afterSave) {
        super(owner, "Fonts & Settings", true);
        this.gameInstall = gameInstall;
        this.afterSave = afterSave;

        try {
            settings = MonsterIniService.read(gameInstall.monsterIniPath());
        } catch (IOException ex) {
            settings = MonsterIniSettings.defaults();
        }

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        useCustomFonts.setSelected(settings.useGlobalFonts());
        useCustomFonts.addActionListener(e -> updateFontControlState());
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 4;
        form.add(useCustomFonts, c);

        c.gridy++;
        c.gridwidth = 1;
        form.add(new JLabel("Category"), c);
        c.gridx = 1;
        form.add(new JLabel("Use"), c);
        c.gridx = 2;
        form.add(new JLabel("Font name"), c);
        c.gridx = 3;
        form.add(new JLabel("Font size"), c);

        for (FontSetting font : settings.fonts()) {
            c.gridy++;
            c.gridx = 0;
            form.add(new JLabel(font.label()), c);
            JCheckBox enabled = new JCheckBox();
            enabled.setSelected(font.enabled());
            enabled.addActionListener(e -> updateFontControlState());
            fontEnabled.add(enabled);
            c.gridx = 1;
            form.add(enabled, c);
            JTextField nameField = new JTextField(font.fontName(), 24);
            fontNames.add(nameField);
            c.gridx = 2;
            c.weightx = 1;
            form.add(nameField, c);
            JTextField sizeField = new JTextField(font.fontSize(), 12);
            fontSizes.add(sizeField);
            c.gridx = 3;
            c.weightx = 0;
            form.add(sizeField, c);
        }

        c.gridy++;
        c.gridx = 0;
        c.weightx = 0;
        form.add(new JLabel("Localization file"), c);
        localizationCombo.setEditable(true);
        for (String loc : MonsterIniSettings.STOCK_LOCALIZATION_FILES) {
            localizationCombo.addItem(loc);
        }
        localizationCombo.setSelectedItem(settings.localizationFile());
        c.gridx = 1;
        c.gridwidth = 3;
        c.weightx = 1;
        form.add(localizationCombo, c);
        c.gridwidth = 1;
        c.weightx = 0;

        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 4;
        extraHorn.setSelected(settings.extraHorn());
        numericLatency.setSelected(settings.numericLatency());
        hiddenTrack.setSelected(settings.hiddenTrack());
        JPanel tweakPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tweakPanel.add(extraHorn);
        tweakPanel.add(numericLatency);
        tweakPanel.add(hiddenTrack);
        form.add(tweakPanel, c);

        c.gridy++;
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JButton stockPreset = new JButton("Stock preset");
        stockPreset.addActionListener(e -> applyStockPreset());
        presetPanel.add(stockPreset);
        form.add(presetPanel, c);

        JButton save = new JButton("Save");
        save.addActionListener(e -> onSave());
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dispose());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(save);
        buttons.add(cancel);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        updateFontControlState();
        pack();
        setSize(new Dimension(760, Math.max(getHeight(), 420)));
        setMinimumSize(getSize());
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void applyStockPreset() {
        List<FontSetting> stock = MonsterIniSettings.defaultFonts();
        for (int i = 0; i < stock.size(); i++) {
            fontEnabled.get(i).setSelected(stock.get(i).enabled());
            fontNames.get(i).setText(stock.get(i).fontName());
            fontSizes.get(i).setText(stock.get(i).fontSize());
        }
        useCustomFonts.setSelected(true);
        extraHorn.setSelected(true);
        numericLatency.setSelected(false);
        hiddenTrack.setSelected(false);
        localizationCombo.setSelectedItem(MonsterIniSettings.STOCK_LOCALIZATION_FILES.get(0));
        updateFontControlState();
    }

    private void updateFontControlState() {
        boolean global = useCustomFonts.isSelected();
        for (int i = 0; i < fontEnabled.size(); i++) {
            fontEnabled.get(i).setEnabled(global);
            fontNames.get(i).setEnabled(global && fontEnabled.get(i).isSelected());
            fontSizes.get(i).setEnabled(global && fontEnabled.get(i).isSelected());
        }
    }

    private void onSave() {
        try {
            MonsterIniService.write(gameInstall.monsterIniPath(), collectSettings());
            if (afterSave != null) {
                afterSave.run();
            }
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Monster.ini could not be saved:\n" + ex.getMessage(), "JPodman", JOptionPane.WARNING_MESSAGE);
        }
    }

    private MonsterIniSettings collectSettings() {
        List<FontSetting> fonts = new ArrayList<>();
        for (int i = 0; i < settings.fonts().size(); i++) {
            FontSetting original = settings.fonts().get(i);
            fonts.add(new FontSetting(
                    original.suffix(),
                    original.label(),
                    fontEnabled.get(i).isSelected(),
                    fontNames.get(i).getText(),
                    fontSizes.get(i).getText()));
        }
        Object loc = localizationCombo.getEditor().getItem();
        return new MonsterIniSettings(
                useCustomFonts.isSelected(),
                fonts,
                loc == null ? "" : loc.toString(),
                extraHorn.isSelected(),
                numericLatency.isSelected(),
                hiddenTrack.isSelected());
    }
}

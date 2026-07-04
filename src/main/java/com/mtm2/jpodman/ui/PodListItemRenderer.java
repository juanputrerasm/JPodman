package com.mtm2.jpodman.ui;

import com.mtm2.jpodman.PodListItem;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Locale;

/** Shared POD list renderer for rich metadata tags. */
final class PodListItemRenderer extends DefaultListCellRenderer {
    private static final Color SYSTEM_COLOR = new Color(80, 150, 95);
    private static final Color TRACK_COLOR = new Color(80, 170, 255);
    private static final Color TRUCK_COLOR = new Color(255, 120, 120);
    private static final Color MISSING_COLOR = new Color(210, 70, 70);
    private static final Color FALLBACK_METADATA_COLOR = new Color(150, 150, 150);

    @Override
    public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        JLabel base = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (!(value instanceof PodListItem item)) {
            return base;
        }

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(true);
        row.setBackground(base.getBackground());
        row.setBorder(base.getBorder());
        row.setToolTipText(item.mountPath());

        Font listFont = list.getFont();
        int metadataStart = item.displayLabel().indexOf(" [");
        if (metadataStart < 0 || !item.displayLabel().endsWith("]")) {
            row.add(label(item.displayLabel(), listFont.deriveFont(Font.BOLD), base.getForeground()));
            return row;
        }

        String podName = item.displayLabel().substring(0, metadataStart);
        String metadata = item.displayLabel().substring(metadataStart + 2, item.displayLabel().length() - 1);
        row.add(label(podName, listFont.deriveFont(Font.BOLD), base.getForeground()));
        row.add(label(" [", listFont, base.getForeground()));
        addMetadataLabels(row, metadata, listFont, isSelected ? base.getForeground() : null);
        row.add(label("]", listFont, base.getForeground()));
        return row;
    }

    private static void addMetadataLabels(JPanel row, String metadata, Font font, Color selectedForeground) {
        String[] groups = metadata.split("; ");
        for (int i = 0; i < groups.length; i++) {
            if (i > 0) {
                row.add(label("; ", font, selectedForeground != null ? selectedForeground : FALLBACK_METADATA_COLOR));
            }
            String group = groups[i];
            row.add(label(group, font, selectedForeground != null ? selectedForeground : metadataColor(group)));
        }
    }

    private static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    private static Color metadataColor(String group) {
        String lower = group.toLowerCase(Locale.ROOT);
        if (lower.equals("system")) {
            return SYSTEM_COLOR;
        }
        if (lower.equals("missing")) {
            return MISSING_COLOR;
        }
        if (lower.startsWith("track")) {
            return TRACK_COLOR;
        }
        if (lower.startsWith("truck")) {
            return TRUCK_COLOR;
        }
        return FALLBACK_METADATA_COLOR;
    }
}

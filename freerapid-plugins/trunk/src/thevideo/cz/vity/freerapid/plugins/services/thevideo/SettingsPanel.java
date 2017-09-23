package cz.vity.freerapid.plugins.services.thevideo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author birchie
 */
public class SettingsPanel extends JPanel {
    private SettingsConfig settings;
    private TheVideoServiceImpl service;

    public SettingsPanel(TheVideoServiceImpl service) throws Exception {
        this.service = service;
        settings = new SettingsConfig();
        settings.setQualitySetting(service.getSettings().getQualitySetting());
        initPanel();
    }

    public SettingsConfig getSettings() {
        return settings;
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred quality:");
        final JComboBox qualityList = new JComboBox(VideoQuality.values());
        final JButton accountButton = new JButton("Premium Account");
        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setSelectedItem(settings.getQualitySetting());
        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settings.setQualitySetting((VideoQuality) qualityList.getSelectedItem());
            }
        });
        accountButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    service.showConfigDialog();
                } catch (Exception x) {
                    x.printStackTrace();
                }
            }
        });
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(new JLabel(" "));
        add(accountButton);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}
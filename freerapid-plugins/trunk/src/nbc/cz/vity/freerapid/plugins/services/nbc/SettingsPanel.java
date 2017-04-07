package cz.vity.freerapid.plugins.services.nbc;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
class SettingsPanel extends JPanel {
    private SettingsConfig config;

    public SettingsPanel(NbcServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox<VideoQuality> qualityList = new JComboBox<VideoQuality>(VideoQuality.getItems());
        final JCheckBox subtitlesCheckBox = new JCheckBox("Download subtitles");
        final JCheckBox onlySubsCheckBox = new JCheckBox("     Only subtitles");

        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitlesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        onlySubsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        qualityList.setSelectedItem(config.getVideoQuality());
        subtitlesCheckBox.setSelected(config.isDownloadSubtitles());
        if (!config.isDownloadSubtitles())
            config.setOnlySubtitles(config.isDownloadSubtitles());
        onlySubsCheckBox.setSelected(config.isOnlySubtitles());
        onlySubsCheckBox.setEnabled(config.isDownloadSubtitles());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) qualityList.getSelectedItem());
            }
        });
        subtitlesCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setDownloadSubtitles(subtitlesCheckBox.isSelected());
                if (!config.isDownloadSubtitles()) {
                    config.setOnlySubtitles(config.isDownloadSubtitles());
                    onlySubsCheckBox.setSelected(config.isDownloadSubtitles());
                }
                onlySubsCheckBox.setEnabled(config.isDownloadSubtitles());
            }
        });
        onlySubsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setOnlySubtitles(onlySubsCheckBox.isSelected());
            }
        });

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(new JLabel(" "));
        add(subtitlesCheckBox);
        add(onlySubsCheckBox);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}

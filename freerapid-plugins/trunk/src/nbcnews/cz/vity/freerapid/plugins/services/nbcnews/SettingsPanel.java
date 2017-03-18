package cz.vity.freerapid.plugins.services.nbcnews;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
class SettingsPanel extends JPanel {
    private SettingsConfig config;

    public SettingsPanel(NbcNewsServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JCheckBox videoHiCheckBox = new JCheckBox("High quality video");
        final JCheckBox videoLoCheckBox = new JCheckBox("Low quality Video");
        final JCheckBox subtitlesCheckBox = new JCheckBox("Download subtitles");
        final JCheckBox onlySubsCheckBox = new JCheckBox("     Only subtitles");

        videoHiCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        videoLoCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitlesCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        onlySubsCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        videoHiCheckBox.setSelected(config.isHighVideoQuality());
        videoLoCheckBox.setSelected(!config.isHighVideoQuality());
        subtitlesCheckBox.setSelected(config.isDownloadSubtitles());
        if (!config.isDownloadSubtitles())
            config.setOnlySubtitles(config.isDownloadSubtitles());
        onlySubsCheckBox.setSelected(config.isOnlySubtitles());
        onlySubsCheckBox.setEnabled(config.isDownloadSubtitles());

        videoHiCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setHighVideoQuality(videoHiCheckBox.isSelected());
                videoLoCheckBox.setSelected(!config.isHighVideoQuality());
            }
        });
        videoLoCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setHighVideoQuality(!videoLoCheckBox.isSelected());
                videoHiCheckBox.setSelected(config.isHighVideoQuality());
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
        add(videoHiCheckBox);
        add(videoLoCheckBox);
        add(new JLabel(" "));
        add(subtitlesCheckBox);
        add(onlySubsCheckBox);
        add(Box.createRigidArea(new Dimension(0, 10)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}

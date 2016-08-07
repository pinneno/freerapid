package cz.vity.freerapid.plugins.services.appletrailers;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author tong2shot
 */
class SettingsPanel extends JPanel {
    private SettingsConfig config;

    public SettingsPanel(AppleTrailersServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred quality level:");
        final JComboBox<VideoQuality> qualityList = new JComboBox<VideoQuality>(VideoQuality.getItems());
        final JLabel formatLabel = new JLabel("Preferred video format:");
        final JComboBox<VideoFormat> formatList = new JComboBox<VideoFormat>(VideoFormat.values());

        qualityLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setAlignmentX(Component.LEFT_ALIGNMENT);
        formatLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        formatList.setAlignmentX(Component.LEFT_ALIGNMENT);
        qualityList.setSelectedItem(config.getVideoQuality());
        formatList.setSelectedItem(config.getVideoFormat());

        qualityList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) qualityList.getSelectedItem());
            }
        });

        formatList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoFormat((VideoFormat) formatList.getSelectedItem());
            }
        });
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(qualityList);
        add(Box.createRigidArea(new Dimension(0, 5)));
        add(formatLabel);
        add(formatList);
        add(Box.createRigidArea(new Dimension(0, 15)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}

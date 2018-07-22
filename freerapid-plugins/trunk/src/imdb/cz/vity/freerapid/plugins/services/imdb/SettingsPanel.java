package cz.vity.freerapid.plugins.services.imdb;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author birchie
 */
class SettingsPanel extends JPanel {
    private SettingsConfig config;

    public SettingsPanel(ImdbServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel qualityLabel = new JLabel("Preferred video quality:");
        qualityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        rbHigh = new JRadioButton("Highest available", (config.isHighestQuality() && ! config.isCustomQuality()));
        rbLow = new JRadioButton("Lowest available", (!config.isHighestQuality() && ! config.isCustomQuality()));
        rbCustom = new JRadioButton("Custom", config.isCustomQuality());
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(rbHigh);
        btnGroup.add(rbLow);
        btnGroup.add(rbCustom);

        final JPanel pnlOptions = new JPanel();
        pnlOptions.setLayout(new BoxLayout(pnlOptions, BoxLayout.Y_AXIS));
        pnlOptions.setAlignmentX(Component.CENTER_ALIGNMENT);
        pnlOptions.add(rbHigh);
        pnlOptions.add(rbLow);
        pnlOptions.add(rbCustom);

        rbHigh.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setHighestQuality();
                updateList();
            }
        });
        rbLow.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setLowestQuality();
                updateList();
            }
        });
        rbCustom.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setCustomQuality();
                updateList();
            }
        });

        qualityList = new JList(config.getVideoQuality());
        qualityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        qualityList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                config.setCustomQuality();
                rbHigh.setSelected(false);
                rbLow.setSelected(false);
                rbCustom.setSelected(true);
                updateList();
            }
        });

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(qualityList);
        scrollPane.setMaximumSize(new Dimension(80, 100));
        scrollPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        scrollPane.setPreferredSize(scrollPane.getMaximumSize());

        final JPanel pnlButtons = new JPanel();
        pnlButtons.setLayout(new GridLayout(1, 4));
        JButton btnUp = new JButton("/\\");
        JButton btnDown = new JButton("\\/");
        btnUp.setFocusable(false);
        btnDown.setFocusable(false);
        pnlButtons.add(new JLabel(" "));
        pnlButtons.add(btnUp);
        pnlButtons.add(btnDown);
        pnlButtons.add(new JLabel(" "));
        btnUp.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = qualityList.getSelectedIndex();
                if (index > 0) {
                    VideoQuality qualities[] = config.getVideoQuality();
                    VideoQuality tmp = qualities[index];
                    qualities[index] = qualities[index-1];
                    qualities[index-1] = tmp;
                    config.setVideoQuality(qualities);
                    updateList();
                    qualityList.setSelectedIndex(index - 1);
                }
            }
        });
        btnDown.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = qualityList.getSelectedIndex();
                if (index < (qualityList.getModel().getSize()-1)) {
                    VideoQuality qualities[] = config.getVideoQuality();
                    VideoQuality tmp = qualities[index];
                    qualities[index] = qualities[index+1];
                    qualities[index+1] = tmp;
                    config.setVideoQuality(qualities);
                    updateList();
                    qualityList.setSelectedIndex(index + 1);
                }
            }
        });

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(qualityLabel);
        add(pnlOptions);
        add(scrollPane);
        add(pnlButtons);
        add(Box.createRigidArea(new Dimension(0, 10)));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

    private void updateList() {
        qualityList.setEnabled(false);
        qualityList.setEnabled(true);
    }

    JRadioButton rbHigh;
    JRadioButton rbLow;
    JRadioButton rbCustom;
    JList qualityList;
}

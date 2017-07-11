package cz.vity.freerapid.plugins.services.iprima;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * @author JPEXS
 * @author ntoskrnl
 * @author tong2shot
 */
class iPrimaSettingsPanel extends JPanel {
    private iPrimaSettingsConfig config;

    public iPrimaSettingsPanel(iPrimaServiceImpl service) throws Exception {
        super();
        config = service.getConfig();
        initPanel();
    }

    private void initPanel() {
        final JLabel lblQuality = new JLabel("Preferred quality level:");
        final JComboBox<VideoQuality> cbbQuality = new JComboBox<VideoQuality>(VideoQuality.values());

        lblQuality.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbbQuality.setAlignmentX(Component.LEFT_ALIGNMENT);
        cbbQuality.setMaximumSize(new Dimension(500, 24));

        cbbQuality.setSelectedItem(config.getVideoQuality());

        cbbQuality.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setVideoQuality((VideoQuality) cbbQuality.getSelectedItem());
            }
        });

        JPanel pnlQuality = new JPanel();
        pnlQuality.setLayout(new BoxLayout(pnlQuality, BoxLayout.Y_AXIS));
        pnlQuality.add(lblQuality);
        pnlQuality.add(cbbQuality);


        final JLabel lblUsername = new JLabel("Email address:");
        final JTextField txtfldUsername = new JTextField(40);
        final JLabel lblPassword = new JLabel("Password:");
        final JPasswordField pswdfldPassword = new JPasswordField(40);
        final JButton btnClearSlotData = new JButton("Clear session data");

        lblUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtfldUsername.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        pswdfldPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnClearSlotData.setAlignmentX(Component.LEFT_ALIGNMENT);
        txtfldUsername.setMaximumSize(new Dimension(500, 24));
        pswdfldPassword.setMaximumSize(new Dimension(500, 24));

        txtfldUsername.setText(config.getUsername());
        pswdfldPassword.setText(config.getPassword());

        txtfldUsername.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                //
            }

            @Override
            public void focusLost(FocusEvent e) {
                config.setUsername(txtfldUsername.getText().trim().isEmpty() ? null : txtfldUsername.getText());
            }
        });

        pswdfldPassword.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                //
            }

            @Override
            public void focusLost(FocusEvent e) {
                config.setPassword((pswdfldPassword.getPassword().length == 0) ? null : new String(pswdfldPassword.getPassword()));
            }
        });

        btnClearSlotData.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                config.setSessionCookies(null);
                config.setSessionUsername(null);
                config.setSessionPassword(null);
            }
        });
        JPanel pnlAccount = new JPanel();
        pnlAccount.setLayout(new BoxLayout(pnlAccount, BoxLayout.Y_AXIS));
        pnlAccount.add(lblUsername);
        pnlAccount.add(txtfldUsername);
        pnlAccount.add(lblPassword);
        pnlAccount.add(pswdfldPassword);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        pnlAccount.add(btnClearSlotData);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.add("Quality settings", pnlQuality);
        tabbedPane.add("Account", pnlAccount);

        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(tabbedPane);
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    }

}
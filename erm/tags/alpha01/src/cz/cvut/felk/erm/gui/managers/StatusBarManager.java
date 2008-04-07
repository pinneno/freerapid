package cz.cvut.felk.erm.gui.managers;

import cz.cvut.felk.erm.gui.managers.interfaces.IAreaChangeListener;
import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.swingx.JXStatusBar;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * Sprava a vytvoreni Statusbaru
 * @author Ladislav Vitasek
 */
public class StatusBarManager implements PropertyChangeListener, IAreaChangeListener {
    private JXStatusBar statusbar;
    private JLabel infoLabel;
    private final ManagerDirector director;


    public StatusBarManager(ManagerDirector director, ApplicationContext context) {
        this.director = director;
        final Action action = context.getActionMap().get("showStatusBar");
        action.putValue(Action.SELECTED_KEY, true); //defaultni hodnota
        action.addPropertyChangeListener(new PropertyChangeListener() {
            //odchyt udalosti z akce pro zmenu viditelnosti statusbaru
            public void propertyChange(PropertyChangeEvent evt) {
                if (Action.SELECTED_KEY.equals(evt.getPropertyName())) {
                    setStatusBarVisible((Boolean) evt.getNewValue());
                }
            }
        });
    }

    public JXStatusBar getStatusBar() {
        if (statusbar == null) {
            statusbar = new JXStatusBar();

            statusbar.setName("statusbarPanel");
            infoLabel = new JLabel();
            infoLabel.setPreferredSize(new Dimension(330, 15));
            director.getMenuManager().getMenuBar().addPropertyChangeListener("selectedText", this);
            statusbar.add(infoLabel, JXStatusBar.Constraint.ResizeBehavior.FIXED);
//            statusbar.add(progressBar, JXStatusBar.Constraint.ResizeBehavior.FIXED);
            statusbar.add(Box.createGlue(), JXStatusBar.Constraint.ResizeBehavior.FILL);
//            final ClockField comp = new ClockField();
            //    progressBar.setVisible(false);
            //comp.setHorizontalAlignment(JLabel.CENTER);
  //          statusbar.add(comp);
            Application.getInstance().getContext().getTaskMonitor().addPropertyChangeListener(this);
        }
        return statusbar;
    }

    private void setStatusBarVisible(boolean visible) {
        getStatusBar().setVisible(visible);
        //AppPrefs.storeProperty(AppPrefs.SHOW_STATUSBAR, visible); //ulozeni uzivatelskeho nastaveni
    }

    public void propertyChange(PropertyChangeEvent evt) {
        final String propertyName = evt.getPropertyName();
        if ("done".equals(propertyName)) {
            infoLabel.setText("");
        } else if ("message".equals(propertyName) || "selectedText".equals(propertyName)) {
            infoLabel.setText((String) evt.getNewValue());
        }
    }

    public void areaActivated(AreaChangeEvent event) {

    }

    public void areaDeactivated(AreaChangeEvent event) {

    }
}

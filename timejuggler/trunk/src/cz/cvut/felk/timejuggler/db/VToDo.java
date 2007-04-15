package cz.cvut.felk.timejuggler.db;

import java.sql.Timestamp;

/**
 * @version 0.1
 * @created 14-IV-2007 21:47:56
 */
public class VToDo extends CalComponent {

    private Timestamp due;
    private String geo = "";
    private String location = "";
    private int priority = 0;
    private int percentcomplete = 0;
    private Timestamp completed;
    //public Alarms m_Alarms;
    //public Categories m_Categories;

    public VToDo() {

    }

    public Timestamp getDue() {
        return due;
    }

    /**
     * @param newVal
     */
    public void setDue(Timestamp newVal) {
        due = newVal;
    }

    public String getGeo() {
        return geo;
    }

    /**
     * @param newVal
     */
    public void setGeo(String newVal) {
        geo = newVal;
    }

    public String getLocation() {
        return location;
    }

    /**
     * @param newVal
     */
    public void setLocation(String newVal) {
        location = newVal;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * @param newVal
     */
    public void setPriority(int newVal) {
        priority = newVal;
    }

    public int getPercentComplete() {
        return percentcomplete;
    }

    /**
     * @param newVal
     */
    public void setPercentComplete(int newVal) {
        percentcomplete = newVal;
    }

    public Timestamp getCompleted() {
        return completed;
    }

    /**
     * @param newVal
     */
    public void setCompleted(Timestamp newVal) {
        completed = newVal;
    }

}
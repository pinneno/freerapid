package cz.felk.cvut.erm.ermodeller;

import cz.felk.cvut.erm.conceptual.beans.*;
import cz.felk.cvut.erm.ermodeller.interfaces.FontManager;
import cz.felk.cvut.erm.event.*;
import cz.felk.cvut.erm.event.exceptions.ImpossibleNegativeValueException;
import cz.felk.cvut.erm.event.exceptions.ItemNotInsideManagerException;
import cz.felk.cvut.erm.event.interfaces.Item;
import cz.felk.cvut.erm.event.interfaces.Manager;
import cz.felk.cvut.erm.eventtool.ConnectionLine;
import cz.felk.cvut.erm.eventtool.interfaces.Connection;
import cz.felk.cvut.erm.swing.ShowException;

import javax.swing.*;
import java.awt.*;

/**
 * This type represents the participation on the relation. Holds the cardinality and parciality of the participation.
 * Adds the role name. Has the pointer to the model object.
 * <p/>
 * Its created by <code>Relation</code> method <code>createCardinality</code>.
 *
 * @see RelationConstruct#createCardinality(EntityConstruct , cz.felk.cvut.erm.event.interfaces.Manager ,int,int)
 */
public class CardinalityConstruct extends ConceptualConstructObject {
    /**
     * The model of the cardinality -- object from the Ale� Kopeck� work
     */
    Cardinality model = null;

    /**
     * Constructs new cardinality. Counts the size from the role name and integrit restrictions and also set as
     * <code>propertyChangeListener</code> to its model to know about changes of the <b>name</b>, <b>arbitrary</b> and
     * <b>multiCardinality</b> changes.
     *
     * @param car     The model of this cardinality.
     * @param manager The window group (or desktop) where to put the new cardinality.
     * @param left    The x coordinate of the left top point of the new cardinality.
     * @param top     The y coordinate of the left top point of the new cardinality.
     * @throws <code>java.lang.NullPointerException</code>
     *          Thrown by inherited constructor.
     * @throws <code>cz.green.event.ImpossibleNegativeValueException</code>
     *          Thrown by inherited constructor.
     * @see ConceptualConstructObject#ConceptualConstructObject(cz.felk.cvut.erm.event.interfaces.Manager ,int,int,int,int)
     */
    public CardinalityConstruct(Cardinality car, Manager manager, int left, int top) throws NullPointerException, ImpossibleNegativeValueException {
        //inhereted constructor
        super(manager, left, top, 10, 10);
        //set as property change listener
        car.addPropertyChangeListener(this);
        model = car;
        //counts the size
        java.awt.Dimension dim = countSize();
        rect[0][1] = rect[0][0] + dim.width;
        rect[1][1] = rect[1][0] + dim.height;
    }

    /**
     * Counts the size from the role name and the integrit restrictions.
     *
     * @return The counted size needful for holding.
     */
    protected java.awt.Dimension countSize() {
        String name = model.getName();
        java.awt.FontMetrics fm;
        try {
            fm = ((FontManager) manager).getReferentFontMetrics();
            int w1 = fm.stringWidth(name), w2 = fm.stringWidth("N:N"), height = fm.getAscent();
            return new java.awt.Dimension(height + ((w1 > w2) ? w1 : w2), (int) (2.25 * height));
        } catch (ClassCastException e) {
            return new java.awt.Dimension(10, 10);
        }
    }

    /**
     * This method adds items to the context menu, which are specific to the atribute.
     *
     * @param menu  The popup menu where to add the new items.
     * @param event The event, which caused the context menu displaying. Is useful for determing targets of the methods
     *              call.
     * @return The filled menu.
     */
    protected JPopupMenu createMenu(JPopupMenu menu, PopUpMenuEvent event) {
        super.createMenu(menu, event);
        if (model.getArbitrary()) {
            addMenuItem(menu, "Optional", "img/mNotMandatory.gif", getModel(), "setArbitrary", Boolean.FALSE, boolean.class);
        } else {
            addMenuItem(menu, "Mandatory", "img/mMandatory.gif", getModel(), "setArbitrary", Boolean.TRUE, boolean.class);
        }
        if (model.getMultiCardinality()) {
            addMenuItem(menu, "Unary cardinality", "img/mUnary.gif", getModel(), "setMultiCardinality", Boolean.FALSE, boolean.class);
        } else {
            addMenuItem(menu, "N-ary cardinality", "img/mMulti.gif", getModel(), "setMultiCardinality", Boolean.TRUE, boolean.class);
        }
        return menu;
    }

    /**
     * Get the entity that participation this object represents.
     */
    public EntityConstruct getEntity() {
        for (Connection c : connections) {
            if (c.getOne() instanceof EntityConstruct)
                return (EntityConstruct) (c.getOne());
            if (c.getTwo() instanceof EntityConstruct)
                return (EntityConstruct) (c.getTwo());
        }
        return null;
    }

    /**
     * Returns the model object.
     */
    public Object getModel() {
        return model;
    }

    /**
     * Get the relation that this object belongs to.
     */
    public RelationConstruct getRelation() {
        for (Connection c : connections) {
            if (c.getOne() instanceof RelationConstruct)
                return (RelationConstruct) (c.getOne());
            if (c.getTwo() instanceof RelationConstruct)
                return (RelationConstruct) (c.getTwo());
        }
        return null;
    }

    /**
     * Get the connection line ro relation that this object belongs to.
     */
    public ConnectionLine getRelationConnectionLine() {
        for (Connection c : connections) {
            if (c.getOne() instanceof RelationConstruct || c.getTwo() instanceof RelationConstruct)
                return (ConnectionLine) c; //TODO podezrele pretypovani
        }
        return null;
    }

    /**
     * We can't change the size of the cardinality, is counted automaticily -> return null.
     */
    public cz.felk.cvut.erm.event.ResizePoint[] getResizePoints() {
        return null;
    }

    /**
     * Handle event when soma element is dragging over. Can work only with <code>ConceptualConstruct</code> instances.
     */
    public void handleDragOverEvent(DragOverEvent event) {
        if (selected && event.getAdd())
            return;
        Item item = event.getItem();
        if (item instanceof ConceptualConstructItem) {
            if (event.getAdd()) {
                ConceptualConstructItem cc = (ConceptualConstructItem) item;
                if (this.connectionTo(cc) == null) {
                    event.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
            }
        }
        event.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Handle event when soma element is dropping above. Can work only with <code>ConceptualConstruct</code> instances and
     * that action caused the reconnection to other entity or relation.
     */
    public void handleDropAboveEvent(DropAboveEvent event) {
        if (selected && event.getAdd())
            return;
        Item item = event.getItem();
        if (item instanceof ConceptualConstructItem) {
            if (event.getAdd()) {
                ConceptualConstructItem cc = (ConceptualConstructItem) item;
                if (this.connectionTo(cc) == null) {
                    reconnect(cc);
                    event.setDropped(true);
                }
            }
        }
        event.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Handle moving event and adds restrictions to BIN and UML notation
     */
    public void handleExMovingEvent(ExMovingEvent event) {
        int dx, dy = 0;
        float scale = getManager().getScale();
        java.awt.Point cardinalityCenter = getCenter();
        java.awt.Rectangle er = getEntity().getBounds();
        java.awt.Rectangle r = getBounds();
        int eventX = (int) (event.getX() / scale);
        int eventY = (int) (event.getY() / scale);
        int eventDx = event.getDx();
        int eventDy = event.getDy();
//	int eventDx = (int) (event.getDx()/scale);
//	int eventDy = (int) (event.getDy()/scale);

        if (getNotationType() == BINARY || getNotationType() == UML) {
            if (eventX < er.x) {
                dx = (er.x - r.width / 2) - cardinalityCenter.x;
                if (cardinalityCenter.y + r.height / 2 + eventDy >= er.y
                        && cardinalityCenter.y - r.height / 2 + eventDy <= er.y + er.height)
                    dy = eventDy;
                else dy = 0;
                //|| cardinalityCenter.y > er.y + er.height)?0:eventDy;
                //dy = eventDy;
            } else if (eventX > (er.x + er.width)) {
                dx = (er.x + er.width + r.width / 2) - cardinalityCenter.x;
                if (cardinalityCenter.y + r.height / 2 + eventDy >= er.y
                        && cardinalityCenter.y - r.height / 2 + eventDy <= er.y + er.height)
                    dy = eventDy;
                else dy = 0;
            } else if (eventY < er.y) {
                dx = eventDx;
                dy = (er.y - r.height / 2) - cardinalityCenter.y;
//					dy = (int) (((er.y - r.height/2) - cardinalityCenter.y)/scale);
            } else if (cardinalityCenter.y > (er.y + er.height)) {
                dx = eventDx;
                dy = (er.y + er.height + r.height / 2) - cardinalityCenter.y;
            }/* Cardinality is inside Enity */ else {
                if (cardinalityCenter.x < er.x + er.width / 2)
                    dx = er.x - r.width / 2 - cardinalityCenter.x;
                else
                    dx = er.x + er.width + r.width / 2 - cardinalityCenter.x;
            }
        } else {
            dx = eventDx;
            dy = eventDy;
        }
        if (paintedFast) {
            (manager).repaintItemFast(this);
        } else {
            paintedFast = true;
            rectangle = getBounds();
        }
        try {
            move(dx, dy, false);
        } catch (ItemNotInsideManagerException e) {
        } finally {
            (manager).repaintItemFast(this);
        }
    }

    /**
     * Handle moving event and adds restrictions to BIN and UML notation
     */
    public void handleExMoveEvent(ExMoveEvent event) {
        int dx = 0, dy = 0;
        java.awt.Point cardinalityCenter = getCenter();
        java.awt.Rectangle er = getEntity().getBounds();
        java.awt.Rectangle r = getBounds();
/*	int eventX = (int) (event.getX()/scale);
	int eventY = (int) (event.getY()/scale);
	int eventDx = (int) (event.getDx());
	int eventDy = (int) (event.getDy());
*/
        if (getNotationType() == ConceptualConstructItem.BINARY || getNotationType() == UML) {
            if (cardinalityCenter.x < er.x && cardinalityCenter.y < er.y) {
                dx = er.x - cardinalityCenter.x + r.height / 5;
                dy = er.y - cardinalityCenter.y - r.height / 2;
            } else if (cardinalityCenter.x > er.x + er.width && cardinalityCenter.y < er.y) {
                dx = er.x + er.width - cardinalityCenter.x - r.height / 5;
                dy = er.y - cardinalityCenter.y - r.height / 2;
            } else if (cardinalityCenter.x < er.x && cardinalityCenter.y > er.y + er.height) {
                dx = er.x - cardinalityCenter.x + r.height / 5;
                dy = er.y + er.height - cardinalityCenter.y + r.height / 2;
            } else if (cardinalityCenter.x > er.x + er.width && cardinalityCenter.y > er.y + er.height) {
                dx = er.x + er.width - cardinalityCenter.x - r.height / 5;
                dy = er.y + er.height - cardinalityCenter.y + r.height / 2;
            } else {
            }
        } else {
            dx = event.getDx();
            dy = event.getDy();
        }
        if (paintedFast) {
            (manager).repaintItemFast(this);
            paintedFast = false;
        } else {
            rectangle = getBounds();
        }
        try {
            move(dx, dy, true);
            move(event.getDx(), event.getDy(), true);
            if (rectangle != null) {
                r = rectangle;
                rectangle = null;
                (manager).repaintRectangle(r.x, r.y, r.width, r.height);
            }
            (manager).repaintItem(this);
        } catch (ItemNotInsideManagerException e) {
        }
    }

    /**
     * Handle remove event and adds only one functionality -> remove cardinality from the model's object.
     */
    public void handleRemoveEvent(cz.felk.cvut.erm.event.RemoveEvent event) {
        try {
            RelationConstruct rel = getRelation();
            rel.removeCardinality(this);
            super.handleRemoveEvent(event);
/*	neni mozno pouzit protoze pri decompose to chce mazat vztah, ktery je posleze smazan jinou metodou, ktera jej nenajde
  			if(getNotationType() != CHEN)
			if (rel.getConnections() == null || rel.getConnections().size() == 0)
				rel.handleRemoveEvent(new RemoveEvent(rel.getBounds().x, rel.getBounds().y, null));
*/
        } catch (Throwable x) {
            new ShowException(null, "Error", x, true);
        }
    }

    public void moveCardinality(ExMovingEvent event) {
        this.handleExMovingEvent(event);
        this.handleExMoveEvent(event);
    }

    /**
     * Determine whether this cardinality is useful to find compactable entity.
     *
     * @return <code>true</code> if it is.
     */
    public boolean isCompactable() {
        return (model.getArbitrary() && !model.getMultiCardinality());
    }

    /**
     * Determine whether the entity has multi participation on the relation.
     *
     * @return <code>true</code> if it has.
     */
    public boolean isMultiCardinality() {
        return model.getMultiCardinality();
    }

    /**
     * Paints the cardinality - it means to draw integrit restrictions and role name.
     */
    public void paint(java.awt.Graphics g) {
        final Stroke stroke = updateStrokeWithAliasing(g);
        java.awt.Rectangle r = getBounds();
        String name = model.getName();
        boolean arbitrary = model.getArbitrary();
        boolean multiCard = model.getMultiCardinality();
        java.awt.FontMetrics fm = g.getFontMetrics();
        String ir;
        switch (getNotationType()) {
            case CHEN:
                if (selected) {
                    g.setColor(getSelectedBackgroundColor());
                    g.fillRect(r.x, r.y, r.width, r.height);
                }
                g.setColor(getForegroundColor());
                ir = ((arbitrary) ? "1" : "0") + ":" + ((multiCard) ? "N" : "1");
                g.drawString(ir, r.x + (r.width - fm.stringWidth(ir)) / 2, r.y + fm.getAscent());
                g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + r.height);
                break;
            case BINARY:
                if (selected) {
                    g.setColor(getSelectedBackgroundColor());
                    g.fillRect(r.x, r.y, r.width, r.height);
                }
                g.setColor(getForegroundColor());
                ir = ((arbitrary) ? "1" : "0") + ":" + ((multiCard) ? "N" : "1");
                java.awt.Point cardinalityCenter = getCenter();
                java.awt.Rectangle er = getEntity().getBounds();

                if (cardinalityCenter.x < er.x && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width, r.y + r.height / 2);
                    if (multiCard) {
                        g.drawLine(r.x + r.width, r.y + r.height / 2 - r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                        g.drawLine(r.x + r.width, r.y + r.height / 2 + r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                    }
                    g.drawString(name, r.x, r.y + r.height);
                    paintLineToCardinality(g, false);
                } else
                if (cardinalityCenter.x > (er.x + er.width) && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height / 2);
                    if (multiCard) {
                        g.drawLine(r.x, r.y + r.height / 2 - r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                        g.drawLine(r.x, r.y + r.height / 2 + r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                    }
                    g.drawString(name, r.x + (r.width - fm.stringWidth(name)), r.y + r.height);
                    paintLineToCardinality(g, false);
                } else if (cardinalityCenter.y < er.y) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height);
                    if (multiCard) {
                        g.drawLine(r.x + r.width / 2 - r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                        g.drawLine(r.x + r.width / 2 + r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                    }
                    g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + fm.getAscent());
                    paintLineToCardinality(g, true);
                } else if (cardinalityCenter.y > (er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y, r.x + r.width / 2, r.y + r.height / 2);
                    if (multiCard) {
                        g.drawLine(r.x + r.width / 2 - r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                        g.drawLine(r.x + r.width / 2 + r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                    }
                    g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + r.height);
                    paintLineToCardinality(g, false);
                } else {
                    g.drawRect(r.x, r.y, r.width, r.height);
                }
                break;
            case UML:
                if (selected) {
                    g.setColor(getSelectedBackgroundColor());
                    g.fillRect(r.x, r.y, r.width, r.height);
                }
                g.setColor(getForegroundColor());
                ir = ((arbitrary) ? "1" : "0") + ".." + ((multiCard) ? "*" : "1");
                if (Schema.SHOW_SHORTEN_CARD_IN_UML == 1 && !arbitrary && multiCard) ir = "*";
                if (Schema.SHOW_SHORTEN_CARD_IN_UML == 1 && arbitrary && !multiCard) ir = "1";
                g.drawString(ir, r.x + (r.width - fm.stringWidth(ir)) / 2, r.y + fm.getAscent());
                g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + r.height - fm.getAscent() / 4);
                break;
        }
        updateBackupStroke(g, stroke);
    }

    private void paintLineToCardinality(java.awt.Graphics g, boolean UP) {
        java.awt.Rectangle r = getBounds();
        Point rcenter = getRelation().getRealCenter();
        Point ccenter = getRealCenter();
        int dx = rcenter.x - ccenter.x;
        int dy = ccenter.y - rcenter.y;
        float konst = (float) dy / (float) dx;

        Graphics2D g2 = (Graphics2D) g;
        float dash1[] = {8f};//default 4.5f
        float dash2[] = {333333.5f};
        if (!model.getArbitrary()) {
            BasicStroke roundStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash1, 0.0f);
            g2.setStroke(roundStroke);
        }
        // na horni stenu
        if (dy > 0 && (((r.width / 2) * konst) > r.height / 2 || ((r.width / 2) * konst) < -r.height / 2)) {
            if (!UP)
                g.drawLine(r.x + r.width / 2, r.y + r.height / 2, (int) (r.x + r.width / 2 + (r.height / 2) / konst), r.y);
        } else if (dy < 0 && (((r.width / 2) * konst) > r.height / 2 || ((r.width / 2) * konst) < -r.height / 2)) {
            if (UP)
                g.drawLine(r.x + r.width / 2, r.y + r.height / 2, (int) (r.x + r.width / 2 - (r.height / 2) / konst), r.y + r.height);
        } else {
            if (dx > 0)
                g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width, (int) (r.y + r.height / 2 - (r.width / 2) * konst));
            if (dx < 0)
                g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x, (int) (r.y + r.height / 2 + (r.width / 2) * konst));
        }
        if (!model.getArbitrary()) {
            BasicStroke lineStroke = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 100f, dash2, 0.0f);
            g2.setStroke(lineStroke);
        }
    }

    /**
     * Prints the cardinality on the printer graphics. Same as paint but don't changes the colors.
     */
    public void print(java.awt.Graphics g) {
        java.awt.Rectangle r = getBounds();
        String name = model.getName();
        boolean arbitrary = model.getArbitrary();
        boolean multiCard = model.getMultiCardinality();
        java.awt.FontMetrics fm = g.getFontMetrics();
        String ir;
        switch (getNotationType()) {
            case CHEN:
                g.setColor(getForegroundColor());
                ir = ((arbitrary) ? "1" : "0") + ":" + ((multiCard) ? "N" : "1");
                g.drawString(ir, r.x + (r.width - fm.stringWidth(ir)) / 2, r.y + fm.getAscent());
                g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + r.height);
                break;
            case BINARY:
                g.setColor(getForegroundColor());
                ir = ((arbitrary) ? "1" : "0") + ":" + ((multiCard) ? "N" : "1");
                java.awt.Point cardinalityCenter = getCenter();
                java.awt.Rectangle er = getEntity().getBounds();

                if (cardinalityCenter.x < er.x && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width, r.y + r.height / 2);
                    if (multiCard) {
                        g.drawLine(r.x + r.width, r.y + r.height / 2 - r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                        g.drawLine(r.x + r.width, r.y + r.height / 2 + r.height / 5, r.x + r.width - r.height / 2, r.y + r.height / 2);
                    }
                    g.drawString(name, r.x, r.y + r.height);
                    paintLineToCardinality(g, false);
                } else
                if (cardinalityCenter.x > (er.x + er.width) && !(cardinalityCenter.y < er.y || cardinalityCenter.y > er.y + er.height)) {
                    g.drawLine(r.x, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height / 2);
                    if (multiCard) {
                        g.drawLine(r.x, r.y + r.height / 2 - r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                        g.drawLine(r.x, r.y + r.height / 2 + r.height / 5, r.x + r.height / 2, r.y + r.height / 2);
                    }
                    g.drawString(name, r.x + (r.width - fm.stringWidth(name)), r.y + r.height);
                    paintLineToCardinality(g, false);
                } else if (cardinalityCenter.y < er.y) {
                    g.drawLine(r.x + r.width / 2, r.y + r.height / 2, r.x + r.width / 2, r.y + r.height);
                    if (multiCard) {
                        g.drawLine(r.x + r.width / 2 - r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                        g.drawLine(r.x + r.width / 2 + r.height / 5, r.y + r.height, r.x + r.width / 2, r.y + r.height / 2 + 1);
                    }
                    g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + fm.getAscent());
                    paintLineToCardinality(g, true);
                } else if (cardinalityCenter.y > (er.y + er.height)) {
                    g.drawLine(r.x + r.width / 2, r.y, r.x + r.width / 2, r.y + r.height / 2);
                    if (multiCard) {
                        g.drawLine(r.x + r.width / 2 - r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                        g.drawLine(r.x + r.width / 2 + r.height / 5, r.y, r.x + r.width / 2, r.y + r.height / 2 - 1);
                    }
                    g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + r.height);
                    paintLineToCardinality(g, false);
                } else {
                    g.drawRect(r.x, r.y, r.width, r.height);
                }
                break;
            case UML:
                g.setColor(getForegroundColor());
                ir = ((arbitrary) ? "1" : "0") + ".." + ((multiCard) ? "*" : "1");
                if (Schema.SHOW_SHORTEN_CARD_IN_UML == 1 && !arbitrary && multiCard) ir = "*";
                if (Schema.SHOW_SHORTEN_CARD_IN_UML == 1 && arbitrary && !multiCard) ir = "1";
                g.drawString(ir, r.x + (r.width - fm.stringWidth(ir)) / 2, r.y + fm.getAscent());
                g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, r.y + r.height - fm.getAscent() / 4);
                break;
        }
    }

    /**
     * Invoked when some property changes its value. When the property is <b>name</b> (role name) then recounts the size and
     * always when invoken repaints the item.
     */
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        ((ConceptualObject) getModel()).setChanged(true);
        if (e.getPropertyName().equals("name")) {
            int[][] r = rect;
            java.awt.Dimension dim = countSize(), real = new java.awt.Dimension(r[0][1] - r[0][0], r[1][1] - r[1][0]);
            int dx = dim.width - real.width;
            int dy = dim.height - real.height;
            java.awt.Rectangle b = getBounds();
            try {
                resize(dx, dy, ResizePoint.RIGHT | ResizePoint.BOTTOM, true);
            } catch (ItemNotInsideManagerException ex) {
            }
            b = b.union(getBounds());
            (manager).repaintRectangle(b.x, b.y, b.width, b.height);
        }
        if (e.getPropertyName().equals("arbitrary")) {
            ConnectionLine conn = getRelationConnectionLine();
            conn.setConnectionMandatory(model.getArbitrary());
            if (getNotationType() == BINARY) {
                java.awt.Rectangle r = conn.getBounds();
                conn.getManager().repaintRectangle(r.x, r.y, r.width, r.height);
            }
        }
        java.awt.Rectangle b = getBounds();
        (manager).repaintRectangle(b.x, b.y, b.width, b.height);

    }

    /**
     * Is used to sets this instance as <code>propertyChangeListener</code> for its model. Invoked automaticly after
     * deserializaing of the instance the atribute.
     */
    private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        //set yourself as property change listener
        try {
            model.addPropertyChangeListener(this);
        } catch (NullPointerException e) {
            e.printStackTrace(); //LV
        }
    }

    /**
     * Invoken when wants to change its relation (entity participation on other relation - <code>cc</code>) or entity
     * (participation other entity - <code>cc</code>).
     *
     * @param cc Whether instance of the <code>Entity</code> changes the entity otherwise (instance <code>Relation</code>)
     *           change the relation.
     */
    protected void reconnect(ConceptualConstructItem cc) {
        if (cc instanceof EntityConstruct) {
            //changes the participating entity
            EntityConstruct old = getEntity();
            Connection conn = connectionTo(old);
            Entity cEnt = (Entity) cc.getModel();
            try {
                model.setEntity(cEnt);
                cc.getManager().addItem(this);
                //reconnect
                if (conn.getOne() == old) {
                    conn.setOne(cc);
                    return;
                }
                if (conn.getTwo() == old) {
                    conn.setTwo(cc);
                    return;
                }
            } catch (Throwable x) {
                new ShowException(null, "Error", x, true);
            }
        }
        if (cc instanceof RelationConstruct) {
            //change the relation on which participate
            RelationConstruct old = getRelation();
            Connection conn = connectionTo(old);
            Relation cRel = (Relation) cc.getModel();
            try {
                model.setRelation(cRel);
                if (conn.getOne() == old) {
                    conn.setOne(cc);
                    return;
                }
                if (conn.getTwo() == old) {
                    conn.setTwo(cc);
                }
            } catch (Throwable x) {
                new ShowException(null, "Error", x, true);
            }
        }
    }

    /**
     * This method was created by Jiri Mares
     */
    public void transformToRelation(EntityConstruct ent, Manager man) {
        //others cardinalities decompose as new relations
        java.awt.Point p = ent.getCenter(getEntity());
        //noinspection SuspiciousNameCombination
        RelationConstruct rel = RelationConstruct.createRelation(model.getSchema(), man, p.x, p.y);
        ((Relation) rel.getModel()).setName(model.getName());
        model.setName("");
        reconnect(getRelation());
        p = ent.getCenter(getRelation());
        CardinalityConstruct car = rel.createCardinality(ent, manager, p.x, p.y);
        ((Cardinality) car.getModel()).setArbitrary(true);
        ((Cardinality) car.getModel()).setMultiCardinality(false);
    }

    /**
     * This method was created by Jiri Mares
     */
    public void transformToStrongAddiction(EntityConstruct son, Manager man) {
        //java.awt.Point p = son.getCenter(getEntity());
//	StrongAddiction.createStrongAddiction(getEntity(), son, son.getManager(), p.x, p.y);
        StrongAddiction.createStrongAddiction(getEntity(), son, son.getManager(), getBounds().x, getBounds().y);
    }

    /**
     * Writes data for cardinality into XML file
     *
     * @param pw java.io.PrintWriter
     */
    public void write(java.io.PrintWriter pw) {
        pw.println("\t<cardinality>");
        super.write(pw);
        pw.println("\t</cardinality>");
    }
}

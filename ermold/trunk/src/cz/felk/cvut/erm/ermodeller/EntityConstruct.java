package cz.felk.cvut.erm.ermodeller;


import cz.felk.cvut.erm.conceptual.NotationType;
import cz.felk.cvut.erm.conceptual.beans.Cardinality;
import cz.felk.cvut.erm.conceptual.beans.Entity;
import cz.felk.cvut.erm.conceptual.beans.Schema;
import cz.felk.cvut.erm.conceptual.beans.UniqueKey;
import cz.felk.cvut.erm.conceptual.exception.CannotHavePrimaryKeyException;
import cz.felk.cvut.erm.conceptual.exception.CycleWouldAppearException;
import cz.felk.cvut.erm.conceptual.exception.IsISASonException;
import cz.felk.cvut.erm.conceptual.exception.WasNotFoundException;
import cz.felk.cvut.erm.dialogs.ConstraintsDialog;
import cz.felk.cvut.erm.ermodeller.interfaces.FontManager;
import cz.felk.cvut.erm.ermodeller.interfaces.ISchema;
import cz.felk.cvut.erm.event.*;
import cz.felk.cvut.erm.event.exceptions.ImpossibleNegativeValueException;
import cz.felk.cvut.erm.event.exceptions.ItemNotInsideManagerException;
import cz.felk.cvut.erm.event.interfaces.Invokable;
import cz.felk.cvut.erm.event.interfaces.Item;
import cz.felk.cvut.erm.event.interfaces.Manager;
import cz.felk.cvut.erm.event.interfaces.PaintableManager;
import cz.felk.cvut.erm.eventtool.ConnectionLine;
import cz.felk.cvut.erm.eventtool.interfaces.Connection;
import cz.felk.cvut.erm.eventtool.interfaces.ConnectionManager;
import cz.felk.cvut.erm.swing.ShowException;

import javax.swing.*;
import java.awt.*;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Represents the entity construct. Entity object is always inserted to the manager (instance of the class DGroup) and
 * it caused the moving atributes and unique key together with its entity.
 *
 * @see DGroupTool
 */
public class EntityConstruct extends ConceptualConstructItem {
    /**
     * Points between two rect, when this entity has strong addiction parents
     */
    static final private int DIFFERENCE = 4;

    /**
     * Distance between Entity and Relation when the Self Relation is called
     */
    static final private int SELFRELATIONDISTANCE = 140;

    /**
     * The model object from the Ale� Kopeck� work
     */
    protected Entity model = null;

    /**
     * The primary unique key of this entity
     */
    protected final UniqueKeyConstruct primary = null;

    /**
     * Determine whether this entity is strong addiction child - drawn as double rect
     */
    protected boolean isStrongAddictionChild = false;

    /**
     * All ISA childs
     */
    protected Vector<EntityConstruct> ISAChilds = null;

    /**
     * Members of Primary key attributes
     */
    protected Vector<AttributeConstruct> PKmembers = null;

    /** All attributes */
//	protected java.util.Vector attribs = null;

    /**
     * Editor of Cosntraints
     */
    ConstraintsDialog constDialog;

    /**
     * The ISA parent
     */
    protected EntityConstruct ISAParent = null;

    /**
     * Was just now sets as ISA child
     */
    transient protected boolean setAsISAChild = false;

    /**
     * By which way to decompose this entity
     */
    transient protected boolean decomposeAsRelation = true;

    /**
     * Width of Primary Key in BINARY notation
     */
    protected int PKwidth = 0;

    protected Manager EntManager;


    /**
     * Creates entity, counts size to fit the name of the entity.
     *
     * @param ent     The model object - entity from the Ale� Kopeck� work.
     * @param manager The window group where to put the new relation. Instance of the DGroup class.
     * @param left    The x coordinate of the left top point of the new entity.
     * @param top     The y coordinate of the left top point of the new entity.
     * @throws <code>java.lang.NullPointerException</code>
     *          Thrown by inherited constructor.
     * @throws <code>cz.green.event.ImpossibleNegativeValueException</code>
     *          Thrown by inherited constructor.
     * @see ConceptualConstructItem#ConceptualConstructItem(cz.felk.cvut.erm.event.interfaces.Manager ,int,int,int,int)
     */
    protected EntityConstruct(Entity ent,
                              Manager manager, int left, int top)
            throws NullPointerException,
            ImpossibleNegativeValueException {
        super(manager, left, top, 0, 0);
        ent.addPropertyChangeListener(this);
        String name = (model = ent).getName();
        //ent.setConstraints("�lu�ou�k� k��");
        EntManager = manager;
        FontMetrics fm;
        PKmembers = new java.util.Vector<AttributeConstruct>(3, 2);
        try {
            fm = ((FontManager) manager).getReferentFontMetrics();
            int width = fm.stringWidth(name), height = fm.getAscent();
            rect[0][1] = rect[0][0] + 2 * width + height;
            rect[1][1] = rect[1][0] + 3 * height;
        } catch (ClassCastException e) {
            e.printStackTrace(); // pridano LV
        }
    }

    /**
     * Add ISA child to this entity. Recounts the size and do all needed.
     *
     * @param ent   The added entity.
     * @param event Event to can post remove event.
     */
    private void addISAChild(EntityConstruct ent, CoordinateEvent event) {
        try {
            int[][] r = ent.getRect();
            int height = countResizeBottom(-1);
            int[][] s = getRect();
            int dy = r[1][1] - r[1][0] + 2 * DIFFERENCE + height - s[1][1]
                    + s[1][0];
            int dx = (r[0][1] - r[0][0] + 4 * DIFFERENCE - rect[0][1] + rect[0][0]);
            dx = (dx > 0) ? dx : 0;
            dy = (dy > 0) ? dy : 0;
            ResizeRectangle rr = new ResizeRectangle(
                    0, 0, 0, 0, ResizePoint.BOTTOM
                    | ResizePoint.RIGHT);
            ResizeEvent ev = new ResizeEvent(event.getX(), event.getY(), dx, dy, rr, event.getComponent());
            handleResizeEvent(ev);
            ent.setISAParent(manager, this,
                    (s[0][1] + s[0][0] + r[0][0] - r[0][1]) / 2, s[1][0]
                    + height, event);
            if (ISAChilds == null)
                ISAChilds = new Vector<EntityConstruct>(3, 2);
            ISAChilds.add(ent);
        } catch (Throwable x) {
            new ShowException(null, "Error", x, true);
        }
    }

    /**
     * Tests whether this entity and ent are compactable. Try way from primary key of this entity, then the way to primary
     * key others entities and finaly the relations, between two relations.
     *
     * @param ent The tested entity);
     * @return <code>true</code> if they are.
     * @see compactableRelation( EntityConstruct )
     * @see UniqueKeyConstruct#areOthersConnections(EntityConstruct)
     */
    protected boolean areCompactable(EntityConstruct ent) {
        if (primary != null)
            if (!primary.areOthersConnections(ent))
                return true;
        UniqueKeyConstruct uk;
        for (int i = connections.size() - 1; i >= 0; i--) {
            if ((uk = (UniqueKeyConstruct) ((Connection) connections.get(i))
                    .isConnectedTo(UniqueKeyConstruct.class)) != null) {
                if ((uk.getOwner() == ent) && (!uk.areOthersConnections(this)))
                    return true;
            }
        }
        return (compactableRelation(ent) != null);
    }

    /**
     * entity.
     *
     * @param ent   The second entity.
     * @param event Needful to pass remove event.
     * @see #changeAllStrongAddictions(EntityConstruct)
     * @see ConceptualConstructItem#reconnectAllCardinalities(EntityConstruct)
     * @see ConceptualConstructItem#moveAllAtributes(EntityConstruct)
     */
    protected void compact(EntityConstruct ent, cz.felk.cvut.erm.event.CoordinateEvent event) {
        EntityConstruct from, to;
        if ((primary != null) && (!primary.areOthersConnections(ent))) {
            from = this;
            to = ent;
        } else {
            RelationConstruct rel = compactableRelation(ent);
            if (rel != null)
                rel.handleRemoveEvent(new cz.felk.cvut.erm.event.RemoveEvent(event
                        .getX(), event.getY(), event.getComponent()));
            from = ent;
            to = this;
        }
        from.changeAllStrongAddictions(to);
        from.reconnectAllCardinalities(to);
        from.reconnectAllAtributes(to);
        from.handleRemoveEvent(new cz.felk.cvut.erm.event.RemoveEvent(event.getX(),
                event.getY(), event.getComponent()));
    }

    /**
     * Looks for the relation, by which it is possible to compact.
     *
     * @param ent The entity with which we have to compact
     * @return Such relation or when doesn't exists null.
     */
    protected RelationConstruct compactableRelation(EntityConstruct ent) {
        CardinalityConstruct car;
        for (int i = connections.size() - 1; i >= 0; i--) {
            if ((car = (CardinalityConstruct) ((Connection) connections.get(i))
                    .isConnectedTo(CardinalityConstruct.class)) != null) {
                if (car.isCompactable()
                        && car.getRelation().compactConnection(ent, this))
                    return car.getRelation();
            }
        }
        return null;
    }

    public java.util.List<Connection> getConnections() {
        return connections;
    }

    /**
     */
    public void composeEntity(EntityConstruct ent, cz.felk.cvut.erm.event.CoordinateEvent event) {
        reconnectChilds(ent, event);
        this.changeAllStrongAddictions(ent);
        this.reconnectAllCardinalities(ent);
        super.reconnectAllAtributes(ent);
        ent.recalculatePositionsOfAtributes();
/*		v1 = getUniqueKeys();
		try {
			for (i = 0; i < v1.size(); i++) {
				uk1 = (UniqueKey) v1.get(i);
				l = uk1.getL();
				t = uk1.getT();
				uk2 = ent.createUniqueKey(l, t);
				if (uk1.getPrimary())
					uk2.setPrimary();
				v2 = uk1.getAtributes();
				for (j = 0; j < v2.size(); j++) {
					atr = (Atribute) v2.get(j);
					ent.reconnectAtribute(atr);
					uk2.addAtribute(atr);
				}
				uk1.handleRemoveEvent(new RemoveEvent(event.getX(), event
						.getY(), event.getComponent()));
			}
		} catch (Exception e) {
			new ShowException(null, "Error", e, true);
		}
*/
        this.handleRemoveEvent(new RemoveEvent(event.getX(), event.getY(),
                event.getComponent()));
    }

    /**
     * Determines whether the resizing specified by <code>ev</code> is possible (after holds all ISA childs). If its not
     * changes the dx and dy properties the event to be the resize maximal possible.
     *
     * @param ev The resize event, which will be handled.
     */
    protected void countMinSize(cz.felk.cvut.erm.event.ResizeEvent ev) {
        int r[][];
        int width = 60;
        FontMetrics fm;
        fm = ((FontManager) manager).getReferentFontMetrics();
        int nameWidth = fm.stringWidth((getModel()).getName());
        if (nameWidth > width)
            width = nameWidth;
        final NotationType type = model.getSchema().getNotationType();
        switch (type) {
            case CHEN:
                break;
            case BINARY:
                for (int i = 0; i < getAtributes().size(); i++) {
                    AttributeConstruct a = getAtributes().get(i);
                    int x = a.getBounds().width;
                    if (x > width) width = x;
                }
                if (PKwidth > width) width = PKwidth;
                break;
            case UML:
                for (int i = 0; i < getAtributes().size(); i++) {
                    AttributeConstruct a = getAtributes().get(i);
                    int x = a.getBounds().width +
                            fm.stringWidth(": ") +
                            fm.stringWidth((a.getModel()).getDataType().toDescriptionString());
                    if (x > width) width = x;
                }
                break;
        }
        if (type != NotationType.CHEN) {
            for (int i = 0; i < getAtributes().size(); i++) {
                AttributeConstruct a = getAtributes().get(i);
                int x = a.getBounds().width;
                if (x > width)
                    width = x;
            }
            if (type == NotationType.BINARY) {
                if (PKwidth > width)
                    width = PKwidth;
            }
        }
        if (ISAChilds != null) {
            //Enumeration<EntityConstruct> e = ISAChilds.elements();
            // counts maximal width
            for (EntityConstruct entityConstruct : ISAChilds) {
                r = entityConstruct.getRect();
                int h = r[0][1] - r[0][0];
                if (h > width)
                    width = h;
            }
        }
        width += 4 * DIFFERENCE;
        r = getRect();
        // test whether dx is possible, if not have to change
        int dx = width - r[0][1] + r[0][0];
        int real = ((ev.getResizeRect().direction & ResizePoint.LEFT) == ResizePoint.LEFT) ? -ev
                .getDx()
                : ev.getDx();
        if (real < dx) {
            ev.setDx(((ev.getResizeRect().direction & ResizePoint.LEFT) == ResizePoint.LEFT) ? -dx
                    : dx);
        }
        // count max dy
        int dy = countResizeBottom(-1) - r[1][1] + r[1][0];
//		int dy = - r[1][1] + r[1][0];
        real = ((ev.getResizeRect().direction & ResizePoint.TOP) == ResizePoint.TOP) ? -ev
                .getDy()
                : ev.getDy();
        // if event dy is greater - change it
        if (real < dy) {
            ev
                    .setDy(((ev.getResizeRect().direction & ResizePoint.TOP) == ResizePoint.TOP) ? -dy
                            : dy);
        }
    }

    /**
     * Counts move the ISA child <code>from</code> when it knows the index item before (from it we can count the needed
     * posstion).
     *
     * @param e      Event needful to construct returning event.
     * @param from   The moving entity.
     * @param before The index in the ISA childs the previous entity.
     * @return Creates the event to process to the ISA child to move it to the right possition.
     */
    protected cz.felk.cvut.erm.event.MovingEvent countMove(
            cz.felk.cvut.erm.event.CoordinateEvent e, EntityConstruct from, int before) {
        // create the event
        cz.felk.cvut.erm.event.MovingEvent mev = new cz.felk.cvut.erm.event.MovingEvent(e
                .getX(), e.getY(), 0, 0, e.getComponent());
        int[][] s = getRect();
        int[][] r = from.getRect();
        int top;
/*		if (ConceptualConstruct.type == ConceptualConstruct.BINARY) {
		top += 20 * ((ConceptualConstruct) from).getAtributes().size(); 
		}
*/
        final Schema schema = model.getSchema();
        final NotationType type = schema.getNotationType();
        try {
            if (before == -1) {
                // if it is first -> count size from the top
                FontMetrics fm;
                fm = ((FontManager) manager).getReferentFontMetrics();
                top = s[1][0] + 3 * fm.getAscent();
                if (type != NotationType.CHEN) {
                    switch (schema.getLevelOfDetails()) {
                        case (Schema.LOD_FULL):
                            top += 20 * getAtributes().size();
                            if (type == BINARY && PKmembers != null && PKmembers.size() > 0)
                                top -= 20 * (PKmembers.size() - 1);
                            break;
                        case (Schema.LOD_MEDIUM):
                            if (type == BINARY && PKmembers != null && PKmembers.size() > 0) top += 20;
                            if (type == UML && PKmembers != null && PKmembers.size() > 0)
                                top += 20 * PKmembers.size();
                            break;
                    }
                }
            } else {
                // else count the top from the bottom of the previous
                top = (ISAChilds.get(before)).getRect()[1][1]
                        + 2 * DIFFERENCE;
            }
        } catch (Throwable ex) {
            top = s[1][0] + 8 * DIFFERENCE;
        }
        // sets the move differences
        mev.setDy(top - r[1][0]);
        mev.setDx(s[0][0] - r[0][0] + (s[0][1] - s[0][0] - (r[0][1] - r[0][0]))
                / 2);
        return mev;
    }

    /**
     * Counts the resize event to this entity when it knows which entity wants to resize and how much. Test whether after
     * resizing ISA child can this entity holds all ISA childs again.
     *
     * @param e     How much will be resized ISAchild entity.
     * @param from  The resized entity.
     * @param index The index the previous ISA child.
     * @return Resize event to resize this entity.
     */
    protected cz.felk.cvut.erm.event.ResizingEvent countResize(
            cz.felk.cvut.erm.event.ResizeEvent e, EntityConstruct from, int index) {
        int[][] r = from.getRect();
        int[][] s = getRect();
        // creates new event
        ResizeRectangle rr = e.getResizeRect();
        ResizeRectangle newr = new ResizeRectangle(
                0, 0, 0, 0, 0);
        cz.felk.cvut.erm.event.ResizingEvent newe = new cz.felk.cvut.erm.event.ResizingEvent(e
                .getX(), e.getY(), 0, 0, newr, e.getComponent());
        newr.direction |= rr.direction
                & (ResizePoint.LEFT | ResizePoint.RIGHT);
        newr.direction |= ResizePoint.BOTTOM;
        // count resize size
        if (e.getDy() != 0) {
            int dy = ((rr.direction & ResizePoint.TOP) == ResizePoint.TOP) ? -e.getDy() : e.getDy();
            if (index != -1) {
                dy = dy + countResizeBottom(index) - s[1][1] + r[1][1];
            }
            if (dy > 0) {
                newe.setDy(dy);
            }
        }
        if (e.getDx() != 0) {
            int dx = ((rr.direction & ResizePoint.LEFT) == ResizePoint.LEFT) ? -e.getDx() : e.getDx();
            dx += r[0][1] - r[0][0] + 4 * DIFFERENCE - s[0][1] + s[0][0];
            if (dx > 0) {
                newe.setDx(((rr.direction & ResizePoint.LEFT) == ResizePoint.LEFT) ? -dx : dx);
            }
        }
        return newe;
    }

    /**
     * Counts the space to holds the ISA childs from <code>from</code>.
     *
     * @param from From which child we wants to count.
     * @return The vertical space needful to holds all ISA childs.
     */
    private int countResizeBottom(int from) {
        int l = 2 * DIFFERENCE, height = l;
        int[][] r;
        getRect();
        final NotationType type = model.getSchema().getNotationType();
        if (from == -1) {
            try {
                FontMetrics fm;
                fm = ((FontManager) manager).getReferentFontMetrics();
                height = 3 * fm.getAscent();
                if (type == BINARY) {
                    switch (getLevelOfDetails()) {
                        case (Schema.LOD_LOW):
                            break;
                        case (Schema.LOD_MEDIUM):
                            if (PKmembers.size() != 0)
                                height += fm.getAscent();
                            break;
                        case (Schema.LOD_FULL):
                            height += 20 * getAtributes().size();
                            if (PKmembers.size() != 0)
                                height -= 20 * (PKmembers.size() - 1);
                            break;
                    }
                }
                if (type == UML) {
                    switch (getLevelOfDetails()) {
                        case (Schema.LOD_LOW):
                            break;
                        case (Schema.LOD_MEDIUM):
                            height += 20 * PKmembers.size();
                            break;
                        case (Schema.LOD_FULL):
                            height += 20 * getAtributes().size();
                            if (model.getConstraints().length() > 0)
                                height += 3 * fm.getAscent();
                            break;
                    }
                }
            } catch (Throwable ex) {
                height = 8 * DIFFERENCE;
            }
        }
        if (ISAChilds != null) {
            for (int i = ISAChilds.size() - 1; i > from; i--) {
                r = (ISAChilds.get(i)).getRect();
                height += (r[1][1] - r[1][0]) + l;
            }
        }
        return height;
    }

    /**
     * Is called to create new entity with exact size
     *
     * @param schema  Is needful because model object entity creates model object schema.
     * @param manager Where to put the new Dgroup instance created to put into new Entity instance.
     * @param left    The x coordinate of the left top point of the new entity.
     * @param top     The y coordinate of the left top point of the new entity.
     * @param weight  The weight of the new entity.
     * @param height  The height of the new entity.
     * @param old     The decomposing entity.
     * @return The new created entity.
     */
    static public EntityConstruct createEntity(
            Schema schema,
            Manager manager, int left, int top, int width, int height, EntityConstruct old) {
        EntityConstruct ent;
        ent = createEntity(schema, manager, left, top, old);
        java.awt.Rectangle r = ent.getBounds();
        try {
            //System.out.println("left: " + left + "width: " + width + "r.x: " + r.x);
            ent.resize((left + width) - (r.x + r.width), (top + height) - (r.y + r.height),
                    (ResizePoint.RIGHT | ResizePoint.BOTTOM), true);
        } catch (ItemNotInsideManagerException e1) {
            e1.printStackTrace();
        }
        return ent;
    }


    /**
     * Is called to create new entity
     *
     * @param schema  Is needful because model object entity creates model object schema.
     * @param manager Where to put the new Dgroup instance created to put into new Entity instance.
     * @param left    The x coordinate of the left top point of the new entity.
     * @param top     The y coordinate of the left top point of the new entity.
     * @param old     The decomposing entity.
     * @return The new created entity.
     */
    static public EntityConstruct createEntity(
            Schema schema,
            Manager manager, int left, int top, EntityConstruct old) {

        try {
// creates the new DGroup instance
            DGroupTool group = new DGroupTool(manager, left, top, 0, 0);
            Entity cEnt = schema
                    .createEntity();
            // creates new entity
            EntityConstruct ent = new EntityConstruct(cEnt, manager, left, top);
            group.addItem(ent);
            // decompose
            if (old != null) {
                old.decompose(ent, manager);
            }
            // adds group to the manager
            manager.addItem(group);
            manager.repaintItem(group);
            return ent;
        } catch (ImpossibleNegativeValueException e) {
            e.printStackTrace();
        } catch (ItemNotInsideManagerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * This method adds items to the context menu, which are specific to the entity.
     *
     * @param menu  The popup menu where to add the new items.
     * @param event The event, which caused the context menu displaying. Is useful for determing targets of the methods
     *              call.
     * @return The filled menu.
     */
    protected JPopupMenu createMenu(JPopupMenu menu, PopUpMenuEvent event) {
        super.createMenu(menu, event);
        final NotationType type = model.getSchema().getNotationType();
/*		addMenuItem(menu, "Add unique key", "mUKey.gif", event.getComponent(),
				"addingUniqueKey", this,
				cz.felk.cvut.erm.ermodeller.ConceptualConstruct.class);*/
        addMenuItem(menu, "Add atribute", "img/mAtribute.gif", event.getComponent(), "addingAtribute", this, ConceptualConstructItem.class);
        addMenuItem(menu, "Add Relationship to other Entity ...", "img/mAddRelation.gif", event.getComponent(),
                "addingRelationCon", this,
                EntityConstruct.class);
        addMenuItem(menu, "Add Connection to Relationship ...", "img/mAddConnection.gif", event.getComponent(),
                "addingConnectionToRel", this,
                EntityConstruct.class);
        addMenuItem(menu, "Add Identification dependency on ...", "img/mAddIdentDep.gif", event.getComponent(),
                "addingIdentDependency", this,
                EntityConstruct.class);
        if (ISAParent == null) {
            addMenuItem(menu, "Set as ISA child of ...", "img/mSetAsISAChild.gif", event.getComponent(),
                    "addingAsISAChild", this,
                    EntityConstruct.class);
        }
        if (ISAParent != null && ISAChilds != null && ISAChilds.size() > 0) {
            addMenuItem(menu, "Reset ISA parent", "img/mResetISAParent.gif", this,
                    "resetISAParent", event,
                    cz.felk.cvut.erm.event.CoordinateEvent.class);
        }
        if (ISAChilds != null && ISAChilds.size() > 0) {
            addMenuItem(menu, "Remove ISA childs", "img/mRemoveISAChilds.gif", this,
                    "removeISAChilds", event,
                    cz.felk.cvut.erm.event.CoordinateEvent.class);
        }
        addMenuItem(menu, "Add identification dependent Entity ...",
                "img/mDSAEntity.gif", this, "decomposeUsingStrongAddiction", event
                .getComponent(), DesktopContainer.class);
        addMenuItem(menu, "Add Relationship to new Entity ...", "img/mDREntity.gif",
                this, "decomposeUsingRelation", event.getComponent(),
                DesktopContainer.class);
        addMenuItem(menu, "Compose entity with ...", "img/mCompEntity.gif", event
                .getComponent(), "removing", this, Item.class);
        // addMenuItem(menu, "Compose with entity", "mCompEntity.gif",
        // event.getComponent(), "composingEntity", this,
        // cz.felk.cvut.erm.ermodeller.Entity.class);
        addMenuItem(menu, "Readjust size", "img/mReadjustSize.gif", this,
                "minimizeSize", event,
                cz.felk.cvut.erm.event.CoordinateEvent.class);
        if (type == UML) {
            addMenuItem(menu, "Adjacent members of PK", "img/mAdjacentPK.gif", this,
                    "collectPKatributes", null,
                    null);
        }
        //		if (type == UML) {
        addMenuItem(menu, "Edit Constraints", "img/mEditConstraints.gif", event.getComponent(),
                "editConstraints", this,
                EntityConstruct.class);

//		}
        return menu;
    }

    /**
     * Call dialog to edit Constraints.
     */
    /*	public void editConstraints() {
            if (constDialog == null)
                constDialog = new ConstraintsDialog(null, (cz.felk.cvut.erm.conceptual.beans.Entity) getModel());
            if (getModel() != null) constDialog.setVisible(true);
        }
    */
    public void minimizeSize(cz.felk.cvut.erm.event.CoordinateEvent e) {
        ResizeRectangle rr = new ResizeRectangle(
                0, 0, 0, 0, ResizePoint.BOTTOM
                | ResizePoint.RIGHT);

        minimizeEntity(new ResizeEvent(getBounds().x, getBounds().y, 0, 0, rr, e.getComponent()));
    }

    /**
     * Creates the new unique key to this object. It includes the model and connection creation.
     *
     * @param left The x coordinate of the left top point of the new unique key.
     * @param top  The y coordinate of the left top point of the new unique key.
     * @return The new created unique key.
     */
    public UniqueKeyConstruct createUniqueKey(int left, int top) {
        try {
            // create model - unique key
            Entity cc = getModel();
            UniqueKey cUq = cc.createUniqueKey();
            // create unique key
            UniqueKeyConstruct uq = new UniqueKeyConstruct(cUq, this, manager, left, top);
            manager.addItem(uq);
            (manager).repaintItem(uq);
            // create the connection to the
            Connection conn = new ConnectionLine(manager, getModel().getSchema(), uq, this);
            ((ConnectionManager) manager).addConnection(conn);
            (manager).repaintItem(conn);
            return uq;
        } catch (Throwable x) {
            new ShowException(null, "Error", x, true);
        }
        return null;
    }

    /**
     * Decompose this entity to this and ent. According to the value of the atribute <code>decomposeAsRelation</code>
     * decompose using relation or strong addiction.
     *
     * @param ent     The new entity.
     * @param manager The desktop.
     */
    protected void decompose(EntityConstruct ent, Manager man) {
        java.awt.Point p = getAbsoluteCenter(ent);
        if (decomposeAsRelation) {
            final NotationType type = model.getSchema().getNotationType();
            try {
                // decompose using relation -- create new relation
                RelationConstruct rel = ((ISchema) man).createRelation(p.x, p.y);
                rel.handleMoveEvent(new MoveEvent(rel.getBounds().x, rel.getBounds().y, -rel.getBounds().width / 2, -rel.getBounds().height / 2, null));
                p = ent.getAbsoluteCenter(rel);
                // create new cardinalities
                CardinalityConstruct car = rel.createCardinality(ent, man, p.x, p.y);
                ((Cardinality) car.getModel())
                        .setArbitrary(true);
                ((Cardinality) car.getModel())
                        .setMultiCardinality(false);
                car.handleMoveEvent(new MoveEvent(car.getBounds().x, car.getBounds().y, -car.getBounds().width / 2, -car.getBounds().height / 2, null));
                if (type != ConceptualConstructItem.CHEN) {
                    car.model.setName(this.model.getName());
                    car.moveCardinality(new ExMovingEvent(p.x, p.y, 0, 0, null, false));
                }
                p = getAbsoluteCenter(rel);
                car = rel.createCardinality(this, man, p.x, p.y);
                ((Cardinality) car.getModel())
                        .setArbitrary(true);
                ((Cardinality) car.getModel())
                        .setMultiCardinality(false);
                car.handleMoveEvent(new MoveEvent(car.getBounds().x, car.getBounds().y, -car.getBounds().width / 2, -car.getBounds().height / 2, null));
                if (type != ConceptualConstructItem.CHEN) {
                    car.model.setName(ent.model.getName());
                    car.moveCardinality(new ExMovingEvent(p.x, p.y, 0, 0, null, false));
                }
            } catch (Throwable x) {
                new ShowException(null, "Error", x, true);
            }
        } else {
            // decompose using strong addiction
            // create connection
            p = this.getCenter(ent);
//			StrongAddiction sa = StrongAddiction.createStrongAddiction(this, ent, man, p.x, p.y);
            StrongAddiction sa = StrongAddiction.createStrongAddiction(this, ent, ent.getManager(), p.x, p.y);
            sa.moveStrongAddiction(new ExMovingEvent(sa.getBounds().x, sa.getBounds().y, 0, 0, null, false));
        }
    }

    /**
     * Invoked when selected menu item Decompose using relation. Set the appropriate flag and invokes the container method
     * addingEntity.
     *
     * @param con Where this all lies.
     */
    public void decomposeUsingRelation(DesktopContainer con) {
        decomposeAsRelation = true;
        con.addingEntity(this);
    }

    /**
     * Invoked when selected menu item Decompose using strong addiction. Set the appropriate flag and invokes the container
     * method addingEntity.
     *
     * @param con Where this all lies.
     */
    public void decomposeUsingStrongAddiction(DesktopContainer con) {
        decomposeAsRelation = false;
        con.addingEntity(this);
    }

    /**
     * Returns the model entity from the Ale� Kopeck� work.
     *
     * @see ConceptualConstructObject#getModel()
     */
    public Entity getModel() {
        return model;
    }

    /**
     * Returns the primary unique key.
     *
     * @return The primary unique key.
     */
    public UniqueKeyConstruct getPrimary() {
        return primary;
    }

    /**
     * Returns all unique keys of this construct
     */
    public Vector<UniqueKeyConstruct> getUniqueKeys() {
        Vector<UniqueKeyConstruct> v = new java.util.Vector<UniqueKeyConstruct>();
        UniqueKeyConstruct uk;
        for (int i = connections.size() - 1; i >= 0; i--) {
            if ((uk = (UniqueKeyConstruct) ((Connection) connections.get(i))
                    .isConnectedTo(UniqueKeyConstruct.class)) != null)
                v.add(uk);
        }
        return v;
    }

    /**
     * Returns all Primary Key members of this entity
     */
    public Vector<AttributeConstruct> getPKmembers() {
        return PKmembers;
    }

    /**
     * Handle event when some other object is dragged over. Can work only with entity (creating ISA relation or compacting
     * two entities), relatio (creating new cardinality) and unique (adding (removing) as the strong addiction parent.
     */
    public void handleDragOverEvent(DragOverEvent event) {
        if (selected && event.getAdd())
            return;
        Item item = event.getItem();
        if (item instanceof EntityConstruct) {
            if (event.getAdd()) {
                if ((item != this)
                        && ((ISAChilds == null) || (ISAChilds.indexOf(item) == -1))) {
                    event.getComponent().setCursor(
                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
            } else {
                // if (areCompactable((Entity) item)) {
                if (item != this) {
                    event.getComponent().setCursor(
                            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    return;
                }
            }
        }
        if (item instanceof RelationConstruct) {
            if (event.getAdd()) {
                event.getComponent().setCursor(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }
        }
        if (item instanceof UniqueKeyConstruct) {
            UniqueKeyConstruct uk = (UniqueKeyConstruct) item;
            if (event.getAdd() && uk.getPrimary()
                    && (uk.connectionTo(this) == null)) {
                event.getComponent().setCursor(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }
        }
        if (item instanceof StrongAddiction) {
            StrongAddiction sa = (StrongAddiction) item;
            if ((event.getAdd()) && (sa.getEntity() != this)) {
                event.getComponent().setCursor(
                        Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                return;
            }
        }
        super.handleDragOverEvent(event);
    }

    public void handleAddIdentificationDependencyEvent(cz.felk.cvut.erm.event.AddIdentificationDependencyEvent event) {
        float scale = getManager().getScale();
        Item item = event.getItem();
        java.awt.Point p = getAbsoluteCenter((EntityConstruct) item);
        StrongAddiction sa = StrongAddiction.createStrongAddiction(this, (EntityConstruct) item, ((EntityConstruct) item).getManager(), (int) (p.x * scale), (int) (p.y * scale));
        sa.moveStrongAddiction(new ExMovingEvent((int) (p.x * scale), (int) (p.y * scale), 0, 0, null, false));
    }

    public void handleAddRelWithConnsEvent(cz.felk.cvut.erm.event.AddRelWithConnsEvent event) throws ItemNotInsideManagerException {
        if (selected && event.getAdd())
            return;
        final NotationType type = model.getSchema().getNotationType();
        float scale = getManager().getScale();
        Item item = event.getItem();
        java.awt.Point p = getAbsoluteCenter((EntityConstruct) item);
        CardinalityConstruct car;
        if (item instanceof EntityConstruct) {
            if (item != this) {
                Manager man = ((EntityConstruct) item).getManager();
                Manager man2 = getManager();
                RelationConstruct rel = RelationConstruct.createRelation(model.getSchema(), EntManager, (int) (p.x * scale), (int) (p.y * scale));
                rel.handleMoveEvent(new MoveEvent(rel.getBounds().x, rel.getBounds().y, -rel.getBounds().width / 2, -rel.getBounds().height / 2, null));
                p = ((EntityConstruct) item).getAbsoluteCenter(rel);
                car = rel.createCardinality((EntityConstruct) item, man, (int) (p.x * scale), (int) (p.y * scale));
                car.handleMoveEvent(new MoveEvent(car.getBounds().x, car.getBounds().y, -car.getBounds().width / 2, -car.getBounds().height / 2, null));
                if (type != ConceptualConstructItem.CHEN) {
                    car.model.setName(this.model.getName());
                    car.moveCardinality(new ExMovingEvent((int) (p.x * scale), (int) (p.y * scale), 0, 0, null, false));
                }
                p = getAbsoluteCenter(rel);
                car = rel.createCardinality(this, man2, (int) (p.x * scale), (int) (p.y * scale));
                car.handleMoveEvent(new MoveEvent(car.getBounds().x, car.getBounds().y, -car.getBounds().width / 2, -car.getBounds().height / 2, null));
                if (type != ConceptualConstructItem.CHEN) {
                    car.model.setName(((EntityConstruct) item).model.getName());
                    car.moveCardinality(new ExMovingEvent((int) (p.x * scale), (int) (p.y * scale), 0, 0, null, false));
                }
            } else {
                Manager man = getManager();
                RelationConstruct rel = RelationConstruct.createRelation(model.getSchema(), man, (int) ((p.x + SELFRELATIONDISTANCE) * scale), (int) ((p.y + SELFRELATIONDISTANCE) * scale));
                if (type == ConceptualConstructItem.CHEN) {
                    car = rel.createCardinality((EntityConstruct) item, man, (int) ((p.x + SELFRELATIONDISTANCE) * scale) + rel.getBounds().width / 2, (int) (p.y * scale) + item.getBounds().height / 2);
                    car.move(-car.getBounds().width / 2, -car.getBounds().height / 2, true);
                    man.repaintItem(car);
                } else
                    car = rel.createCardinality((EntityConstruct) item, man, (int) (p.x * scale), (int) (p.y * scale));
                if (type == ConceptualConstructItem.CHEN) {
                    car = rel.createCardinality(this, man, (int) (p.x * scale) + item.getBounds().width / 2, (int) ((p.y + SELFRELATIONDISTANCE) * scale) + rel.getBounds().height / 2);
                    car.move(-car.getBounds().width / 2, -car.getBounds().height / 2, true);
                    man.repaintItem(car);
                } else
                    car = rel.createCardinality(this, man, (int) (p.x * scale), (int) (p.y * scale));
            }
        }
    }

    public void handleAddConnectionEvent(cz.felk.cvut.erm.event.AddConnectionEvent event) {
        if (selected && event.getAdd())
            return;
        float scale = getManager().getScale();
        Item item = event.getItem();
        final NotationType type = model.getSchema().getNotationType();
        if (item instanceof RelationConstruct) {
            RelationConstruct rel = (RelationConstruct) item;
            String name = "";
            if (type != ConceptualConstructItem.CHEN) {
                CardinalityConstruct car1;
                for (Connection c : rel.getConnections()) {
                    if (c.getOne() instanceof CardinalityConstruct) {
                        car1 = ((CardinalityConstruct) c.getOne());
                        name = (car1.getEntity().getModel()).getName();
                    }
                    if (c.getTwo() instanceof CardinalityConstruct) {
                        car1 = ((CardinalityConstruct) c.getTwo());
                        name = (car1.getEntity().getModel()).getName();
                    }
                }
            }
            java.awt.Point p = ((RelationConstruct) item).getAbsoluteCenter(this);
            CardinalityConstruct car = ((RelationConstruct) item).createCardinality(this, ((RelationConstruct) item).getManager(), (int) (p.x * scale), (int) (p.y * scale));
            car.handleMoveEvent(new MoveEvent(car.getBounds().x, car.getBounds().y, -car.getBounds().width / 2, -car.getBounds().height / 2, null));
            if (type != ConceptualConstructItem.CHEN) {
                car.model.setName(this.model.getName());
                if (name.length() > 0) car.model.setName(name);
                car.moveCardinality(new ExMovingEvent((int) (p.x * scale), (int) (p.y * scale), 0, 0, null, false));
            }
        }
    }

    public void handleAddAsISAChildEvent(cz.felk.cvut.erm.event.AddAsISAChildEvent event) {
        Item item = event.getItem();
        if (item instanceof EntityConstruct) {
            final EntityConstruct ent = (EntityConstruct) item;
            if ((item != this) && ((ISAChilds == null) || (ISAChilds.indexOf(ent) == -1))) {
                addISAChild(ent, event);
            }
        }
    }

    /**
     * Handle event when some other object is dragged over. Can work only with entity (creating ISA relation or compacting
     * two entities), relatio (creating new cardinality) and unique (adding (removing) as the strong addiction parent.
     */
    public void handleDropAboveEvent(DropAboveEvent event) {
        if (selected && event.getAdd())
            return;
        Item item = event.getItem();
        final NotationType type = model.getSchema().getNotationType();
        if (item instanceof EntityConstruct) {
            // entity over
            if (event.getAdd()) {
                // add as ISA child
                if ((item != this)
                        && ((ISAChilds == null) || (ISAChilds.indexOf(item) == -1))) {//TODO podezrele volani
                    addISAChild((EntityConstruct) item, event);
                    event.setDropped(true);
                }
            } else {
                // try to compact
                // if (areCompactable((Entity) item)) {
                // compact((Entity) item, event);
                // }
                // else {
                composeEntity((EntityConstruct) item, event);
                // }
            }
        }
        if (item instanceof RelationConstruct) {
            // relation over --> creates new cardinality
            if (event.getAdd()) {
                RelationConstruct rel = (RelationConstruct) item;
                try {
                    if (type == CHEN)
                        ((DesktopContainer) event.getComponent()).addingCardinality(new CardinalityPair(this, rel));
                    else {
                        java.awt.Point p = ((RelationConstruct) item).getAbsoluteCenter(this);
                        CardinalityConstruct car = ((RelationConstruct) item).createCardinality(this, ((RelationConstruct) item).getManager(), p.x, p.y);
                        car.handleMoveEvent(new MoveEvent(car.getBounds().x, car.getBounds().y, -car.getBounds().width / 2, -car.getBounds().height / 2, null));
                        car.model.setName(this.model.getName());
                        car.moveCardinality(new ExMovingEvent(p.x, p.y, 0, 0, null, false));
                    }
                    event.setDropped(true);
                    return;
                } catch (Throwable x) {
                    new ShowException(null, "Error", x, true);
                }
            }
        }
        if (item instanceof UniqueKeyConstruct) {
            // add strong addiction
            if (event.getAdd()) {
                UniqueKeyConstruct uk = (UniqueKeyConstruct) item;
                // add as strong addiction parent
                if (uk.getPrimary() && (uk.connectionTo(this) == null)) {
                    ((DesktopContainer) event.getComponent())
                            .addingStrongAddiction(new StrongAddictionPair(
                                    this, uk));
                    event.setDropped(true);
                }
            }
        }
        if (item instanceof StrongAddiction) {
            // reconnect strong addiction
            if (event.getAdd()) {
                StrongAddiction sa = (StrongAddiction) item;
                // add as strong addiction parent
                if (sa.getEntity() != this) {
                    sa.reconnectStrongAddictionParent(this);
                    event.setDropped(true);
                }
            }
        }
        super.handleDropAboveEvent(event);
    }

    /**
     * Same as inhereted but when has ISA parent this event is posted to it.
     */
    public void handleExMoveEvent(ExMoveEvent event) {
        // do drop above event and pass it
        DropAboveEvent ev = null;
        int dx = event.getDx(), dy = event.getDy();
        if (!event.getMove()) {
            ev = new DropAboveEvent(event, this);
            manager.fallAndHandleEvent(event.getX() + event.getDx(), event
                    .getY()
                    + event.getDy(), ev);
            if (ev.getDropped() && hRect != null) {
                event.setDx(rect[0][0] - hRect[0][0]);
                event.setDy(rect[1][0] - hRect[1][0]);
            }
        }
        if (!setAsISAChild && ISAParent != null) {
            // pas to ISA parent
            ISAParent.handleExMoveEvent(event);
        } else {
            // standard handling
            setAsISAChild = false;
            ((DGroupTool) manager).handleExMoveEvent(event);
            paintedFast = false;
        }
        // restore the dx and dy
        if ((ev != null) && (ev.getDropped())) {
            event.setDx(dx);
            event.setDy(dy);
        }
        //repaint strong addiction line
        for (Connection c : connections) {
            StrongAddiction sa = null;
            if (c.getOne() instanceof StrongAddiction)
                sa = (StrongAddiction) (c.getOne());
            if (c.getTwo() instanceof StrongAddiction)
                sa = (StrongAddiction) (c.getTwo());
            if (sa != null) {
                java.awt.Rectangle r = sa.getBounds();
                sa.getManager().repaintRectangle(r.x, r.y, r.width, r.height);
            }
        }

    }

    /**
     * Same as inhereted but when has ISA parent this event is posted to it.
     */
    public void handleExMovingEvent(ExMovingEvent event) {
        if (ISAParent != null) {
            // pass to ISA parent
            ISAParent.handleExMovingEvent(event);
        } else {
            // standard handling
            paintedFast = true;
            ((DGroupTool) manager).handleExMovingEvent(event);
        }
        if (!event.getMove())
            manager.fallAndHandleEvent(event.getX() + event.getDx(), event
                    .getY()
                    + event.getDy(), new DragOverEvent(event, this));
        else
            event.getComponent().setCursor(
                    Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    /**
     * Only pass the evnt to its manager (DGroup instance).
     *
     * @see DGroupTool#handleMoveEvent(cz.felk.cvut.erm.event.MoveEvent)
     */
    public void handleMoveEvent(cz.felk.cvut.erm.event.MoveEvent event) {
        ((DGroupTool) manager).handleMoveEvent(event);
        paintedFast = false;
    }

    /**
     * Only pass the evnt to its manager (DGroup instance).
     *
     * @see DGroupTool#handleMovingEvent(cz.felk.cvut.erm.event.MovingEvent)
     */
    public void handleMovingEvent(cz.felk.cvut.erm.event.MovingEvent event) {
        paintedFast = true;
        ((DGroupTool) manager).handleMovingEvent(event);
    }

    /**
     * If has ISA parent removes from it as ISA child, remove strong addictions where is parent, dispose own strong
     * addictions and ISA childs and finaly calls inherited handling.
     *
     * @see #disposeChilds(cz.felk.cvut.erm.event.RemoveEvent)
     * @see #disposeStrongAddiction()
     */
    public void handleRemoveEvent(cz.felk.cvut.erm.event.RemoveEvent event) {
        try {
            if (ISAParent != null) {
                ISAParent.removeISAChild(this, event);
            }
            removeAllStrongAddictionChilds(event);
            removeChilds(event);
            removeAllStrongAddictionParents(event);
            model.getSchema().disposeEntity(model);
            super.handleRemoveEvent(event);
        } catch (Throwable x) {
            new ShowException(null, "Error", x, true);
        }
    }

    /**
     * Calls the resizeEvent methods and then move all ISA childs (moveChilds).
     *
     * @see resizeEntity( cz.felk.cvut.erm.event.ResizeEvent)
     * @see moveChilds( cz.felk.cvut.erm.event.CoordinateEvent)
     */
    public void handleResizeEvent(cz.felk.cvut.erm.event.ResizeEvent event) {
        //java.awt.Rectangle rOld = getBounds();
        //System.out.println("eventDX " + event.getDx());
        resizeEntity(event);
        //java.awt.Rectangle rNew = getBounds();
        //moveCardinalities(event, rOld, rNew);
        moveChilds(event);
        recalculatePositionsOfAtributes();
    }

    public void moveCardinalities() {
        /* Move cardinalities to its Entities*/
        CardinalityConstruct car;

        for (Connection c : getConnections()) {
            if (c.getOne() instanceof CardinalityConstruct) {
                car = ((CardinalityConstruct) c.getOne());
                car.moveCardinality(new ExMovingEvent(car.getBounds().x, car.getBounds().y, 0, 0, null, false));
            }
            if (c.getTwo() instanceof CardinalityConstruct) {
                car = ((CardinalityConstruct) c.getTwo());
                car.moveCardinality(new ExMovingEvent(car.getBounds().x, car.getBounds().y, 0, 0, null, false));
            }
        }
    }

    public void moveStrongAddictions() {
        /* Move cardinalities to its Entities*/
        StrongAddiction sa;
        for (Connection c : getConnections()) {
            if (c.getOne() instanceof StrongAddiction) {
                sa = ((StrongAddiction) c.getOne());
                sa.moveStrongAddiction(new ExMovingEvent(sa.getBounds().x, sa.getBounds().y, 0, 0, null, false));
            }
            if (c.getTwo() instanceof StrongAddiction) {
                sa = ((StrongAddiction) c.getTwo());
                sa.moveStrongAddiction(new ExMovingEvent(sa.getBounds().x, sa.getBounds().y, 0, 0, null, false));
            }
        }
    }

    /*
        public void moveCardinalities(cz.felk.cvut.erm.event.ResizeEvent event, java.awt.Rectangle rOld, java.awt.Rectangle rNew) {
            java.util.Enumeration e = connections.elements();
            Cardinality car;
            Point carCenter;
            int direction;
            while (e.hasMoreElements()) {
                Connection c = ((Connection) e.nextElement());
                if (c.getOne() instanceof Cardinality) car = ((Cardinality) c.getOne());
                else if (c.getTwo() instanceof Cardinality) car = ((Cardinality) c.getTwo());
                    else break;
                carCenter = car.getCenter();
                direction = event.getResizeRect().direction;
                //System.out.println("Card move "+event.getResizeRect().direction);
                try {
                    if (carCenter.x < rOld.getBounds().x &&
                            (direction == ResizePoint.LEFT ||
                             direction == ResizePoint.LEFT + ResizePoint.TOP ||
                             direction == ResizePoint.LEFT + ResizePoint.BOTTOM))
    //					car.moveCardinality(new ExMovingEvent(car.getBounds().x, car.getBounds().y, rNew.x - rOld.x, 0, null, false));
                        car.move(rNew.x - rOld.x, 0,true);
                    if (carCenter.x > (rOld.getBounds().x + rOld.getBounds().width) &&
                            (direction == ResizePoint.RIGHT ||
                             direction == ResizePoint.RIGHT + ResizePoint.TOP ||
                             direction == ResizePoint.RIGHT + ResizePoint.BOTTOM))
                        car.move(rNew.width - rOld.width, 0,true);
                    if (carCenter.y < rOld.getBounds().y &&
                            (direction == ResizePoint.TOP ||
                             direction == ResizePoint.TOP + ResizePoint.LEFT ||
                             direction == ResizePoint.TOP + ResizePoint.RIGHT))
                        car.move(0, rNew.y - rOld.y, true);
                    if (carCenter.y > (rOld.getBounds().y + rOld.getBounds().height) &&
                            (direction == ResizePoint.BOTTOM ||
                             direction == ResizePoint.BOTTOM + ResizePoint.LEFT ||
                             direction == ResizePoint.BOTTOM + ResizePoint.RIGHT))
                        car.move(0, rNew.height - rOld.height, true);
                } catch (ItemNotInsideManagerException e1) {
                    e1.printStackTrace();
                }

            }
        }
    */
    /**
     * Calls the resizingEvent methods and then move all ISA childs (movingChilds).
     *
     * @see resizingEntity( cz.felk.cvut.erm.event.ResizingEvent)
     * @see movingChilds( cz.felk.cvut.erm.event.CoordinateEvent)
     */
    public void handleResizingEvent(cz.felk.cvut.erm.event.ResizingEvent event) {
        resizingEntity(event);
//		moveCardinalities(event);
        movingChilds(event);
    }

    /**
     * Reconnects all strong addiction to the entity ent.
     *
     * @param ent The new strong addiction parent.
     */
    protected void changeAllStrongAddictions(EntityConstruct ent) {
        StrongAddiction sa;
        for (int i = connections.size() - 1; i >= 0; i--) {
            if ((sa = (StrongAddiction) connections.get(i)
                    .isConnectedTo(StrongAddiction.class)) != null) {
                if (sa.getEntity() != ent)
                    sa.reconnectStrongAddictionParent(ent);
            }
        }
    }

    /**
     * Counts the move event for all ISA childs and then moves it.
     *
     * @param event Needful to create move event.
     * @see #countMove(cz.felk.cvut.erm.event.CoordinateEvent, EntityConstruct ,int)
     * @see #handleMoveEvent(cz.felk.cvut.erm.event.MoveEvent)
     */
    protected void moveChilds(cz.felk.cvut.erm.event.CoordinateEvent event) {
        if ((ISAChilds != null) && (ISAChilds.size() > 0)) {
            cz.felk.cvut.erm.event.MoveEvent mev;
            int max = ISAChilds.size() - 1;
            for (int i = 0; i <= max; i++) {
                EntityConstruct ent = ISAChilds.get(i);
                mev = countMove(event, ent, i - 1);
                ent.handleMoveEvent(mev);
            }
        }
    }

    /**
     * Counts the move event for all ISA childs and then moves it.
     *
     * @param event Needful to create move event.
     * @see #countMove(cz.felk.cvut.erm.event.CoordinateEvent, EntityConstruct ,int)
     * @see #handleMoveEvent(cz.felk.cvut.erm.event.MoveEvent)
     */
    protected void moveMinChilds(cz.felk.cvut.erm.event.CoordinateEvent event) {
        if ((ISAChilds != null) && (ISAChilds.size() > 0)) {
            cz.felk.cvut.erm.event.MoveEvent mev;
            int max = ISAChilds.size() - 1;
            for (int i = 0; i <= max; i++) {
                EntityConstruct ent = ISAChilds.get(i);
                mev = countMove(event, ent, i - 1);
                mev.setDx(0);
                ent.handleMoveEvent(mev);
            }
        }
    }

    /**
     * Counts the move event for all ISA childs and then moves it.
     *
     * @param event Needful to create move event.
     * @see #countMove(cz.felk.cvut.erm.event.CoordinateEvent, EntityConstruct ,int)
     * @see #handleMovingEvent(cz.felk.cvut.erm.event.MovingEvent)
     */
    protected void movingChilds(cz.felk.cvut.erm.event.CoordinateEvent event) {
        if ((ISAChilds != null) && (ISAChilds.size() > 0)) {
            cz.felk.cvut.erm.event.MovingEvent mev;
            int max = ISAChilds.size() - 1;
            for (int i = 0; i <= max; i++) {
                EntityConstruct ent = ISAChilds.get(i);
                mev = countMove(event, ent, i - 1);
                ent.handleMovingEvent(mev);
            }
        }
    }

    /**
     * Paints the entity.
     */
    public void paint(java.awt.Graphics g) {
        int y = 0;
        FontMetrics fm = g.getFontMetrics();
        String name = model.getName();
        final NotationType type = model.getSchema().getNotationType();
        java.awt.Rectangle r = getBounds();
        final Stroke stroke = updateStrokeWithAliasing(g);
        //setDefaultSize(e, fm);

        int diff = (int) (EntityConstruct.getDIFFERENCE() / (getManager()).getScale());
        if (isSelected())
            g.setColor(getSelectedBackgroundColor());
        else
            g.setColor(getEntityBackgroundColor());

        g.fillRect(r.x, r.y, r.width, r.height);
        g.setColor(getEntityForegroundColor());
        g.drawRect(r.x, r.y, r.width, r.height);
        switch (type) {
            case CHEN:
                y = r.y + (r.height + fm.getAscent()) / 2;
                if ((ISAChilds != null) && (ISAChilds.size() > 0))
                    y = r.y + 2 * fm.getAscent();
                break;
            case BINARY:
                y = r.y + 5 + fm.getAscent();
                g.drawLine(r.x, r.y + 2 * fm.getAscent(), r.x + r.width, r.y + 2
                        * fm.getAscent());
                if (PKmembers.size() > 0 && getLevelOfDetails() != Schema.LOD_LOW) {
                    g.drawString("*", r.x + 3, r.y + 42);
                    g.drawString(" # (", r.x + 5, r.y + 40);
                    g.drawString(")", r.x + 5 + PKwidth, r.y + 40);
                }
                break;
            case UML:
                y = r.y + 5 + fm.getAscent();
                g.drawLine(r.x, r.y + 2 * fm.getAscent(), r.x + r.width, r.y + 2
                        * fm.getAscent());
                if (getLevelOfDetails() == Schema.LOD_FULL) {
                    int height = r.y + 2 * fm.getAscent() + attribs.size() * 20 + 8;
                    if (ISAChilds != null) {
                        for (int i = ISAChilds.size() - 1; i > -1; i--) {
                            int[][] rr = (ISAChilds.get(i)).getRect();
                            height += (rr[1][1] - rr[1][0]) + 2 * DIFFERENCE;
                        }
                    }
                    g.drawLine(r.x, height, r.x + r.width, height);
                    if (model.getConstraints().length() > 0) {
                        g.drawString("Constraints", r.x + 5, height + fm.getAscent());
                        g.drawString("{" + model.getConstraints() + "}", r.x + 8, height + 2 * fm.getAscent());
                    }
                }
                break;
            default:
                assert false;
        }
        if (isStrongAddictionChild && type == CHEN) {
            g.drawRect(r.x + diff, r.y + diff, r.width - 2 * diff, r.height - 2 * diff);
        }
        g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, y);
        updateBackupStroke(g, stroke);
    }

    /**
     * Prints the entity.
     */
    public void print(java.awt.Graphics g) {
        int y = 0;
        FontMetrics fm = g.getFontMetrics();
        String name = model.getName();
        final NotationType type = model.getSchema().getNotationType();
        java.awt.Rectangle r = getBounds();
        int diff = (int) (EntityConstruct.getDIFFERENCE() / (getManager()).getScale());
        g.setColor(getEntityForegroundColor());
        g.drawRect(r.x, r.y, r.width, r.height);
        switch (type) {
            case CHEN:
                y = r.y + (r.height + fm.getAscent()) / 2;
                if ((ISAChilds != null) && (ISAChilds.size() > 0))
                    y = r.y + 2 * fm.getAscent();
                break;
            case BINARY:
                y = r.y + 5 + fm.getAscent();
                g.drawLine(r.x, r.y + 2 * fm.getAscent(), r.x + r.width, r.y + 2
                        * fm.getAscent());
                if (PKmembers.size() > 0 && getLevelOfDetails() != Schema.LOD_LOW) {
                    g.drawString("*", r.x + 3, r.y + 42);
                    g.drawString(" # (", r.x + 5, r.y + 40);
                    g.drawString(")", r.x + 5 + PKwidth, r.y + 40);
                }
                break;
            case UML:
                y = r.y + 5 + fm.getAscent();
                g.drawLine(r.x, r.y + 2 * fm.getAscent(), r.x + r.width, r.y + 2
                        * fm.getAscent());
                if (getLevelOfDetails() == Schema.LOD_FULL) {
                    int height = r.y + 2 * fm.getAscent() + attribs.size() * 20 + 8;
                    if (ISAChilds != null) {
                        for (int i = ISAChilds.size() - 1; i > -1; i--) {
                            int[][] rr = (ISAChilds.get(i)).getRect();
                            height += (rr[1][1] - rr[1][0]) + 2 * DIFFERENCE;
                        }
                    }
                    g.drawLine(r.x, height, r.x + r.width, height);
                    if (model.getConstraints().length() > 0) {
                        g.drawString("Constraints", r.x + 5, height + fm.getAscent());
                        g.drawString("{" + model.getConstraints() + "}", r.x + 8, height + 2 * fm.getAscent());
                    }
                }
                break;
        }
        if (isStrongAddictionChild && type == NotationType.CHEN) {
            g.drawRect(r.x + diff, r.y + diff, r.width - 2 * diff, r.height - 2 * diff);
        }
        g.drawString(name, r.x + (r.width - fm.stringWidth(name)) / 2, y);
        //r = null;
    }

    /**
     * Invoked when some model's property change. Catch changes of the <b>name</b> and <b>strongAddictionParents</b>
     * property.
     *
     * @see ConceptualConstructItem#propertyChange(java.beans.PropertyChangeEvent);
     */
    public void propertyChange(java.beans.PropertyChangeEvent e) {
        (getModel())
                .setChanged(true);
        java.awt.Rectangle r = getBounds();
        if ("name".equals(e.getPropertyName()) ||
                "constraints".equals(e.getPropertyName())) {
            ResizeRectangle rr = new ResizeRectangle(
                    0, 0, 0, 0, ResizePoint.BOTTOM
                    | ResizePoint.RIGHT);
            this.resizeEntity(new ResizeEvent(getBounds().x, getBounds().y, 0, 0, rr, null));
            (manager).repaintRectangle(r.x,
                    r.y, r.width, r.height);
            return;
        }
        if ("strongAddictionsParents".equals(e.getPropertyName())) {
            java.util.Vector v = (java.util.Vector) e.getNewValue();
            if (isStrongAddictionChild && (v.size() == 0)) {
                isStrongAddictionChild = false;
                (manager).repaintRectangle(
                        r.x, r.y, r.width, r.height);
                return;
            }
            if (!isStrongAddictionChild && (v.size() != 0)) {
                isStrongAddictionChild = true;
                (manager).repaintRectangle(
                        r.x, r.y, r.width, r.height);
            }
        }
    }

    /**
     * This method was created by Jiri Mares
     *
     * @param s java.io.ObjectInputStream
     * @throws java.io.IOException The exception description.
     * @throws java.lang.ClassNotFoundException
     *                             The exception description.
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        // set yourself as property CHENge listener
        try {
            model.addPropertyChangeListener(this);
        } catch (NullPointerException e) {
            e.printStackTrace(); //LV
        }
    }

    /**
     * Recalculate positions of atributes
     */
    public void recalculatePositionsOfAtributes() {
        int PKlength = 0, ALength, dx, dy, position, highestPKposition = 0, lowestNonPKposition = getAtributes().size();
        int count = getAtributes().size();
        int PKmembersCount = getPKmembers().size();
        AttributeConstruct a, pk;
        final NotationType type = model.getSchema().getNotationType();
        FontMetrics fm = ((FontManager) manager).getReferentFontMetrics();
        if (type == BINARY) {
            for (int i = 0; i < count; i++) {
                a = (getAtributes().get(i));
                if (PKmembers.contains(a)) {
                    a.setPKfirst(a.getPosition() == 1);
                    ALength = 0;
                    position = a.getPosition();
                    if (position > highestPKposition) highestPKposition = position;
                    for (Object PKmember : PKmembers) {
                        pk = (AttributeConstruct) PKmember;
                        if (pk.getPosition() < position)
                            ALength += pk.getBounds().width;
                    }
                    dx = getBounds().x + 5 - a.getBounds().x + ALength;
                    dy = getBounds().y + 30 - a.getBounds().y;
                    PKlength += a.getBounds().width;
                } else {
                    position = a.getPosition();
                    if (position < lowestNonPKposition) lowestNonPKposition = position;
                    dx = getBounds().x + 5 - a.getBounds().x;
                    if (PKmembersCount == 0)
                        dy = getBounds().y + 10 + position * 20 - a.getBounds().y;
                    else
                        dy = getBounds().y + 10 + (position - (PKmembers.size() - 1)) * 20 - a.getBounds().y;
                }
                try {
                    a.move(dx, dy, true);
                } catch (ItemNotInsideManagerException e1) {
                    e1.printStackTrace();
                }
            }
            //((cz.felk.cvut.erm.event.interfaces.PaintableManager) manager).repaintItem(this);
            PKwidth = PKlength;
            /* adjust size of entity */
            ResizeRectangle rr = new ResizeRectangle(
                    0, 0, 0, 0, ResizePoint.BOTTOM
                    | ResizePoint.RIGHT);
//				java.awt.Rectangle rOld = getBounds();
            this.resizeEntity(new ResizeEvent(getBounds().x, getBounds().y, 0, 0, rr, null));
            moveChilds(new ResizeEvent(getBounds().x, getBounds().y, 0, 0, rr, null));
//				java.awt.Rectangle rNew = getBounds();
//				moveCardinalities(new ResizeEvent(getBounds().x, getBounds().y, 0, 0, rr, null), rOld, rNew);

            if (lowestNonPKposition < highestPKposition) {
                //SESYPAT
                //collectPKatributes();
            }
        }
        if (type == UML) {
            for (int i = 0; i < count; i++) {
                a = (getAtributes().get(i));
                dx = getBounds().x + 5 - a.getBounds().x;
                dy = getBounds().y + 10 + a.getPosition() * 20 - a.getBounds().y;
                try {
                    a.move(dx, dy, true);
                } catch (ItemNotInsideManagerException ex) {
                    throw new RuntimeException(ex);
                }
            }
            ResizeRectangle rr = new ResizeRectangle(
                    0, 0, 0, 0, ResizePoint.BOTTOM
                    | ResizePoint.RIGHT);
            this.resizeEntity(new ResizeEvent(getBounds().x, getBounds().y, 0, 0, rr, null));
            moveChilds(new ResizeEvent(getBounds().x, getBounds().y, 0, 0, rr, null));
        }
        //((cz.felk.cvut.erm.event.interfaces.PaintableManager) manager).repaintItem(this);
        getManager().repaintItem(this);
    }

    public void collectPKatributes() {
        int count = getAtributes().size();
        Vector<AttributeConstruct> pks = new Vector<AttributeConstruct>();
        AttributeConstruct a;
        for (int i = count; i > 0; i--) {
            for (int j = 0; j < count; j++) {
                a = (getAtributes().get(j));
                if (a.getPosition() == i) {
                    if (a.isPrimary()) pks.addElement(a);
                    break;
                }
            }
        }
        for (Object pk : pks) {
            ((AttributeConstruct) pk).moveTop();
        }
    }

    /**
     * Moves all atributes from <code>this</code> to the new owner - <code>cc</code>. Each atribute is moved by
     * <code>moveAtribute</code>.
     *
     * @param cc The new owner of all atributes.
     * @see #moveAtribute(AttributeConstruct)
     */
    protected void reconnectAllAtributes(ConceptualConstructItem cc) {
        UniqueKeyConstruct uk;
        RemoveEvent ev = new RemoveEvent(0, 0, null);
        for (int i = connections.size() - 1; i >= 0; i--) {
            if ((uk = (UniqueKeyConstruct) (connections.get(i))
                    .isConnectedTo(UniqueKeyConstruct.class)) != null) {
                uk.handleRemoveEvent(ev);
            }
        }
        super.reconnectAllAtributes(cc);
    }

    /**
     * Reconents all ISA childs to the specified entity.
     */
    protected void reconnectChilds(EntityConstruct to, CoordinateEvent event) {
        EntityConstruct ent;

        if (ISAChilds != null) {
            Vector<EntityConstruct> v = new java.util.Vector<EntityConstruct>(ISAChilds);
            for (Object aV : v) {
                ent = (EntityConstruct) aV;
                removeISAChild(ent, event);
                to.addISAChild(ent, event);
            }
        }
    }

    /**
     * If has ISA parent removes from it as ISA child, remove strong addictions where is parent, dispose own strong
     * addictions and ISA childs and finaly calls inherited handling.
     *
     * @see #disposeChilds(cz.felk.cvut.erm.event.RemoveEvent)
     * @see #disposeStrongAddiction()
     */
    protected void removeAllStrongAddictionChilds(
            cz.felk.cvut.erm.event.RemoveEvent event) {
        StrongAddiction sa;
        for (int i = connections.size() - 1; i >= 0; i--)
            if ((sa = (StrongAddiction) ((connections.get(i))
                    .isConnectedTo(StrongAddiction.class))) != null)
                sa.handleRemoveEvent(event);
    }

    /**
     * Call the same method to the primary key.
     *
     * @see UniqueKeyConstruct#disposeStrongAddiction()
     */
    protected void removeAllStrongAddictionParents(
            cz.felk.cvut.erm.event.RemoveEvent event) {
        if (primary != null)
            primary.removeAllStrongAddiction(event);
    }

    /**
     * Dispose all ISA childs - calls to evryone the disposeCardinalities and disposeStrongAddiction.
     *
     * @param event Need for disposeCardinalities
     * @see #disposeCardinalities(cz.felk.cvut.erm.event.RemoveEvent)
     * @see disposeStrongAddiction()
     */
    protected void removeChilds(cz.felk.cvut.erm.event.RemoveEvent event) {
        if ((ISAChilds != null) && (ISAChilds.size() > 0)) {
            Enumeration<EntityConstruct> e = ISAChilds.elements();
            while (e.hasMoreElements()) {
                EntityConstruct ent = e.nextElement();
                ent.removeCardinalities(event);
                ent.removeAllStrongAddictionParents(event);
            }
        }
    }

    /**
     * Removes ISA child to this entity. Recounts the size and do all needed.
     *
     * @param ent   The added entity.
     * @param event Event to can post remove event.
     */
    public void removeISAChild(EntityConstruct ent, cz.felk.cvut.erm.event.CoordinateEvent event) {
        if (ISAChilds == null)
            return;
        int index = ISAChilds.indexOf(ent);
        if (index == -1)
            return;
        try {
            ent.resetISAParent(((DesktopContainer) event.getComponent()).getDesktop());
            ISAChilds.removeElementAt(index);
            PaintableManager m = (manager);
            moveChilds(event);
            m.repaintItem(this);
        } catch (Throwable x) {
            new ShowException(null, "Error", x, true);
        }
    }

    /**
     * Disposes the existed unique key of this <code>conceptualConstruct</code>.
     * Also disposes the unique key in the model objects.
     *
     * @param uk
     *            The disposed unique key.
     * @exception
     *            <code>cz.omnicom.ermodeller.conceptual.ParameterCannotBeNull</code>
     *            Thrown by model object.
     * @exception <code>cz.omnicom.ermodeller.conceptual.WasNotFound</code>
     *                Thrown by model object.
     */
    /*	public void removeUniqueKey(UniqueKey uk)
                throws ParameterCannotBeNullException, WasNotFoundException,
                IsStrongAddictedException {
            cz.felk.cvut.erm.conceptual.beans.Entity cCc = (cz.felk.cvut.erm.conceptual.beans.Entity) getModel();
            cCc.disposeUniqueKey((cz.felk.cvut.erm.conceptual.beans.UniqueKey) uk
                    .getModel());
        }
    */
    /**
     * Simply call the removeISAChild to my ISA parent.
     *
     * @param event Necessary for the removeISAChild
     * @see #removeISAChild(EntityConstruct , cz.felk.cvut.erm.event.CoordinateEvent)
     */
    public void resetISAParent(cz.felk.cvut.erm.event.CoordinateEvent event) {
        if (ISAParent != null)
            ISAParent.removeISAChild(this, event);
        cz.felk.cvut.erm.event.MoveEvent ev = new cz.felk.cvut.erm.event.MoveEvent(
                event.getX(), event.getY(), 100, 0, event.getComponent());
        handleMoveEvent(ev);
    }

    /**
     * Simply call the removeISAChild to my ISA parent.
     *
     * @param event Necessary for the removeISAChild
     * @see #removeISAChild(EntityConstruct , cz.felk.cvut.erm.event.CoordinateEvent)
     */
    public void removeISAChilds(cz.felk.cvut.erm.event.CoordinateEvent event) {
        if (ISAChilds != null) {
            int max = ISAChilds.size() - 1;
            for (int i = max; i >= 0; i--) {
                EntityConstruct ent = ISAChilds.get(i);
                removeISAChild(ent, event);
                cz.felk.cvut.erm.event.MoveEvent ev = new cz.felk.cvut.erm.event.MoveEvent(
                        event.getX(), event.getY(), 100, 0, event.getComponent());
                ent.handleMoveEvent(ev);
            }
        }
    }

    /**
     * Reset the isa parent in the model and sets the entity group (instance DGroup) to the new manager.
     *
     * @param man Manager where to put the entity manager.
     */
    protected void resetISAParent(Manager man) {
        try {
            model.setISAParent(null);
            ((DGroupTool) manager)
                    .handleRemoveEvent(new cz.felk.cvut.erm.event.RemoveEvent(0, 0,
                            null));
            man.addItem((Item) manager);
            ISAParent = null;
        } catch (Throwable x) {
            new ShowException(null, "Error", x, true);
        }
    }

    /*	public void minimize(int where, boolean recount) throws cz.felk.cvut.erm.event.exceptions.ItemNotInsideManagerException {

        try {
            super.resize(7 - width, 7 - height, (ResizePoint.RIGHT | ResizePoint.BOTTOM), true);
        } catch (ItemNotInsideManagerException e) {
            e.printStackTrace();
        }
    */
    /**
     * Do real resize of the entity. Calls countMinSize to tests the resize requirement, if has ISA parent the calls its
     * resizeParent method, then resize yourself and again if there is ISA parent then calls moveChilds.
     *
     * @see #countMinSize(cz.felk.cvut.erm.event.ResizeEvent)
     * @see #resizeParent(cz.felk.cvut.erm.event.ResizeEvent, EntityConstruct)
     * @see #moveChilds(cz.felk.cvut.erm.event.CoordinateEvent)
     */
    public void resizeEntity(cz.felk.cvut.erm.event.ResizeEvent event) {
        countMinSize(event);
        if (ISAParent != null) {
            ISAParent.resizeParent(event, this);
        }
        super.handleResizeEvent(event);
        moveCardinalities();
        moveStrongAddictions();
        if (ISAParent != null) {
            ISAParent.moveChilds(event);
        }
    }

    /**
     * Do real minimize of the entity. Calls countMinSize to tests the resize requirement, if has ISA childs the calls its
     * minimizeChilds method, then minimize yourself and again if there is ISA parent then calls moveChilds.
     *
     * @see #countMinSize(cz.felk.cvut.erm.event.ResizeEvent)
     * @see #minimizeChilds(cz.felk.cvut.erm.event.ResizeEvent, EntityConstruct)
     * @see #moveChilds(cz.felk.cvut.erm.event.CoordinateEvent)
     */
    public void minimizeEntity(cz.felk.cvut.erm.event.ResizeEvent event) {
        if (ISAChilds != null && ISAChilds.size() > 0) {
            for (Object ISAChild : ISAChilds) {
                EntityConstruct ent = (EntityConstruct) ISAChild;
                ent.minimizeEntity(event);
            }
        }
        event.setDx(-1000);
        event.setDy(-1000);
        countMinSize(event);
        super.handleResizeEvent(event);
        moveCardinalities();
        moveStrongAddictions();
        this.moveChilds(event);
        if (ISAParent != null) {
            ISAParent.moveChilds(event);
        }
    }

    /**
     * Invoken by ISA child to inform about its resizing. Invokes countResize to find out how many have to resize itself
     * and the do that by resizeEntity.
     *
     * @param event The resize event - carry the dx and dy by which want to resize the entity who.
     * @param The   resizing entity
     * @see #countResize(cz.felk.cvut.erm.event.ResizeEvent, EntityConstruct ,int)
     * @see #resizeEntity(cz.felk.cvut.erm.event.ResizeEvent)
     */
    protected void resizeParent(cz.felk.cvut.erm.event.ResizeEvent event, EntityConstruct who) {
        int index = (ISAChilds != null) ? (ISAChilds.indexOf(who)) : -1;
        cz.felk.cvut.erm.event.ResizeEvent ev = countResize(event, who, index);
        resizeEntity(ev);
    }

    /**
     * Do real resize of the entity. Calls countMinSize to tests the resize requirement, if has ISA parent the calls its
     * resizingParent method, then resize yourself and again if there is ISA parent then calls movingChilds.
     *
     * @see #countMinSize(cz.felk.cvut.erm.event.ResizeEvent)
     * @see #resizingParent(cz.felk.cvut.erm.event.ResizingEvent, EntityConstruct)
     * @see #movingChilds(cz.felk.cvut.erm.event.CoordinateEvent)
     */
    protected void resizingEntity(cz.felk.cvut.erm.event.ResizingEvent event) {
        countMinSize(event);
        if (ISAParent != null) {
            ISAParent.resizingParent(event, this);
        }
        super.handleResizingEvent(event);
        if (ISAParent != null) {
            ISAParent.movingChilds(event);
        }
    }

    /**
     * Invoken by ISA child to inform about its resizing. Invokes countResize to find out how many have to resize itself
     * and the do that by resizingEntity.
     *
     * @param event The resize event - carry the dx and dy by which want to resize the entity who.
     * @param The   resizing entity
     * @see #countResize(cz.felk.cvut.erm.event.ResizeEvent, EntityConstruct ,int)
     * @see #resizeEntity(cz.felk.cvut.erm.event.ResizeEvent)
     */
    protected void resizingParent(cz.felk.cvut.erm.event.ResizingEvent event, EntityConstruct who) {
        int index = (ISAChilds != null) ? (ISAChilds.indexOf(who)) : -1;
        cz.felk.cvut.erm.event.ResizingEvent ev = countResize(event, who, index);
        resizingEntity(ev);
    }

    /**
     * Sets the isa parent. Do the necessary move to the ISA parent, adds to its manager.
     *
     * @param man   Manager where to put into.
     * @param ent   The ISA parent.
     * @param x     Where to have to be x coordinate of the left top point.
     * @param y     Where to have to be y coordinate of the left top point.
     * @param event Necessary to construct the remove event.
     */
    protected void setISAParent(Manager man, EntityConstruct ent, int x,
                                int y, cz.felk.cvut.erm.event.CoordinateEvent event)
            throws WasNotFoundException, CycleWouldAppearException,
            ItemNotInsideManagerException, CannotHavePrimaryKeyException,
            IsISASonException {
        if (ISAParent != null) {
            throw new IsISASonException(model);
        }
        model.setISAParent(ent
                .getModel());
        int[][] source = getRect();
        int dx = x - source[0][0], dy = y - source[1][0];
        handleMoveEvent(new cz.felk.cvut.erm.event.MoveEvent(event.getX(), event.getY(), dx, dy, event.getComponent()));
        ((Invokable) manager).invokeEventHandler(new cz.felk.cvut.erm.event.RemoveEvent(0, 0, null));

        man.addItem((Item) manager);

        ISAParent = ent;
        setAsISAChild = true;
    }

    /**
     */
    /*	public void setPrimary(Atribute attr) throws IsStrongAddictedException,	IsISASonException {
            model.addMemberOfPrimaryKey(attr.model);
        }
    */    /**
     * Move atributes to the right of entity when you switch the notation from binary to CHEN
     */
    public void moveAtributesBinarytoChen(int entWidth) {
        int count = getAtributes().size();
        for (int i = 0; i < count; i++) {
            AttributeConstruct a = (getAtributes().get(i));
            try {
                a.move(entWidth + 10, 0, true);
            } catch (ItemNotInsideManagerException e1) {
                e1.printStackTrace();
            }
            //((cz.felk.cvut.erm.event.interfaces.PaintableManager) manager).repaintItem(a);
        }
        (manager).repaintItem(this);
    }

    /**
     * Writes data for entity into XML file
     *
     * @param pw java.io.PrintWriter
     */
    public void write(java.io.PrintWriter pw) {
        pw.println("\t<entity>");
        super.write(pw);
        pw.println("\t</entity>");
    }

    public static int getDIFFERENCE() {
        return DIFFERENCE;
    }

    public java.util.List<AttributeConstruct> getAttribs() {
        return attribs;
    }

    public void setAttribs(Vector<AttributeConstruct> attribs) {
        assert attribs != null;
        this.attribs = attribs;
    }

    public boolean isDecomposeAsRelation() {
        return decomposeAsRelation;
    }

    public void setDecomposeAsRelation(boolean decomposeAsRelation) {
        this.decomposeAsRelation = decomposeAsRelation;
    }

    public Vector<EntityConstruct> getISAChilds() {
        return ISAChilds;
    }

    public void setISAChilds(Vector<EntityConstruct> childs) {
        ISAChilds = childs;
    }

    public EntityConstruct getISAParent() {
        return ISAParent;
    }

    public void setISAParent(EntityConstruct parent) {
        ISAParent = parent;
    }

    public boolean isStrongAddictionChild() {
        return isStrongAddictionChild;
    }

    public void setStrongAddictionChild(boolean isStrongAddictionChild) {
        this.isStrongAddictionChild = isStrongAddictionChild;
    }

    public boolean isSetAsISAChild() {
        return ISAParent != null || setAsISAChild;
    }

    public void setSetAsISAChild(boolean setAsISAChild) {
        this.setAsISAChild = setAsISAChild;
    }

    public void setModel(Entity model) {
        this.model = model;
    }

    public Color getEntityBackgroundColor() {
        return this.getBackgroundColor();
    }

    public Color getEntityForegroundColor() {

        return this.getForegroundColor();
    }

    public boolean isSelected() {
        return selected;
    }

    public void addISAChild(EntityConstruct ent) {
        addISAChild(ent, new ResizeEvent(0, 0, 0, 0, new ResizeRectangle(0, 0, 0, 0, 0), null));
    }
}

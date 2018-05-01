/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.util.*;
import snap.util.*;
import snap.util.JSONArchiver.*;

/**
 * Represents a data bearing object from a WebSite.
 */
public class Row extends HashMap <String,Object> implements GetKeys, GetValue, SetValue {

    // The table that owns this row
    DataTable                 _table;
    
    // The original state of this row
    Row                       _original;

    // The entity that describes the data in this row
    Entity                    _entity;
    
    // Whether this row exists in DataSite
    boolean                   _saved;
    
    // The row's last save time
    long                      _modTime;
    
    // Whether this row has been modified since last load/save
    boolean                   _modified;
        
    // A property change support
    PropChangeSupport         _pcs = PropChangeSupport.EMPTY;
    
    // The modified PropertyChange
    final static String       Modified_Prop = "Modified";
    
/**
 * Returns the table that owns this row.
 */
public DataTable getTable()  { return _table; }

/**
 * Sets the table that owns this row.
 */
protected void setTable(DataTable aTable)  { _table = aTable; }

/**
 * Returns the WebSite that created this row.
 */
public DataSite getSite()  { return _table.getSite(); }

/**
 * Returns the entity that describes the data in this row.
 */
public Entity getEntity()  { return _table.getEntity(); }

/**
 * Returns the named property.
 */
public Property getProperty(String aName)  { return getEntity().getProperty(aName); }

/**
 * Returns the primary property.
 */
public Object getPrimaryValue()
{
    Property primProp = getEntity().getPrimary();
    Object primVal = get(primProp.getName());
    return primVal instanceof Number && ((Number)primVal).longValue()==0? null : primVal;
}

/**
 * Initialize row values from map (or Entity default values).
 */
public void initValues(Map aMap)
{
    // Initialize row with entity defaults
    for(Property prop : getEntity().getProperties()) { if(prop.isPrimary()) continue;
        Object value = aMap!=null? aMap.get(prop.getName()) : null;
        if(value==null) value = prop.getDefaultValue();
        if(value!=null) put(prop.getName(), value);
    }
}

/**
 * Returns the current row value for key. If key is a relation, return value as an Row or List <Row>.
 */
public Object get(Object aKey)
{
    if(aKey instanceof String) return get((String)aKey);
    if(aKey instanceof Property) return get((Property)aKey);
    throw new RuntimeException("Row.get(object): Invalid key class (" + aKey.getClass().getName() + ")");
}

/**
 * Returns the current row value for key. If key is a relation, return value as a Row or List <Row>.
 */
public Object get(String aKey)  { Property p = getProperty(aKey); return p!=null? get(p) : super.get(aKey); }

/**
 * Returns the current row value for key. If key is a relation, return value as a Row or List <Row>.
 */
public Object get(Property aProp)
{
    // Get value for property and return RowLink Row/Rows (or value/null?)
    Object value = getRaw(aProp);
    return value instanceof RowLink? ((RowLink)value).getRemoteRowOrRows() : value;
}

/**
 * Returns the current row value for key.
 */
public Object getValue(String aKey)  { Property p = getProperty(aKey); return p!=null? getValue(p) : null; }

/**
 * Returns the current row value for key.
 */
public Object getValue(Property aProp)
{
    // Get value for property and return RowLink Value/Values (or value/null?)
    Object value = getRaw(aProp);
    return value instanceof RowLink? ((RowLink)value).getRemoteValueOrValues() : value;
}

/**
 * Returns the current value or RowLink for a key.
 */
protected Object getRaw(Property aProp)
{
    // Get property name and value
    String pname = aProp.getName();
    Object value = super.get(pname);
    
    // If null and property is derived relation, create and install row link
    if(value==null && aProp.isRelation() && aProp.isDerived())
        super.put(pname, value = new RowLink(this, aProp, null));
    
    // Return value
    return value;
}

/**
 * Put value by string.
 */
public Object put(String aKey, Object aValue)  { Property p = getProperty(aKey); return put(p, aValue); }

/**
 * Override put to do conversion.
 */
public Object put(Property aProp, Object anObj)
{
    // Get raw value and old value (just return if equal)
    Object value = !aProp.isRelation()? aProp.convertValue(anObj) :
        anObj instanceof RowLink? (RowLink)anObj : new RowLink(this, aProp, anObj);
    Object old = getRaw(aProp); if(SnapUtils.equals(old, value)) return old;
    
    // If row exists and Original not set, create Original
    if(isSaved() && getOriginal()==null) _original = createOriginal();
    
    // Put value, fire PropertyChange and set Modified
    String pname = aProp.getName();
    super.put(pname, value);
    firePropertyChange(pname, old, value, -1);
    if(isSaved()) setModified(true);
    
    // Return old value
    return old;
}

/**
 * Returns the original state of this row (or null if row not modified).
 */
public Row getOriginal()  { return _original; }

/**
 * Returns the row this row was loaded from or the original state of this row.
 */
protected Row createOriginal()
{
    Row orow = new Row(); orow.setTable(getTable());
    List <Property> properties = getEntity().getProperties();
    for(Property prop : properties) { if(prop.isDerived()) continue; Object val = get(prop);
        orow.put(prop, val); }
    return orow;
}

/**
 * Returns an array of relation rows for this row that don't have a primary key value.
 */
public Row[] getUnresolvedRelationRows()
{
    // Iterate over properties and add UnresolvedRows from Relation RowLinks
    Row urows[] = null;
    List <Property> relations = getEntity().getRelations();
    for(Property relation : relations) { if(relation.isDerived()) continue; // Skip derived relations
        RowLink rlink = (RowLink)getRaw(relation); if(rlink==null) continue; // Skip null relation
        Row rurows[] = rlink.getUnresolvedRows(); if(rurows==null) continue; // Skip if no UnresolvedRows
        ArrayUtils.addAll(urows, rurows); // Add UnResolvedRows
    }
    
    // Return unresolved rows as array
    return urows;
}

/**
 * Adds an object to a list at given index.
 */
public void add(String aKey, Row aRow, int anIndex)  { add(getProperty(aKey), aRow, anIndex); }

/**
 * Adds an object to a list at given index.
 */
public void add(Property aProperty, Row aRow, int anIndex)
{
    // Get list of rows for property
    List <Row> list = (List)get(aProperty), list2 = list!=null? new ArrayList(list) : new ArrayList();
    list2.add(anIndex, aRow);
    put(aProperty, list2);
}

/**
 * Removes an object from a list at given index.
 */
public Row remove(String aKey, int anIndex)  { return remove(getProperty(aKey), anIndex); }

/**
 * Removes an object from list at given index.
 */
public Row remove(Property aProp, int anIndex)
{
    List <Row> list = (List)get(aProp), list2 = new ArrayList(list);
    Row row = list2.remove(anIndex);
    put(aProp, list2);
    return row;
}

/**
 * Returns whether row exists in site (has been saved and, if so, not deleted).
 */
public boolean isSaved()  { return _saved; }

/**
 * Sets whether row exists in site (has been saved and, if so, not deleted).
 */
protected void setSaved(boolean aFlag)  { _saved = aFlag; }

/**
 * Returns the file modification time.
 */
public long getModTime()  { return _modTime; }

/**
 * Sets the file modification time.
 */
public void setModTime(long aTime)  { _modTime = aTime; }

/**
 * Returns whether this row's row object has been changed.
 */
public boolean isModified()  { return _modified; }

/**
 * Sets whether this row's row object has been changed.
 */
protected void setModified(boolean aValue)
{
    if(aValue==isModified()) return;
    firePropertyChange(Modified_Prop, _modified, _modified = aValue, -1);
}

/**
 * Saves this row.
 */
public void save()
{
    try { getSite().saveRow(this); }
    catch(Exception e) { throw new RuntimeException(e); }
}

/**
 * Deletes this row.
 */
public void delete()
{
    try { getSite().deleteRow(this); }
    catch(Exception e) { throw new RuntimeException(e); }
}

/** PropChangeListener method to propagate changes from row object to row.  */
/*void rowDidPropChange(PropChange anEvent) { Property prop = getProperty(anEvent.getPropertyName());
    if(prop!=null && !prop.isPrimary()) put(prop, anEvent.getNewValue()); }*/

/**
 * Add a property change listener.
 */
public void addPropChangeListener(PropChangeListener aLsnr)
{
    if(_pcs==PropChangeSupport.EMPTY) _pcs = new PropChangeSupport(this);
    _pcs.addPropChangeListener(aLsnr);
}

/**
 * Remove a property change listener.
 */
public void removePropChangeListener(PropChangeListener aLsnr)  { _pcs.removePropChangeListener(aLsnr); }

/**
 * Fires a property change.
 */
protected void firePropertyChange(String aPropName, Object oldVal, Object newVal, int anIndex)
{
    _pcs.firePropChange(aPropName, oldVal, newVal);
}

/**
 * JSONArchiver method to return keys in property order and, potentially, NewValues key.
 */
public List<String> getJSONKeys()
{
    List <String> keys = new ArrayList(getEntity().getPropertyCount());
    for(Property p : getEntity().getProperties()) if(!p.isDerived()) keys.add(p.getName());
    return keys;
}

/**
 * JSONArchiver method to get archiver values via getValue() and handle NewValues.
 */
public Object getJSONValue(String aKey)  { return super.get(aKey); }

/**
 * JSONArchiver method to put archiver values and handle NewValues.
 */
public void setJSONValue(String aKey, Object aValue)  { super.put(aKey, aValue); }

/**
 * Override to just check identity, since rows are unique.
 */
public boolean equals(Object anObj)  { return anObj==this; }

/**
 * Standard toString implementation.
 */
public String toString()  { return Key.getStringValue(this, getEntity().getDescriptorKeyGuess()); }

}
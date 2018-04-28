/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.util.*;
import snap.util.SnapObject;

/**
 * A class to front for a table of data.
 */
public class DataTable extends SnapObject {

    // The data source
    DataSite                     _site;
    
    // The entity
    Entity                       _entity;
    
    // The rows that have been loaded locally
    Map <Object,Row>             _localRows = new HashMap();
    
    // The primary fetch that holds all rows
    Fetch                        _mfetch;
    
    // Constants for property changes
    static final String LocalRow_Prop = "LocalRow";
    
/**
 * Returns the data site for this table.
 */
public DataSite getSite()  { return _site; }

/**
 * Returns the data site for this table.
 */
protected void setSite(DataSite aSite)  { _site = aSite; }

/**
 * Returns the table name.
 */
public String getName()  { return getEntity().getName(); }

/**
 * Returns the entity for this table.
 */
public Entity getEntity()  { return _entity; }

/**
 * Sets the entity.
 */
protected void setEntity(Entity anEntity)  { _entity = anEntity; }

/**
 * Returns the primary fetch that returns all rows.
 */
public Fetch getMasterFetch()
{
    if(_mfetch!=null) return _mfetch;
    _mfetch = new Fetch(null, this, "AllRows");
    return _mfetch;
}

/**
 * Returns all table rows.
 */
public List <Row> getRows()  { return getMasterFetch().getRows(); }

/**
 * Creates a new row.
 */
public Row createRow()  { return getSite().createRow(getEntity(), null); }

/**
 * Returns a local row for a primary value.
 */
public synchronized Row getLocalRow(Object aPrimaryValue)  { return _localRows.get(aPrimaryValue); }

/**
 * Adds a local row.
 */
protected synchronized void addLocalRow(Row aRow)
{
    // Put row (just return if identical)
    Row old = _localRows.put(aRow.getPrimaryValue(), aRow); if(aRow==old || !aRow.getExists()) return;
    firePropChange(LocalRow_Prop, aRow, old);
}

/**
 * Removes a local row.
 */
protected synchronized void removeLocalRow(Row aRow)
{
    // Remove row
    Row old = _localRows.remove(aRow.getPrimaryValue()); if(old==null) return;
    firePropChange(LocalRow_Prop, null, old);
}

/**
 * Standard toString implementation.
 */
public String toString()
{
    return "DataTable { site=\"" + getSite().getURLString() + "\", entity=\"" + _entity.getName() + "\" }";
}

}
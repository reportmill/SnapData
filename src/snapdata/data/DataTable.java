/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.util.*;
import snap.util.SnapObject;

/**
 * This class represents a table of all rows for an entity.
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
 * Returns a row for a given entity and primary value.
 */
public Row getRow(Object aPrimeVal)
{
    // Make sure PrimaryValue is non-null
    assert(aPrimeVal!=null);
    
    // See if there is a local row - if so return it
    Row row = getLocalRow(aPrimeVal);
    if(row!=null && row.isSaved())
        return row;
        
    // Create query for fetch
    Query query = new Query(_entity); query.setFetchLimit(1);
    query.addCondition(_entity.getPrimary().getName(), Condition.Operator.Equals, aPrimeVal);
    
    // Fetch row - if found, set exists
    List <Row> rows = getRows(query);
    row = rows.size()>0? rows.get(0) : null;
    if(row!=null)
        row.setSaved(true);

    // Return row
    return row;
}

/**
 * Returns a set of rows for the given properties and condition.
 */
public List <Row> getRows(Query aQuery)
{
    // Fetch rows, set Exists and return
    List <Row> rows; try { rows = _site.getRowsImpl(this, aQuery); }
    catch(Exception e) { throw new RuntimeException(e); }
    for(Row row : rows) row.setSaved(true);
    return rows;
}

/**
 * Creates a new row.
 */
public Row createRow(Map aMap)
{
    // Create row and set values
    Row row = new Row(); row.setTable(this);
    row.initValues(aMap);
    
    // Return row
    return row;
}

/**
 * Returns a local row for a primary value.
 */
protected synchronized Row getLocalRow(Object aPrimaryValue)  { return _localRows.get(aPrimaryValue); }

/**
 * Adds a local row.
 */
protected synchronized void addLocalRow(Row aRow)
{
    // Put row (just return if identical)
    Row old = _localRows.put(aRow.getPrimaryValue(), aRow); if(aRow==old || !aRow.isSaved()) return;
    firePropChange(LocalRow_Prop, aRow, old); _mfetch = null;
}

/**
 * Removes a local row.
 */
protected synchronized void removeLocalRow(Row aRow)
{
    // Remove row
    Row old = _localRows.remove(aRow.getPrimaryValue()); if(old==null) return;
    firePropChange(LocalRow_Prop, null, old); _mfetch = null;
}

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
public List <Row> getAllRows()  { return getMasterFetch().getRows(); }

/**
 * Standard toString implementation.
 */
public String toString()
{
    return "DataTable { site=\"" + getSite().getURLString() + "\", entity=\"" + _entity.getName() + "\" }";
}

}
/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.util.*;
import snap.util.*;
import snap.web.*;

/**
 * This class performs data retrieval and udpates on a WebSite.
 */
public class DataSite extends SnapObject {

    // The WebSite
    WebSite                   _wsite;
    
    // The schema
    Schema                    _schema;
    
    // The DataTables
    Map <String,DataTable>    _tables = new HashMap();
    
    // PropChangeListener for Row changes
    PropChangeListener        _rowLsnr = pc -> rowDidPropChange(pc);
    
    // All sites
    static Map <WebSite,DataSite> _allSites = new HashMap();

/**
 * Returns the WebSite.
 */
public WebSite getSite()  { return _wsite; }

/**
 * Sets the WebSite.
 */
protected void setSite(WebSite aSite)
{
     _wsite = aSite;
    _allSites.put(aSite, this);
}

/**
 * Returns the name.
 */
public String getName()  { return _wsite.getName(); }

/**
 * Returns the URL root.
 */
public String getURLString()  { return _wsite.getURLString(); }

/**
 * Returns the schema of represented WebSite as a hierarchy of RMEntity and RMProperty objects.
 */
public synchronized Schema getSchema()
{
    if(_schema!=null) return _schema;
    Schema schema = new Schema(getName()); schema.setSite(this);
    return _schema = schema;
}

/**
 * Returns the table entity for given name.
 */
public Entity getEntity(String aName)
{
    DataTable table = getTable(aName);
    Entity entity = table!=null? table.getEntity() : null;
    return entity;
}

/**
 * Returns the list of known data tables.
 */
public synchronized List <DataTable> getTables()  { return new ArrayList(_tables.values()); }

/**
 * Returns the DataTable for given name
 */
public synchronized DataTable getTable(String aName)
{
    // Get table from cache, just return if found
    DataTable table = _tables.get(aName); if(table!=null) return table;

    // Create table with impl-specific version
    try { table = getTableImpl(aName); }
    catch(Exception e) { throw new RuntimeException(e); }
    if(table==null) return null;
    
    // Get entity and add to schema
    Entity entity = table.getEntity();
    getSchema().addEntity(entity);
    
    // Add table to map and return
    _tables.put(aName, table);
    return table;
}

/**
 * Returns the table for given name (impl-specific version for subclasses).
 * This version loads the entity from file at site:/TableName.table.
 */
protected DataTable getTableImpl(String aName) throws Exception
{
    // Get entity file (if not found, complain and return)
    WebFile efile = getEntityFile(aName, false);
    if(efile==null) { System.err.println("DataSite:getTableImpl: Entity file not found"); return null; }

    // Create entity, load from file bytes and return
    Entity entity = new Entity(aName);
    try { entity.fromBytes(efile.getBytes()); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // Create/configure DataTable and return
    DataTable table = new DataTable(); table.setSite(this); table.setEntity(entity);
    return table;
}

/**
 * Creates a table for given entity.
 */
public DataTable createTable(Entity anEntity, String oldTableName) throws Exception
{
    // Get old table and call implementation specific version
    DataTable oldTable = oldTableName!=null? getTable(oldTableName) : null;
    createTableImpl(anEntity, oldTable);
    
    // Load table normally and return
    DataTable table = getTable(anEntity.getName());
    return table;
}

/**
 * Creates table for given entity (impl-specific version for subclasses).
 * This version just saves the entity to a file at site:/TableName.table.
 */
public void createTableImpl(Entity anEntity, DataTable aTable) throws Exception
{
    WebFile efile = getEntityFile(anEntity.getName(), true); if(efile==null) return;
    efile.setBytes(anEntity.toBytes());
    efile.save();
}

/**
 * Deletes the table for given table name.
 */
public void deleteTable(String aTableName) throws Exception
{
    // Get table and call implementation specific version
    DataTable table = getTable(aTableName);
    deleteTableImpl(table);
    
    // Remove entity from schema
    Entity entity = table.getEntity();
    getSchema().removeEntity(entity);
}

/**
 * Deletes table for given table name (impl-specific version for subclasses).
 * This version just deletes the entity file at site:/TableName.table.
 */
protected void deleteTableImpl(DataTable aTable) throws Exception
{
    String name = aTable.getName();
    WebFile efile = getEntityFile(name, false); if(efile==null) return;
    efile.delete();
}

/**
 * Returns the entity file.
 */
protected WebFile getEntityFile(String aName, boolean doCreate)
{
    String path = "/" + aName + ".table";
    WebFile file = _wsite.getFile(path);
    if(file==null && doCreate) file = _wsite.createFile(path, false);
    return file;
}

/**
 * Returns a row for an entity and primary value that is guaranteed to be unique for this data source.
 */
public synchronized Row createRow(Entity anEntity, Object aPrimeVal, Map aMap)
{
    // Get table for entity
    DataTable table = getTable(anEntity.getName());
    Row row = null;
    
    // If PrimaryValue provided, check/set LocalRows cache
    if(aPrimeVal!=null) {
        row = table.getLocalRow(aPrimeVal); if(row!=null) return row;
        row = new Row(); row.setTable(table);
        row.put(anEntity.getPrimary(), aPrimeVal);
        table.addLocalRow(row);
    }
    
    // Otherwise just create row
    else { row = new Row(); row.setTable(table); }
    
    // Initialize values, start listening to PropChanges and return
    row.initValues(aMap);
    row.addPropChangeListener(_rowLsnr);
    return row;
}

/**
 * Returns a set of rows for the given table and query.
 */
protected List <Row> getRowsImpl(DataTable aTable, Query aQuery) throws Exception { throw notImpl("getRowsImpl"); }

/**
 * Inserts or updates a given row.
 */
public synchronized void saveRow(Row aRow) throws Exception
{
    // If row exists and hasn't changed, just return
    boolean exists = aRow.getExists(); if(exists && !aRow.isModified()) return;
    
    // If there are UnresolvedRelationRows, make sure they get saved
    Row urows[] = aRow.getUnresolvedRelationRows();
    if(urows!=null) {
        if(!exists) { saveRowImpl(aRow); aRow.setExists(true); } // Save this row first in case of circular reference
        for(Row urow : urows)
            urow.save();
    }

    // Save row for real
    saveRowImpl(aRow);
    
    // Set row exists and not modified and add to DataTable
    aRow.setExists(true);
    aRow.setModified(false);
    if(!exists) {
        DataTable dtable = aRow.getTable();
        dtable.addLocalRow(aRow);
    }
}

/**
 * Inserts or updates a given row.
 */
protected void saveRowImpl(Row aRow) throws Exception  { throw notImpl("saveRowImpl"); }

/**
 * Deletes a given row.
 */
public synchronized void deleteRow(Row aRow) throws Exception
{
    // Delete row
    deleteRowImpl(aRow);
    
    // Set Exists to false and remove from table
    aRow.setExists(false);
    DataTable dtable = aRow.getTable();
    dtable.removeLocalRow(aRow);    
}

/**
 * Deletes a given row.
 */
protected void deleteRowImpl(Row aRow) throws Exception  { throw notImpl("deleteRowImpl"); }

/**
 * Clears site Schema and ClassLoader.
 */
public synchronized void refresh()  { _schema = null; _wsite.refresh(); }

/**
 * Flushes any unsaved changes to backing store.
 */
public void flush() throws Exception  { _wsite.flush(); }

/**
 * Called when row changes.
 */
protected void rowDidPropChange(PropChange aPC)  { _pcs.fireDeepChange(this, aPC); }

/** Returns a "not implemented" exception for string (method name). */
private Exception notImpl(String aStr)  { return new Exception(getClass().getName() + ": Not implemented:" + aStr); }

/**
 * Returns a DataSite for given WebSite.
 */
public static DataSite get(WebSite aSite)
{
    // Get site from AllSites map, just return if found
    DataSite dsite = _allSites.get(aSite); if(dsite!=null) return dsite;

    // Create new FileDataSite, set site and return
    dsite = new FileDataSite(); dsite.setSite(aSite);
    return dsite;
}

}
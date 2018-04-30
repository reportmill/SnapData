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
    
    // The entities
    Map <String, Entity>      _entities = new HashMap();
    
    // The DataTables
    Map <String,DataTable>    _tables = new HashMap();
    
    // PropChangeListener for Row changes
    PropChangeListener        _rowLsnr = pc -> rowDidPropChange(pc);
    
/**
 * Returns the WebSite.
 */
public WebSite getWebSite()  { return _wsite; }

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
    if(_schema==null) {
        _schema = createSchema(); _schema.setName(getName()); _schema.setSite(this); }
    return _schema;
}

/**
 * Creates the schema.
 */
protected Schema createSchema()  { return new Schema(); }

/**
 * Creates an entity for given name.
 */
public synchronized Entity createEntity(String aName)
{
    // If entity already exists, just return it
    Entity entity = _entities.get(aName); if(entity!=null) return entity;
    
    // Create and add entity
    _entities.put(aName, entity = createEntityImpl(aName));
    entity.setName(aName);
    entity.setSchema(getSchema());
    return entity;
}

/**
 * Returns the entity for given name.
 */
protected Entity createEntityImpl(String aName)  { return new Entity(); }

/**
 * Returns the entity for given name.
 */
public synchronized Entity getEntity(String aName)
{
    // Get entity from files cache
    Entity entity = _entities.get(aName);
    if(entity!=null && entity.getExists())
        return entity;
    
    // Get entity for name from data source
    try { entity = getEntityImpl(aName); }
    catch(Exception e) { throw new RuntimeException(e); }
    
    // If found, set Exists to true
    if(entity!=null) {
        entity.setExists(true);
        getSchema().addEntity(entity);
    }
    
    // Return entity
    return entity;
}

/**
 * Returns the entity for given name.
 */
protected Entity getEntityImpl(String aName) throws Exception
{
    WebFile efile = _wsite.getFile("/" + aName + ".table");
    if(efile!=null) {
        Entity entity = createEntity(efile.getSimpleName());
        try { return entity.fromBytes(efile.getBytes()); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    return null;
}

/**
 * Saves the given entity.
 */
public void saveEntity(Entity anEntity) throws Exception
{
    saveEntityImpl(anEntity);
    if(!anEntity.getExists()) {
        anEntity.setExists(true);
        getSchema().addEntity(anEntity);
    }
}

/**
 * Saves the given entity.
 */
protected void saveEntityImpl(Entity anEntity) throws Exception
{
    WebFile efile = anEntity.getSourceFile(); if(efile==null) return;
    efile.setBytes(anEntity.toBytes());
    efile.save();
}

/**
 * Saves the given entity.
 */
public void deleteEntity(Entity anEntity) throws Exception
{
    deleteEntityImpl(anEntity);
    anEntity.setExists(false);
    getSchema().removeEntity(anEntity);
}

/**
 * Saves the given entity.
 */
protected void deleteEntityImpl(Entity anEntity) throws Exception
{
    WebFile efile = anEntity.getSourceFile(); if(efile==null) return;
    efile.delete();
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
    DataTable table = _tables.get(aName);
    if(table==null) {
        table = createTable(aName); if(table==null) return null;
        _tables.put(aName, table);
    }
    return table;
}

/**
 * Creates a DataTable for given entity name.
 */
protected DataTable createTable(String aName)
{
    Entity entity = getEntity(aName); if(entity==null) return null;
    DataTable table = new DataTable(); table.setSite(this); table.setEntity(entity);
    return table;
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
        DataTable dtable = getTable(aRow.getEntity().getName());
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
    DataTable dtable = getTable(aRow.getEntity().getName());
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
    DataSite dsite = _dsites.get(aSite);
    if(dsite==null) {
        dsite = new FileDataSite(); dsite._wsite = aSite;
        _dsites.put(aSite, dsite);
    }
    return dsite;
}
static Map <WebSite,DataSite> _dsites = new HashMap();

}
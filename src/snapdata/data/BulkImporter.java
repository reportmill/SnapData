/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.util.*;
import snap.util.ListUtils;

/**
 * This class provides functionality to import a list of connected maps.
 */
public class BulkImporter {

    // The data site to work with
    DataSite          _site;

    // List of provided maps
    List <Map>        _providedMaps = new ArrayList();
    
    // List of created rows
    List <Row>        _createdRows = new ArrayList();
    
/**
 * Creates a new BulkImporter with given data site.
 */
public BulkImporter(DataSite aSite)  { _site = aSite; }

/**
 * Bulk import.
 */
public void bulkImport(Map <String, List<Map>> theMaps) throws Exception
{
    // Iterate over imported entities and entity maps and add to importer
    for(String entityName : theMaps.keySet()) { List <Map> maps = theMaps.get(entityName);
        for(Map map : maps)
            createRow(entityName, map); }
    
    // Tell importer to put objects and flush site
    saveRows();
    _site.flush();
}

/**
 * Returns importer data site.
 */
public DataSite getSite()  { return _site; }

/**
 * Creates a new row for given entity name and map.
 */
public Row createRow(String aTableName, Map aMap)
{
    // Look for previously created row for provided object
    int index = ListUtils.indexOfId(_providedMaps, aMap);
    Row row = index>=0? _createdRows.get(index) : null;
    if(row!=null)
        return row;
    
    // Create row, add to lists and return
    DataTable table = getSite().getTable(aTableName);
    row = table.createRow(aMap);
    _providedMaps.add(aMap);
    _createdRows.add(row);
    return row;
}

/**
 * Creates a row deep.
 */
public void createRowDeep(Row aRow, Map aMap)
{
    // Get entity
    Entity entity = aRow.getEntity();
    
    // Add properties
    for(Property prop : entity.getProperties()) {
        
        // If property is primary or derived, just skip
        if(prop.isPrimary()) continue;
        
        // Get provided object property value (just skip if null)
        String pname = prop.getName();
        Object value = aMap.get(pname); if(value==null) continue;
        
        // If property isn't relation, just add
        if(!prop.isRelation() || prop.getRelEntity()==null)
            aRow.put(prop, value);
        
        // If to-one, create row and add
        else if(value instanceof Map) { Map map = (Map)value;
            String tableName = prop.getRelEntity().getName();
            value = createRow(tableName, map);
            aRow.put(prop, value);
        }
        
        // If to-many, get list of rows for maps and add
        else if(value instanceof List) { List list = (List)value, list2 = new ArrayList();
            String tableName = prop.getRelEntity().getName();
            for(Object item : list) { Map map = (Map)item;
                Row newRow = createRow(tableName, map);
                list2.add(newRow); }
            aRow.put(prop, list2);
        }
        
        // Otherwise complain
        else System.err.println("BulkImporter.createRowDeep: Import failure " + entity.getName() + "." + pname);
    }
}

/**
 * Save rows.
 */
public void saveRows()
{
    // Make sure all rows have been created deep
    for(int i=0; i<_providedMaps.size(); i++)
        createRowDeep(_createdRows.get(i), _providedMaps.get(i));
    
    // Save row for all created rows
    for(Row row : _createdRows)
        row.save();
    
    // Clear map/row lists
    _providedMaps.clear();
    _createdRows.clear();
}

}
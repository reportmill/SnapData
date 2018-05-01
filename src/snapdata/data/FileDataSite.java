/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.util.*;
import snap.util.*;
import snap.web.WebFile;

/**
 * A DataSite for flat files databases.
 */
public class FileDataSite extends DataSite {

    // The map of row-lists generated from import
    Map <Entity, List <Row>>      _entityRows = new HashMap();
    
    // Dirty entity set
    Set <Entity>                  _dirtyEntities = new HashSet();
    
/**
 * Override to delete data file.
 */
protected void deleteTableImpl(DataTable aTable) throws Exception
{
    super.deleteTableImpl(aTable);
    WebFile csvFile = getDataFile(aTable.getName(), false);
    if(csvFile!=null)
        csvFile.delete();
}

/**
 * Returns a set of rows for the given table and query.
 */
protected List <Row> getRowsImpl(DataTable aTable, Query aQuery)
{
    // Get entity rows
    Entity entity = aTable.getEntity();
    Condition condition = aQuery.getCondition();
    Row rows[] = getRows(entity).toArray(new Row[0]);
    
    // Create fetch list and add rows that satisfy condition
    List <Row> rows2 = new ArrayList();
    for(Row row : rows)
        if(condition==null || condition.getValue(entity, row))
            rows2.add(row);
    
    // Return rows
    return rows2;
}

/**
 * Inserts or updates a given row.
 */
protected void saveRowImpl(Row aRow)
{
    // Get row entity
    Entity entity = aRow.getEntity();
    
    // If row hasn't been saved yet, insert into entity rows and update any auto generated properties
    if(!aRow.getExists()) {
        
        // Add to entity rows
        List <Row> rows = getRows(entity);
        rows.add(aRow);
    
        // Set auto-generated properties
        for(Property prop : entity.getProperties())
            if(prop.isAutoGen()) {
                int maxID = 0; for(Row row : rows) maxID = Math.max(maxID,SnapUtils.intValue(row.get(prop)));
                aRow.put(prop, maxID + 1);
            }
    }
    
    // Add dirty entity
    synchronized (this) { _dirtyEntities.add(entity); }
}

/**
 * Deletes a given row.
 */
protected void deleteRowImpl(Row aRow)
{
    // Get EntityRows list for Entity and row for PrimaryValue (just return if no row for PrimaryValue)
    Entity entity = aRow.getEntity();
    List <Row> rows = getRows(entity);
    
    // Remove row and add row entity to DirtyEntities set
    ListUtils.removeId(rows, aRow);
    synchronized (this) { _dirtyEntities.add(entity); }
}

/**
 * Override to Saves changes if any made.
 */
public void flush() throws Exception
{
    super.flush();
    saveDataFiles();
}

/**
 * Returns the CSV file for given table name.
 */
protected WebFile getDataFile(String aName, boolean doCreate)
{
    String path = "/" + aName + ".csv";
    WebFile tfile = _wsite.getFile(path);
    if(tfile==null && doCreate) tfile = _wsite.createFile(path, false);
    return tfile;
}

/**
 * Returns the list of rows for a given entity, reading from file if not cached.
 */
protected synchronized List <Row> getRows(Entity anEntity)
{
    // Get rows from cache map and return if found
    List <Row> rows = _entityRows.get(anEntity); if(rows!=null) return rows;
    
    // Otherwise, read rows from file, add to cache map and return
    rows = readDataFile(anEntity);
    _entityRows.put(anEntity, rows);
    return rows;
}
    
/**
 * Reads the CSV file and returns the list of rows for given table.
 */
protected List <Row> readDataFile(Entity anEntity)
{
    // Create rows list
    List <Row> rows = Collections.synchronizedList(new ArrayList());

    // Get data file
    WebFile file = getDataFile(anEntity.getName(), false);
    if(file==null)
        return rows;
    
    // Create CSVReader
    CSVReader csvReader = new CSVReader();
    csvReader.setFieldDelimiter(",");
    csvReader.setHasHeaderRow(true);
    csvReader.setHasQuotedFields(true);
    
    // Read maps
    List <Map> maps = csvReader.readObject(file.getBytes(), anEntity.getName(), false);
    
    // Create rows for maps and add to entityRows list
    Property primeProp = anEntity.getPrimary(); int np = 0;
    for(Map map : maps) {                                    // Should be able to remove this prime val line soon
        Object pv = map.get(primeProp.getName()); if(pv==null) map.put(primeProp.getName(), String.valueOf(np++));
        Object pval = primeProp.convertValue(map.get(primeProp.getName()));
        Row row = createRow(anEntity, pval, map);
        rows.add(row);
    }
    
    // Return rows
    return rows;
}

/**
 * Save CSV files for changed entities.
 */
protected void saveDataFiles() throws Exception
{
    // If no dirty entities, just return
    if(_dirtyEntities.size()==0) return;

    // Copy and clear DirtyEntities
    Entity entities[];
    synchronized (this) {
        entities = _dirtyEntities.toArray(new Entity[_dirtyEntities.size()]);
        _dirtyEntities.clear();        
    }

    // Save files
    for(Entity entity : entities) saveDataFile(entity);
}

/**
 * Save CSV files for changed entity.
 */
protected void saveDataFile(Entity anEntity) throws Exception
{
    // Get entity rows and StringBuffer
    Row rows[] = getRows(anEntity).toArray(new Row[0]);
    StringBuffer sbuffer = new StringBuffer();
    
    // Iterate over properties and add header row
    for(Property property : anEntity.getProperties())
        if(!property.isDerived())
            sbuffer.append(StringUtils.getStringQuoted(property.getName())).append(", ");
    
    // Replace trailing field delimiter with record delimiter
    sbuffer.delete(sbuffer.length()-2, sbuffer.length()).append("\n");

    // Iterate over rows
    for(Row row : rows) {
    
        // Iterate over properties
        for(Property property : anEntity.getProperties()) {
            
            // Skip derived properties
            if(property.isDerived()) continue;
            
            // Get value and string
            Object value = row.getValue(property);
            String string = (String)DataUtils.convertValue(value, Property.Type.String);
            if(string==null)
                string = "";
            
            // Write string
            sbuffer.append(StringUtils.getStringQuoted(string)).append(", ");
        }
        
        // Replace trailing field delimiter with record delimiter
        sbuffer.delete(sbuffer.length()-2, sbuffer.length()).append("\n");
    }
    
    // Get CSV file, set bytes and save
    WebFile csvFile = getDataFile(anEntity.getName(), true);
    byte bytes[] = StringUtils.getBytes(sbuffer.toString()); csvFile.setBytes(bytes);
    csvFile.save();
}

}
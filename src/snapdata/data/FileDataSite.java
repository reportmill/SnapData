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
    Map <String, List <Row>>      _tableRows = new HashMap();
    
    // Dirty table name set
    Set <String>                  _dirtyTables = new HashSet();
    
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
    // Get table rows
    String name = aTable.getName();
    Entity entity = aTable.getEntity();
    Condition condition = aQuery.getCondition();
    Row rows[] = getRows(name).toArray(new Row[0]);
    
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
    // Get row table name and entity
    String tableName = aRow.getTable().getName();
    Entity entity = aRow.getEntity();
    
    // If row hasn't been saved yet, insert into table rows and update any auto generated properties
    if(!aRow.getExists()) {
        
        // Add to table rows
        List <Row> rows = getRows(tableName);
        rows.add(aRow);
    
        // Set auto-generated properties
        for(Property prop : entity.getProperties())
            if(prop.isAutoGen()) {
                int maxID = 0; for(Row row : rows) maxID = Math.max(maxID,SnapUtils.intValue(row.get(prop)));
                aRow.put(prop, maxID + 1);
            }
    }
    
    // Add dirty table
    synchronized (this) { _dirtyTables.add(tableName); }
}

/**
 * Deletes a given row.
 */
protected void deleteRowImpl(Row aRow)
{
    // Get table name and rows list for row
    String tableName = aRow.getTable().getName();
    List <Row> rows = getRows(tableName);
    
    // Remove row and add table name to DirtyTables set
    ListUtils.removeId(rows, aRow);
    synchronized (this) { _dirtyTables.add(tableName); }
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
 * Returns the list of rows for a given table name, reading from file if not cached.
 */
protected synchronized List <Row> getRows(String aName)
{
    // Get rows from cache map and return if found
    List <Row> rows = _tableRows.get(aName); if(rows!=null) return rows;
    
    // Otherwise, read rows from file, add to cache map and return
    rows = readDataFile(aName);
    _tableRows.put(aName, rows);
    return rows;
}
    
/**
 * Reads the CSV file and returns the list of rows for given table.
 */
protected List <Row> readDataFile(String aTableName)
{
    // Get table and entity
    DataTable table = getTable(aTableName);
    Entity entity = table.getEntity();
    
    // Create rows list
    List <Row> rows = Collections.synchronizedList(new ArrayList());

    // Get data file
    WebFile file = getDataFile(aTableName, false);
    if(file==null)
        return rows;
    
    // Create CSVReader
    CSVReader csvReader = new CSVReader();
    csvReader.setFieldDelimiter(",");
    csvReader.setHasHeaderRow(true);
    csvReader.setHasQuotedFields(true);
    
    // Read maps
    List <Map> maps = csvReader.readObject(file.getBytes(), aTableName, false);
    
    // Create rows for maps and add to tableRows list
    Property primeProp = entity.getPrimary(); int np = 0;
    for(Map map : maps) {                                    // Should be able to remove this prime val line soon
        Object pv = map.get(primeProp.getName()); if(pv==null) map.put(primeProp.getName(), String.valueOf(np++));
        Object pval = primeProp.convertValue(map.get(primeProp.getName()));
        Row row = createRow(entity, pval, map);
        rows.add(row);
    }
    
    // Return rows
    return rows;
}

/**
 * Save CSV files for changed tables.
 */
protected void saveDataFiles() throws Exception
{
    // If no dirty tables, just return
    if(_dirtyTables.size()==0) return;

    // Copy and clear DirtyTables
    String tableNames[];
    synchronized (this) {
        tableNames = _dirtyTables.toArray(new String[_dirtyTables.size()]);
        _dirtyTables.clear();        
    }

    // Save files
    for(String tableName : tableNames) saveDataFile(tableName);
}

/**
 * Save CSV files for changed table name.
 */
protected void saveDataFile(String aTableName) throws Exception
{
    // Get table, entity, table rows and StringBuffer
    DataTable table = getTable(aTableName);
    Entity entity = table.getEntity();
    Row rows[] = getRows(aTableName).toArray(new Row[0]);
    StringBuffer sbuffer = new StringBuffer();
    
    // Iterate over properties and add header row
    for(Property prop : entity.getProperties())
        if(!prop.isDerived())
            sbuffer.append(StringUtils.getStringQuoted(prop.getName())).append(", ");
    
    // Replace trailing field delimiter with record delimiter
    sbuffer.delete(sbuffer.length()-2, sbuffer.length()).append("\n");

    // Iterate over rows
    for(Row row : rows) {
    
        // Iterate over properties
        for(Property prop : entity.getProperties()) {
            
            // Skip derived properties
            if(prop.isDerived()) continue;
            
            // Get value and string
            Object value = row.getValue(prop);
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
    WebFile csvFile = getDataFile(aTableName, true);
    byte bytes[] = StringUtils.getBytes(sbuffer.toString()); csvFile.setBytes(bytes);
    csvFile.save();
}

}
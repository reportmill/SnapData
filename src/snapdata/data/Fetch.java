/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.util.*;
import snap.util.*;

/**
 * This class represents a subset of table rows for a Query and a DataTable.
 */
public class Fetch {

    // The name of this view
    String             _name;

    // The query
    Query              _query;

    // The data table
    DataTable          _table;
    
    // The data rows
    List <Row>         _rows;
    
/**
 * Creates a Fetch for given Query, table and name.
 */
public Fetch(Query aQuery, DataTable aTable, String aName)
{
    _query = aQuery; setTable(aTable); _name = aName;
}
    
/**
 * Returns the data site.
 */
public DataSite getSite()  { return _table.getSite(); }

/**
 * Returns the table.
 */
public DataTable getTable()  { return _table; }

/**
 * Returns the table.
 */
protected void setTable(DataTable aTable)
{
    _table = aTable;
    _table.addPropChangeListener(pc -> tableDidPropChange(pc));
}

/**
 * Returns the entity for the table file.
 */
public Entity getTableEntity()  { return getTable().getEntity(); }

/**
 * Returns the table view name.
 */
public String getName()  { return _name; }

/**
 * Sets the table view name.
 */
protected void setName(String aName)  { _name = aName; }

/**
 * Returns the query.
 */
public Query getQuery()  { return _query!=null? _query : (_query=createQuery()); }

/**
 * Sets the query.
 */
public void setQuery(Query aQuery)  { _query = aQuery; }

/**
 * Creates the default query.
 */
protected Query createQuery()  { return new Query(getTableEntity()); }

/**
 * Returns whether rows for this table view have been set.
 */
public boolean isRowsSet()  { return _rows!=null; }

/**
 * Returns the list of rows for this table view.
 */
public synchronized List <Row> getRows()  { return _rows!=null? _rows : (_rows = getRowsImpl()); }

/**
 * Returns the list of rows for this table view.
 */
protected List <Row> getRowsImpl()  { return _table.getRows(getQuery()); }

/**
 * Adds a row.
 */
protected void addRow(Row aRow)  { _rows.add(aRow); }

/**
 * Removes a row.
 */
protected void removeRow(Row aRow)  { _rows.remove(aRow); }

/**
 * Clears existing objects from this table.
 */
public void refresh()  { _query = null; _rows = null; }

/**
 * Property change.
 */
protected void tableDidPropChange(PropChange anEvent)
{
    // Handle LocalRow Add/Remove
    if(anEvent.getPropertyName()==DataTable.LocalRow_Prop) {
        Row orow = (Row)anEvent.getOldValue(), nrow = (Row)anEvent.getNewValue();
        
        // Handle LocalRow Add
        if(nrow!=null) {
            if(!isRowsSet()) return;
            Condition condition = getQuery().getCondition();
            if(condition==null || condition.getValue(getTableEntity(), nrow))
                addRow(nrow);
        }
        
        // Handle LocalRow Remove
        else if(orow!=null) {
            if(!isRowsSet()) return;
            Condition condition = getQuery().getCondition();
            if(condition==null || condition.getValue(getTableEntity(), orow))
                removeRow(orow);
        }
    }
}

}
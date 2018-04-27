package snapdata.app;
import java.util.List;
import snap.gfx.Font;
import snap.gfx.HPos;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.viewx.WebPage;
import snap.web.WebFile;
import snapdata.data.*;

/**
 * A class to provide UI editing of a DataTable.
 */
public class DataTablePane extends WebPage {

    // The table name
    String                 _tname;
    
    // The data site
    DataSite               _dsite;
    
    // The top level table showing records
    TableView <Row>        _rowsTable;
    
    // The first textfield in entity UI
    TextField              _firstFocus;
    
    // The selected row
    Row                    _selRow;
    
/**
 * Returns the table name.
 */
public String getTableName()  { return _tname!=null? _tname : (_tname=getFile().getSimpleName()); }

/**
 * Returns the DataSite.
 */
public DataSite getDataSite()  { return _dsite!=null? _dsite : (_dsite=DataSite.get(getSite())); }

/**
 * Returns the entity.
 */
public Entity getEntity()  { return getDataTable().getEntity(); }

/**
 * Returns the TableFile's default view.
 */
public DataTable getDataTable()
{
    String ename = getTableName();
    return getDataSite().getDataTable(ename);
}

/**
 * Returns the TableFile's default view.
 */
public DataTableView getDataTableView()
{
    String ename = getTableName();
    return DataTableView.getTableView(getDataTable(), "AllRows", true);
}

/**
 * Returns the list of rows for table.
 */
public List <Row> getRows()  { return getDataTableView().getRows(); }

/**
 * Returns the row count.
 */
public int getRowCount()  { return getRows().size(); }

/**
 * Returns the row at given index.
 */
public Row getRow(int anIndex)  { return getRows().get(anIndex); }

/**
 * Returns the selected record.
 */
public Row getSelRow()  { return _selRow!=null || getRowCount()==0? _selRow : (_selRow=getRow(0)); }

/**
 * Returns the selected record.
 */
public void setSelRow(Row aRow)  { _selRow = aRow; resetLater(); }

/**
 * Returns the selected record.
 */
public int getSelIndex()  { return getRows().indexOf(getSelRow()); }

/**
 * Returns the selected record.
 */
public void setSelIndex(int anIndex)
{
    int ind = anIndex>=0 && anIndex<getRowCount()? anIndex : -1;
    Row row = ind>=0? getRow(ind) : null;
    if(row!=null) setSelRow(row);
}

/**
 * Adds a row.
 */
public void addRow()
{
    Row row = getDataTableView().createRow();
    try { row.save(); }
    catch(Exception e)  { showErrorDialog(getUI(), "DataPage AddRecord Failed: " + e); return; }
    
    setSelRow(row);
}

/**
 * Removes a row.
 */
public void removeRow()
{
    // Remove selected row and delete
    Row row = getSelRow(); if(row==null) { beep(); return; }
    try { row.delete(); }
    catch(Exception e) { showErrorDialog(getUI(), "DataPage RemoveRecord Failed: " + e); return; }
    
    // Select next row
    int ind = getSelIndex(); if(ind>=getRowCount()) ind = getRowCount() - 1;
    setSelIndex(ind);
}

/**
 * Opens the given source.
 */
public DataTablePane open(Object aSource)  { return this; }

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Get RowsTable and add columns
    _rowsTable = getView("RowsTable", TableView.class);
    _rowsTable.setShowHeader(true);
    resetRowsTableCols();
    _rowsTable.setItems(getRows());
    
    // Get TabView
    TabView tabView = getView("TabView", TabView.class);
    tabView.removeTab(0);
    
    // Create entity panel for single sheet object and add to tabbed pane
    View attrsView = createUIForEntity(getEntity(), "SelRow");
    Tab tab = new Tab(); tab.setTitle("Attributes"); tab.setContent(attrsView);
    tabView.addTab(tab);
    
    // Iterate over entity relations and add tab for each
    for(Property rel : getEntity().getRelations()) {
        
        // Get relation key, create relation view and add to TabView
        String rkey = "SelRow" + '.' + rel.getName();
        View relView = createUIForRelation(rel, rkey);
        tab = new Tab(); tab.setTitle(rel.getStandardName()); tab.setContent(relView);
        tabView.addTab(tab);
    }
}

/**
 * Reset UI.
 */
protected void resetUI()
{
    // Update RowsTable
    setViewSelItem(_rowsTable, getSelRow());
    
    // Update RowText
    int ind = _rowsTable.getSelRow();
    setViewText("RowText", (ind+1) + " of " + getRowCount());
    
    // Reset Columns
}

/**
 * Respond UI.
 */
protected void respondUI(ViewEvent anEvent)
{
    // Handle RowsTable
    if(anEvent.equals(_rowsTable))
        setSelRow(_rowsTable.getSelItem());
        
    // Handle AddRowButton
    if(anEvent.equals("AddRowButton")) {
        addRow();
        requestFocus(_firstFocus);
    }
    
    // Handle RemoveRowButton
    if(anEvent.equals("RemoveRowButton")) {
        String msg = "Are you sure you want to remove record(s)?";
        if(DialogBox.showConfirmDialog(getUI(), "Remove Rows", msg))
            removeRow();
    }
    
    // Handle RowBackButton, RowBackAllButton, RowNextButton, RowNextAllButton, RowText
    if(anEvent.equals("RowBackButton")) setSelIndex(getSelIndex()-1);
    if(anEvent.equals("RowBackAllButton")) setSelIndex(0);
    if(anEvent.equals("RowNextButton")) setSelIndex(getSelIndex()+1);
    if(anEvent.equals("RowNextAllButton")) setSelIndex(getRowCount()-1);
    if(anEvent.equals("RowText")) setSelIndex(anEvent.getIntValue()-1);
}

/**
 * Reset RowsTable columns.
 */
void resetRowsTableCols()
{
    Entity entity = getEntity();
    for(Property prop : entity.getProperties()) {
        TableCol col = new TableCol(); col.setName(prop.getName()); //col.setPrefWidth(120);
        col.setHeaderText(prop.getName()); col.setItemKey(prop.getName());
        col.getHeader().setFont(Font.Arial10.getBold());
        _rowsTable.addCol(col);
    }
}

/**
 * Creates a new EntityViewer panel.
 */
private View createUIForEntity(Entity anEntity, String aKey)
{
    // Create EntityView
    ColView entityView = new ColView(); entityView.setPadding(20,100,15,36); entityView.setSpacing(4);
    entityView.setFont(Font.Arial12);
    
    // Iterate over entity properties
    for(int i=0, iMax=anEntity.getPropertyCount(); i<iMax; i++) { Property prop = anEntity.getProperty(i);
        
        // Just skip if to-many or private
        if(prop.isToMany() || prop.isPrivate()) continue;
        
        // Create RowView for property and add
        RowView rowView = new RowView(); rowView.setSpacing(6); rowView.setGrowWidth(true);
        entityView.addChild(rowView);
        
        // Get binding key
        String bindingKey = aKey + '.' + prop.getName();

        // Create, configure and add label
        Label label = new Label(prop.getStandardName() + ":");
        label.setPrefWidth(136); label.setAlign(HPos.RIGHT);
        rowView.addChild(label);
        
        // Create control
        View control;
        switch(prop.getType()) {
            case Enum: control = new ComboBox(); break;
            case Relation: control = new TextField(); break; //((TextField)control).setEditable(false); break;
            default: control = new TextField();
        }
        
        // Set First focus
        if(_firstFocus==null && control instanceof TextField) _firstFocus = (TextField)control;
        
        // Configure and add to UI
        control.setGrowWidth(true); //control.resizeRelocate(180, 25 + pane.getChildren().size()/2*25, 144, 22);
        rowView.addChild(control);

        // Add control bindings
        control.addBinding("Value", bindingKey);
        if(prop.getType()==Property.Type.Enum)
            setViewItems(control, prop.getEnumStrings());

        // If autogenerated, disable control
        //if(property.isAutoGenerated()) ((TextField)control).setEditable(false);
    }
    
    // Return pane
    return entityView;
}

/**
 * Creates a new EntityViewer panel.
 */
private View createUIForRelation(Property aRel, String aKey)
{
    // If toOne
    if(!aRel.isToMany()) return createUIForEntity(aRel.getRelationEntity(), aKey);

    // Create RelView
    ColView relView = new ColView(); relView.setPadding(36,36,36,36); relView.setFont(Font.Arial12);
    
    // Create new TableView, add Items binding and add
    TableView table = new TableView(); table.setGrowWidth(true); table.setGrowHeight(true);
    table.addBinding("Items", aKey);
    relView.addChild(table);
    
    // Iterate over properties and add columns
    Entity entity = aRel.getRelationEntity();
    List <Property> props = entity.getProperties();
    for(Property prop : props) {
        if(prop.isToMany()) continue;
        TableCol col = new TableCol(); col.setPrefWidth(75);
        col.setHeaderText(prop.getName()); col.setItemKey(prop.getName());
        table.addCol(col);
    }
    
    // Return view
    return relView;
}

/** Show Error Dialog. */
void showErrorDialog(View aView, String aMsg)  { DialogBox.showErrorDialog(aView, "Data Error", aMsg); }

/**
 * Creates a new file for use with showNewFilePanel method.
 */
protected WebFile createNewFile(String aPath)
{
    // Create file
    WebFile file = super.createNewFile(aPath);
    
    // Create text
    StringBuffer sb = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    sb.append("<ColView Padding=\"4\" Spacing=\"4\">\n");
    sb.append("</ColView>\n");
    file.setText(sb.toString());
    return file;
}

}
package snapdata.app;
import snapdata.data.*;
import java.util.*;
import snap.view.*;
import snap.viewx.DialogBox;
import snap.util.*;

/**
 * A panel to import rows into a DataTable.
 */
public class ImportRowsPanel extends ViewOwner {

    // The entity
    Entity            _entity;

    // The list of data
    List              _maps = new ArrayList();
    
    // The last data string read
    String            _dataString;
    
    // Whether data has a header row
    boolean           _hasHeaderRow;
    
    // The import table
    TableView         _importTable;
    
    // Whether to reset table columns
    boolean           _resetColumns = true;
    
/**
 * Shows the panel.
 */
public void showPanel(TablePane aTablePane)
{
    // Get site, table and table entity
    DataSite site = aTablePane.getDataSite();
    DataTable table = aTablePane.getDataTable();
    _entity = table.getEntity();
    
    // Do normal showDialogPanel (just return if cancelled)
    String title = "Table Import Panel", options[] = { "Import", "Cancel" };
    DialogBox dbox = new DialogBox(title); dbox.setContent(getUI()); dbox.setOptions(options);
    if(dbox.showOptionDialog(aTablePane.getUI(), options[0])!=0) return;
    
    // Import maps and reset TableFilePage
    if(_maps.size()>0) {
        Map map = Collections.singletonMap(_entity.getName(), _maps);
        BulkImporter imp = new BulkImporter(site);
        try { imp.bulkImport(map); }
        catch(Exception e) { throw new RuntimeException(e); }
    }
    
    // Refresh file
    runLater(() -> { aTablePane.resetLater(); aTablePane.rowsDidChange(); });
}

/**
 * Returns the entity.
 */
private Entity getEntity()  { return _entity; }

/**
 * Returns the list of maps.
 */
public List <Map> getMaps()  { return _maps; }

/**
 * Initialize UI.
 */
protected void initUI()
{
    // Get the rows table
    _importTable = getView("ImportTable", TableView.class);
    
    // Remove columns
    while(_importTable.getColCount()>0) _importTable.removeCol(0);
    
    // Iterate over properties and add columns (If column is not relation)
    for(Property prop : getEntity().getProperties()) {
        if(prop.isRelation() || prop.isPrimary()) continue;  // isAutoGen()
        TableCol col = new TableCol(); //col.setPrefWidth(75);
        col.setHeaderText(prop.getName()); col.setItemKey(prop.getName());
        _importTable.addCol(col);
    }

    // Add new binding for Table.Items to Maps
    addViewBinding(_importTable, "Items", "Maps");
    
    // Enable DragEvents
    enableEvents(getUI(), DragEvents);
}

/**
 * ResetUI.
 */
public void resetUI()
{
    // Reset columns
    //if(_resetColumns) { JFXUtils.setColumnWidths(_importTable, new Font("Arial", 11)); _resetColumns = false; }
}

/**
 * RespondUI.
 */
public void respondUI(ViewEvent anEvent)
{
    // Handle DragDropEvent
    if(anEvent.isDragEvent()) {
        anEvent.acceptDrag(); anEvent.consume();
        if(anEvent.isDragDropEvent()) {
            List <ClipboardData> files = anEvent.getClipboard().getFiles();
            ClipboardData file = files!=null && files.size()>0? files.get(0) : null;
            if(file!=null) {
                _dataString = StringUtils.getString(file.getBytes());
                loadData();
            }
            anEvent.dropComplete();
        }
    }
    
    // Handle PasteButton
    if(anEvent.equals("PasteButton")) {
        Clipboard cb = Clipboard.get();
        _dataString =  cb.hasString()? cb.getString() : null;
        loadData();
    }
    
    // Handle ClearButton
    if(anEvent.equals("ClearButton")) {
        _dataString = null; loadData(); }
    
    // Handle HasHeaderCheckBox
    if(anEvent.equals("HasHeaderCheckBox")) {
        _hasHeaderRow = anEvent.getBoolValue();
        loadData();
    }
}

/**
 * Sets column widths from data.
 */
public void setColumnWidths()  { resetLater(); _resetColumns = true; }

/**
 * Load data.
 */
private void loadData()
{
    // Clear current maps (and just return if no data string)
    _maps.clear();
    if(_dataString==null)
        return;
    
    // Create CSVReader, read maps and add to list
    CSVReader csvReader = new CSVReader(); csvReader.setHasHeaderRow(_hasHeaderRow);
    List maps = csvReader.readFromString(_dataString, getEntity());
    _maps.addAll(maps);
    setColumnWidths();
}

}
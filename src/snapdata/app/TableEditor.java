package snapdata.app;
import snap.viewx.DialogBox;
import snapdata.data.*;
import java.util.*;
import snap.view.*;
import snap.util.*;

/**
 * A class that provides UI to create/edit a DataTable.
 */
public class TableEditor extends ViewOwner {

    // The Snap site that this table box works for
    DataSite               _site;
    
    // The DialogBox currently running TableEditor
    DialogBox              _dbox;
    
    // The DataTable
    DataTable              _table;

    // The entity being edited (a copy)
    Entity                 _entity;
    
    // The currently selected property
    Property               _sprop;
    
    // The entity maps
    List <Map>             _enityMaps = new ArrayList();
    
    // A prop change listener for listening to entity changes
    PropChangeListener     _entityLsnr = pc -> entityDidPropChange(pc);
    
/**
 * Creates a TableEditor given site, table (optional) and list of data maps (optional).
 */
public TableEditor(DataSite aSite, DataTable aTable, List <Map> theMaps)
{
    // Set original entity
    _site = aSite;
    _table = aTable;
    _entity = aTable!=null? aTable.getEntity() : null;
    
    // Clone maps
    if(theMaps!=null) for(Map map : theMaps)
        _enityMaps.add(new HashMap(map));
}

/**
 * Shows the table editor panel.
 */
public DataTable showPanel(View aView)
{
    // If no entity, ask user for name and create one
    if(_entity==null) {
        _entity = new Entity("Untitled"); //_site.createEntity(name);
        setHasIdProp(true);
        Property property = new Property("Column1");
        property.setType(Property.Type.String);
        _entity.addProperty(property);
    }
    
    // Start listening to entity changes
    _entity.addPropChangeListener(_entityLsnr);
    
    // Create DialogBox that resets OK.DefaultButton property and has TableEditor UI as content
    _dbox = new DialogBox("Configure Table Panel");
    _dbox.setContent(getUI()); _dbox.setConfirmOnEnter(false);
    
    // Show option pane
    if(!_dbox.showConfirmDialog(aView)) return null;
    
    // Stop listening to property changes
    _entity.removePropChangeListener(_entityLsnr);
    
    // If table doesn't exist, create it and add configured properties
    if(_table==null) {
        try { _table = _site.createTable(_entity, null); }
        catch(Exception e) { throw new RuntimeException(e); }
        _entity = _table.getEntity();
    }
    
    // Return entity
    return _table;
}

/**
 * Returns the table.
 */
public DataTable getTable()  { return _table; }

/**
 * Returns the new entity.
 */
public Entity getEntity()  { return _entity; }

/**
 * Returns the entity maps.
 */
public List <Map> getEntityMaps()  { return _enityMaps; }

/**
 * Returns the schema.
 */
public Schema getSchema()  { return getEntity().getSchema(); }

/**
 * Returns the list of properties for table entity.
 */
public List <Property> getProperties()  { return getEntity().getProperties(); }

/**
 * Adds a property.
 */
public Property addProp()
{
    int index = ListUtils.indexOfId(getProperties(), getSelProp()) + 1;
    Property prop = new Property("Untitiled");
    prop.setType(Property.Type.String);
    getEntity().addProperty(prop, index);
    setSelProp(prop);
    return prop;
}

/**
 * Removes a property.
 */
public void removeProp()
{
    Property prop = getSelProp();
    int index = getEntity().removeProperty(prop);
    
    //
    index = Math.min(index, getEntity().getProperties().size()-1);
    Property prop2 = index>=0? getEntity().getProperties().get(index) : null;
    setSelProp(prop2);
}

/**
 * Returns the selected property.
 */
public Property getSelProp()  { return _sprop; }

/**
 * Sets the selected property.
 */
public void setSelProp(Property aProperty)  { _sprop = aProperty; }

/**
 * Returns the currently selected property number type.
 */
public Property.NumberType getSelNumType()  { return getSelProp()!=null? getSelProp().getNumberType() : null; }

/**
 * Returns the list of all entities except selected entity.
 */
public List <Entity> getRemoteEntities()
{
    List <Entity> entities = getSchema()!=null? getSchema().getEntities() : new ArrayList();
    return ListUtils.getFilteredList(entities, "Name!=\"" + getEntity().getName() + "\"");
}

/**
 * Returns the list of properties that are valid for the selected relation property's local property.
 */
public List <Property> getRelLocalProps()
{
    String pname = getSelProp()!=null? getSelProp().getName() : null;
    return ListUtils.getFilteredList(getEntity().getProperties(), "Primary || Name==\"" + pname + "\"");
}

/**
 * Returns the list of properties that are valid for the selected relation property's remote property.
 */
public List <Property> getRelRemoteProps()
{
    Property property = getSelProp();
    Entity entity = property.getRelEntity(); if(entity==null) return Collections.emptyList();
    List props = entity.getProperties();
    return ListUtils.getFilteredList(props, "Primary || RelationEntityName==\"" + getEntity().getName() + "\"");
}

/**
 * Returns whether entity has id property.
 */
public boolean getHasIdProp()  { return getEntity().getProperty("Id")!=null; }

/**
 * Sets whether entity has id property.
 */
public void setHasIdProp(boolean aValue)
{
    // If value already set, just return
    if(aValue==getHasIdProp()) return;
    
    // Add property
    if(aValue) {
        Property prop = new Property("Id");
        prop.setType(Property.Type.Number);
        prop.setNumberType(Property.NumberType.Integer);
        prop.setAutoGen(true);
        prop.setPrimary(true);
        getEntity().addProperty(prop, 0);
    }
    
    // Or remove property
    else getEntity().removeProperty(getEntity().getProperty("Id"));
}

/**
 * Returns whether entity has date created property.
 */
public boolean getHasDateCreatedProp()  { return getEntity().getProperty("DateCreated")!=null; }

/**
 * Sets whether entity has date created property.
 */
public void setHasDateCreatedProp(boolean aValue)
{
    // If value already set, just return
    if(aValue==getHasDateCreatedProp()) return;
    
    // Add property
    if(aValue) {
        Property property = new Property("DateCreated");
        property.setType(Property.Type.Date);
        getEntity().addProperty(property);
    }
    
    // Or remove property
    else getEntity().removeProperty(getEntity().getProperty("DateCreated"));
}

/**
 * Returns whether entity has date modified property.
 */
public boolean getHasDateModifiedProp()  { return getEntity().getProperty("DateModified")!=null; }

/**
 * Sets whether entity has date modified property.
 */
public void setHasDateModifiedProp(boolean aValue)
{
    // If value already set, just return
    if(aValue==getHasDateModifiedProp()) return;
    
    // Add property
    if(aValue) {
        Property property = new Property("DateModified");
        property.setType(Property.Type.Date);
        getEntity().addProperty(property);
    }
    
    // Or remove property
    else getEntity().removeProperty(getEntity().getProperty("DateModified"));
}

/**
 * Property change listener implementation.
 */
void entityDidPropChange(PropChange anEvent)
{
    // If property name change, rename maps entries
    if(anEvent.getSource() instanceof Property && anEvent.getPropertyName().equals("Name")) {
        for(Map map : getEntityMaps()) {
            Object value = map.get(anEvent.getOldValue());
            if(value!=null)
                map.put(anEvent.getNewValue(), value);
        }
    }
}

/**
 * Initialize UI panel.
 */
protected void initUI()
{
    // Initialize SelectedProperty
    setSelProp(ListUtils.getLast(getEntity().getProperties()));
    
    // Set TableNameText to be first focus
    setFirstFocus("TableNameText");
    
    // Configure FieldTypeList
    String types[] = { "Text", "Number", "Date", "Boolean", "Choice", "Record", "Record List" };
    setViewItems("FieldTypeList", types);
    
    // Add PropertyTypes ConversionMap
    Map ptypes = getConversionMap("PropertyTypes");
    String keys[] = { "String", "Enum", "Relation", "RelationList" };
    String vals[] = { "Text", "Choice", "Record", "Record List" };
    for(int i=0; i<keys.length; i++) ptypes.put(keys[i], vals[i]);
    
    // Configure TableNameText and PropNameText to selectAll when focused
    TextField tableNameText = getView("TableNameText", TextField.class);
    tableNameText.addPropChangeListener(
        pc -> { if(tableNameText.isFocused()) runLater(() -> tableNameText.selectAll()); },
        View.Focused_Prop);
    TextField propNameText = getView("PropNameText", TextField.class);
    propNameText.addPropChangeListener(
        pc -> { if(propNameText.isFocused()) runLater(() -> propNameText.selectAll()); },
        View.Focused_Prop);
        
    // Add EnterAction to focus confirm button
    addKeyActionHandler("EnterAction", "ENTER");
}

/**
 * Reset UI.
 */
protected void resetUI()
{
    // Get selected prop
    Property prop = getSelProp();

    // Update TableNameText
    setViewText("TableNameText", getEntity().getName());
    
    // Update PropsList
    setViewItems("PropsList", getProperties());
    setViewSelItem("PropsList", prop);
    
    // Update HasIdCheckBox, HasCreatedCheckBox, HasModifiedCheckBox
    setViewValue("HasIdCheckBox", getHasIdProp());
    setViewValue("HasCreatedCheckBox", getHasDateCreatedProp());
    setViewValue("HasModifiedCheckBox", getHasDateModifiedProp());
    
    // Update PropNameText
    setViewEnabled("PropNameText", prop!=null);
    setViewText("PropNameText", prop!=null? prop.getName() : null);
    
    // Update FieldTypeList
    setViewEnabled("FieldTypeList", prop!=null);
    setViewSelItem("FieldTypeList", prop!=null? prop.getType() : null);
}

/**
 * Responds to UI controls.
 */
public void respondUI(ViewEvent anEvent)
{
    // Get selected prop
    Property prop = getSelProp();

    // Handle TableNameText
    if(anEvent.equals("TableNameText")) {
        getEntity().setName(anEvent.getStringValue());
        runLater(() -> requestFocus("PropNameText"));
    }
    
    // Handle AddPropButton, RemovePropButton
    if(anEvent.equals("AddPropButton")) { addProp(); requestFocus("PropNameText");
        runLater(() -> runLater(() -> getView("PropNameText", TextField.class).selectAll())); }
    if(anEvent.equals("RemovePropButton")) removeProp();
    
    // Handle PropsList
    if(anEvent.equals("PropsList")) {
        setSelProp((Property)anEvent.getSelItem());
        requestFocus("PropNameText");
    }
    
    // Update HasIdCheckBox, HasCreatedCheckBox, HasModifiedCheckBox
    if(anEvent.equals("HasIdCheckBox")) setHasIdProp(anEvent.getBoolValue());
    if(anEvent.equals("HasCreatedCheckBox")) setHasDateCreatedProp(anEvent.getBoolValue());
    if(anEvent.equals("HasModifiedCheckBox")) setHasDateModifiedProp(anEvent.getBoolValue());
    
    // Handle PropNameText
    if(anEvent.equals("PropNameText")) {
        prop.setName(anEvent.getStringValue());
        getView("PropsList", ListView.class).updateItems(getSelProp());
        runLater(() -> requestFocus("AddPropButton"));
    }
    
    // Handle FieldTypeList
    if(anEvent.equals("FieldTypeList")) prop.setTypeName(anEvent.getStringValue());
    
    // Handle EnterAction: Focus confirm button unless on text
    if(anEvent.equals("EnterAction")) {
        if(!getView("TableNameText").isFocused() && !getView("PropNameText").isFocused())
            _dbox.getConfirmButton().requestFocus(); }
}

}
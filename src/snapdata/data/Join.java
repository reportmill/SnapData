package snapdata.data;
import java.util.*;
import snap.util.*;

/**
 * A class to represent the wiring of a Entity relation Property to a foreign Entity Property.
 */
public class Join extends SnapObject implements JSONArchiver.GetKeys, XMLArchiver.Archivable {

    // The property that owns this join
    Property          _prop;
    
    // The foreign entity
    Entity            _entity;
    
    // The foreign entity name
    String            _entityName;
    
    // The relation column local to this column's table that the relation uses as a key (foreign or local)
    String            _localPropName;
    
    // The table column name that the relation uses as a key (foreign or local)
    String            _remotePropName;

    // Whether join is to-many relation (for type Relation)
    boolean           _toMany;
    
/**
 * Returns the relation property that owns this join.
 */
public Property getProperty()  { return _prop; }

/**
 * Returns the relation entity.
 */
public Entity getEntity()
{
    if(_entity!=null) return _entity;
    Entity e = _prop.getEntity(); String name = getEntityName();
    _entity = e!=null && name!=null? e.getEntity(name) : null;
    return _entity;
}

/**
 * Sets the relation entity.
 */
public void setEntity(Entity anEntity)
{
    _entity = anEntity;
    if(_entity!=null) setEntityName(_entity.getName());
}

/**
 * Returns the name of the entity that this relation property points to. 
 */
public String getEntityName()  { return _entityName; }

/**
 * Sets the name of the entity that this relation property points to. 
 */
public void setEntityName(String aName)
{
    if(SnapUtils.equals(aName, getEntityName())) return;
    firePropChange("EntityName", _entityName, _entityName = aName);
}

/**
 * Returns the relation local property.
 */
public Property getLocalProp()
{
    return getEntity()!=null? getEntity().getProperty(getLocalPropName()) : null;
}

/**
 * Returns the property name local to this property's entity that the relation uses as a key (primary or foreign).
 */
public String getLocalPropName()
{
    if(_localPropName==null && getEntityName()!=null)
        _localPropName = _prop.getName(); // Is it useful to guess this?
    return _localPropName;
}

/**
 * Sets the property name local to this property's entity that the relation uses as a key (primary or foreign).
 */
public void setLocalPropName(String aName)
{
    if(SnapUtils.equals(aName, getLocalPropName())) return;
    firePropChange("LocalPropName", _localPropName, _localPropName = aName);
}

/**
 * Returns the relation remote property.
 */
public Property getRemoteProp()
{
    Entity entity = getEntity();
    return entity!=null? entity.getProperty(getRemotePropName()) : null;
}

/**
 * Returns the property name that the relation uses as a key in the remote table (primary or foreign).
 */
public String getRemotePropName()
{
    if(_remotePropName==null && getEntity()!=null &&
        getEntity().getPrimary()!=null)
        _remotePropName = getEntity().getPrimary().getName();
    return _remotePropName;
}

/**
 * Sets the property name that the relation uses as a key in the remote table (primary or foreign).
 */
public void setRemotePropName(String aName)
{
    if(SnapUtils.equals(aName, getRemotePropName())) return;
    firePropChange("RemotePropName", _remotePropName, _remotePropName = aName);
}

/**
 * Returns whether this join is a to multiple rows.
 */
public boolean isToMany()  { return _toMany; }

/**
 * Sets whether this property is a to multiple rows.
 */
public void setToMany(boolean aValue)
{
    if(aValue==isToMany()) return;
    firePropChange("ToMany", _toMany, _toMany = aValue);
}

/**
 * Returns whether property value is derived from other properties and doesn't require persistence.
 * A common (though advanced) example is a relationship that references the primary key against a foreign table/key.
 * A less sophisticated example would be a property based on an expression comprised of other properties.
 */
public boolean isDerived()
{
    return getLocalProp()!=null && getLocalProp().isPrimary();
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Create new element for Join
    XMLElement e = new XMLElement("Join");
    
    // Archive EntityName, LocalPropName, RemotePropName
    if(getEntityName()!=null && getEntityName().length()>0) e.add("EntityName", getEntityName());
    if(getLocalPropName()!=null && getLocalPropName().length()>0) e.add("LocalPropName", getLocalPropName());
    if(getRemotePropName()!=null && getRemotePropName().length()>0) e.add("RemotePropName", getRemotePropName());
    
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public Join fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive EntityName, LocalPropName, RemotePropName
    if(anElement.hasAttribute("EntityName")) setEntityName(anElement.getAttributeValue("EntityName"));
    if(anElement.hasAttribute("LocalPropName")) setLocalPropName(anElement.getAttributeValue("LocalPropName"));
    if(anElement.hasAttribute("RemotePropName")) setRemotePropName(anElement.getAttributeValue("RemotePropName"));
    
    // Return this property
    return this;
}

/**
 * Returns JSON keys.
 */
public List <String> getJSONKeys()  { return Arrays.asList("EntityName", "LocalPropName", "RemotePropName"); }

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other Join
    if(anObj==this) return true;
    Join other = anObj instanceof Join? (Join)anObj : null; if(other==null) return false;
    
    // Check EntityName, LocalPropName, RemotePropName
    if(!SnapUtils.equals(other._entityName, _entityName)) return false;
    if(!SnapUtils.equals(other._localPropName, _localPropName)) return false;
    if(!SnapUtils.equals(other._remotePropName, _remotePropName)) return false;
    
    // Return true since all checks passed
    return true;
}

/**
 * Standard clone implementation.
 */
public Join clone()  { return (Join)super.clone(); }

}
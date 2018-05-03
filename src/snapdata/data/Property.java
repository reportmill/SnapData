/*
 * Copyright (c) 2010, ReportMill Software. All rights reserved.
 */
package snapdata.data;
import java.text.SimpleDateFormat;
import java.util.*;
import snap.util.*;

/**
 * This class describes an attribute of an entity.
 */
public class Property extends SnapObject implements Comparable, JSONArchiver.GetKeys, XMLArchiver.Archivable {

    // The entity that owns this property
    Entity         _entity;
    
    // The name of this property
    String         _name;
    
    // The property type: String, Number, Date, Map (to-one relation), List (to-many relation)
    Type           _type = Type.String;
    
    // Whether this property is a primary key
    boolean        _primary;
    
    // Whether this property is private
    boolean        _private;
    
    // The string size when type is String
    StringSize     _stringSize = getStringSizeDefault();
    
    // The number type when type is Number
    NumberType     _numberType = getNumberTypeDefault();
    
    // The date type when type is Date
    DateType       _dateType = getDateTypeDefault();
    
    // Whether property is automatically generated (numbered), thus read-only
    boolean        _autoGen;
    
    // Whether property allows null values
    boolean        _nullable = true;
    
    // A default value for this property
    Object         _defaultValue;
    
    // The enum values (for type Enum).
    List <String>  _enumValues;
    
    // The join wiring information (for type Relation)
    Join           _join;
    
    // Constants for property types
    public enum Type { String, Number, Date, Boolean, Enum, Binary, Relation, Other }
    
    // Constants for string sizes
    public enum StringSize {
        Small,    // Up to 255 chars (8 bit length)
        Medium,   // Up to 65,000 chars (16 bit)
        Large    // Up to 4 billion chars (32 bit)
    }
    
    // Constants for number types
    public enum NumberType { Byte, Short, Integer, Long, Float, Double, Decimal }
    
    // Constants for date types
    public enum DateType { DateOnly, DateTime }

/**
 * Creates a new property.
 */
public Property()  { }

/**
 * Creates a new property with given name.
 */
public Property(String aName)  { setName(aName); }

/**
 * Creates a new property with given name and type.
 */
public Property(String aName, Type aType)  { setName(aName); setType(aType); }

/**
 * Creates a new property with given name and type.
 */
public Property(String aName, Object aType)
{
    setName(aName);
    if(aType instanceof Type) setType((Type)aType);
    else if(aType instanceof NumberType) setNumberType((NumberType)aType);
    else if(aType instanceof StringSize) setStringSize((StringSize)aType);
    else if(aType instanceof DateType) setDateType((DateType)aType);
}

/**
 * Returns the entity that owns this property.
 */
public Entity getEntity()  { return _entity; }

/**
 * Sets the entity that owns this property.
 */
public void setEntity(Entity anEntity)  { _entity = anEntity; }

/**
 * Returns the name of this property.
 */
public String getName()  { return _name; }

/**
 * Sets the name of this property.
 */
public void setName(String aName)
{
    String name = aName!=null? aName.trim().replace(" ", "") : null;
    if(SnapUtils.equals(name, _name)) return;
    firePropChange("Name", _name, _name = name);
}

/**
 * Returns the name in a standard format (strip is/get prefix and start with capital letter).
 */
public String getStandardName()  { return Key.getStandard(getName()); }

/**
 * Returns the type of this property.
 */
public Type getType()  { return _type; }

/**
 * Sets the type of this property.
 */
public void setType(Type aType)
{
    if(aType==_type) return;
    firePropChange("Type", _type, _type = aType);
}

/**
 * Returns whether this property is a primary key.
 */
public boolean isPrimary()  { return _primary; }

/**
 * Sets whether this property is a primary key.
 */
public void setPrimary(boolean isPrimary)
{
    if(isPrimary==_primary) return;
    firePropChange("Primary", _primary, _primary = isPrimary);
}

/**
 * Returns whether this property is private.
 */
public boolean isPrivate()  { return _private; }

/**
 * Sets whether this property is private.
 */
public void setPrivate(boolean isPrivate)
{
    if(isPrivate==_private) return;
    firePropChange("Private", _private, _private = isPrivate);
}

/**
 * Returns the string size.
 */
public StringSize getStringSize()  { return _stringSize; }

/**
 * Sets the string size.
 */
public void setStringSize(StringSize aSize)
{
    if(aSize==_stringSize) return;
    setType(Type.String);  // Ensure type is string
    firePropChange("StringSize", _stringSize, _stringSize = aSize);
}

/**
 * Return default string size.
 */
public StringSize getStringSizeDefault()  { return StringSize.Medium; }

/**
 * Returns the number type.
 */
public NumberType getNumberType()  { return _numberType; }

/**
 * Sets the number type.
 */
public void setNumberType(NumberType aNumberType)
{
    if(aNumberType==_numberType) return;
    setType(Type.Number);  // Ensure type is number
    firePropChange("NumberType", _numberType, _numberType = aNumberType);
}

/**
 * Return default number type.
 */
public NumberType getNumberTypeDefault()  { return NumberType.Double; }

/**
 * Returns the date type.
 */
public DateType getDateType()  { return _dateType; }

/**
 * Sets the date type.
 */
public void setDateType(DateType aDateType)
{
    if(aDateType==_dateType) return;
    setType(Type.Date);  // Ensure date type
    firePropChange("DateType", _dateType, _dateType = aDateType);
}

/**
 * Returns the default date type.
 */
public DateType getDateTypeDefault()  { return DateType.DateTime; }

/**
 * Returns whether column is automatically generated (numbered), thus read-only.
 */
public boolean isAutoGen()  { return _autoGen; }

/**
 * Sets whether column is automatically generated (numbered), thus read-only.
 */
public void setAutoGen(boolean aValue)
{
    if(aValue==isAutoGen()) return;
    firePropChange("AutoGenerated", _autoGen, _autoGen = aValue);
}

/**
 * Returns whether property allows nulls.
 */
public boolean isNullable()  { return _nullable; }

/**
 * Sets whether property allows nulls.
 */
public void setNullable(boolean aValue)
{
    if(aValue==isNullable()) return;
    firePropChange("Nullable", _nullable, _nullable = aValue);
}

/**
 * Returns the nullable default.
 */
public boolean getNullableDefault()  { return true; }

/**
 * Returns default value for this property.
 */
public Object getDefaultValue()  { return _defaultValue; }

/**
 * Sets default value for this property.
 */
public void setDefaultValue(Object aValue)
{
    Object value = convertValue(aValue);  // Get converted value
    if(SnapUtils.equals(value, _defaultValue)) return;
    firePropChange("DefaultValue", _defaultValue, _defaultValue = value);
}

/**
 * Returns the enum strings (for type Enum).
 */
public List <String> getEnumStrings()  { return _enumValues; }

/**
 * Sets the enum strings (for type Enum).
 */
public void setEnumStrings(List <String> theValues)
{
    if(SnapUtils.equals(theValues, getEnumStrings())) return;
    firePropChange("EnumValues", _enumValues, _enumValues = theValues);
}

/**
 * Returns the enum strings as a single string (comma separated).
 */
public String getEnumsString()
{
    return getEnumStrings()!=null? StringUtils.join(getEnumStrings().toArray(), ", ") : null;
}

/**
 * Sets the enum strings as a single string (comma separated).
 */
public void setEnumsString(String aValue)
{
    List <String> types = StringUtils.separate(aValue, ",", true);  // Get separated string
    setEnumStrings(types.size()>0? types : null);
}

/**
 * Returns whether this property is a simple attribute.
 */
public boolean isAttribute()  { return getType()!=Type.Relation; }

/**
 * Returns whether this property is a relation.
 */
public boolean isRelation()  { return getType()==Type.Relation; }

/**
 * Returns the join (if type relation).
 */
public Join getJoin()
{
    if(_join!=null) return _join;
    setJoin(new Join());
    return _join;
}

/**
 * Sets the join (if type relation).
 */
protected void setJoin(Join aJoin)  { _join = aJoin; _join._prop = this; }

/**
 * Returns whether this property is a to many relation.
 */
public boolean isToMany()  { return _join!=null && _join.isToMany(); }

/**
 * Returns whether property value is derived from other properties and doesn't require persistence.
 */
public boolean isDerived()  { return _join!=null && _join.isDerived(); }

/**
 * Returns the relation entity.
 */
public Entity getRelEntity()  { return _join!=null? _join.getEntity() : null; }

/**
 * Sets the type from a given name.
 */
public void setTypeName(String aName)
{
    // Get name ensuring first character is upper case
    String type = StringUtils.firstCharUpperCase(aName);
    
    // Do some legacy conversions
    if(type.equals("Map") || type.equals("List")) type = Type.Relation.toString();
    else if(type.equals("DateTime")) type = Type.Date.toString();
    else if(type.equals("Decimal")) type = Type.Number.toString();
    else if(type.equals("Base64Binary")) type = Type.Binary.toString();
    
    // Set type
    setType(Type.valueOf(type));
}

/**
 * Sets property type from sample string - tries to discern whether string represents a date or number.
 */
public void setTypeFromSample(String aSample)
{
    // Handle null, empty string or Type already String
    if(aSample==null || aSample.length()==0 || getType()==Type.String) return;
    
    // If type still assumed Date, try two common date formats and return if either work, otherwise change to Number
    if(getType()==Type.Date) {
        try { new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss").parse(aSample); return; }
        catch(Exception e) { }
        try { new SimpleDateFormat("yyyy-MM-dd").parse(aSample); return; }
        catch(Exception e) { setType(Type.Number); }
    }
    
    // If type still assumed Number, try common number format and return if it works, otherwise change to String
    if(getType()==Type.Number) {
        try { Float.parseFloat(aSample); return; }
        catch(Exception e) { setType(Type.String); }
    }
}

/**
 * Converts an arbitrary object to property type.
 */
public Object convertValue(Object anObj)
{
    // Do basic conversion
    Object value = DataUtils.convertValue(anObj, getType(), getNumberType());
    
    // Special case for Binary - gets converted to Base64 below in toString()
    if(value==null && getType()==Type.Binary && anObj instanceof String)
        try { value = ASCIICodec.decodeBase64((String)anObj); }
        catch(Exception e) { }
    
    // If value is a relation mapping that is still primary type, try to convert to remote property type
    if(isRelation() && !isDerived() && value!=null && !(value instanceof Row)) {
        Property remoteProp = getJoin().getRemoteProp();
        if(remoteProp!=null)
            value = remoteProp.convertValue(value);
    }
    
    // Return value
    return value;
}

/**
 * Key.Get implementation to return Property for key from RelEntity (if found).
 */
public Object getKeyValue(String aKey)
{
    Entity re = getRelEntity(); Property p = re!=null? re.getProperty(aKey) : null;
    return p!=null? p : Key.getValueImpl(this, aKey);
}

/**
 * Returns a string representation of given value according to property type.
 */
public String toString(Object aValue)
{
    // Get value as property type
    Object value = convertValue(aValue);
    
    // Handle specific property types
    switch(getType()) {
        case Number: return DataUtils.toString((Number)value, getNumberType());
        case Date: return DataUtils.toString((Date)value, getDateType());
        default: return SnapUtils.stringValue(value);
    }
}

/**
 * Standard equals implementation.
 */
public boolean equals(Object anObj)
{
    // Check identity and get other property
    if(anObj==this) return true;
    Property other = anObj instanceof Property? (Property)anObj : null; if(other==null) return false;
    
    // Check Name, Type
    if(!SnapUtils.equals(other._name, _name)) return false;
    if(!SnapUtils.equals(other._type, _type)) return false;
    
    // Check StringSize, NumberType, DateType
    if(getType()==Type.String && other._stringSize!=_stringSize) return false;
    if(getType()==Type.Number && other._numberType!=_numberType) return false;
    if(getType()==Type.Date && other._dateType!=_dateType) return false;
    
    // Check Primary, Private
    if(other._primary!=_primary) return false;
    if(other._private!=_private) return false;
    
    // Check Nullable, AutoGen
    if(other._nullable!=_nullable) return false;
    if(other._autoGen!=_autoGen) return false;
    
    // Check EnumValues, Join
    if(!SnapUtils.equals(other._enumValues, _enumValues)) return false;
    if(!SnapUtils.equals(other._join, _join)) return false;

    // Check DefaultValue
    if(!SnapUtils.equals(other._defaultValue, _defaultValue)) return false;
    
    // Return true since all checks passed
    return true;
}

/**
 * Standard clone implementation.
 */
public Property clone()
{
    // Do normal clone and clone enum values
    Property clone = (Property)super.clone();
    if(_enumValues!=null) clone._enumValues = ListUtils.clone(getEnumStrings());
    if(_join!=null) { clone._join = null; clone.setJoin(_join.clone()); }
    return clone;
}

/**
 * Implements comparable to compare based on name.
 */
public int compareTo(Object anObj)
{
    return anObj instanceof Property? getName().compareTo(((Property)anObj).getName()) : 0;
}

/**
 * XML archival.
 */
public XMLElement toXML(XMLArchiver anArchiver)
{
    // Create new element for property
    XMLElement e = new XMLElement("property");
    
    // Archive Name, Type
    if(_name!=null && _name.length()>0) e.add("name", _name);
    if(getType()!=null) e.add("type", getType());
    
    // Archive AutoGen, Nullable
    if(isAutoGen()) e.add("AutoGen", true);
    if(!isNullable()) e.add("Nullable", false);
    
    // Archive Primary, Private
    if(_primary) e.add("primary", true);
    if(_private) e.add("private", true);
    
    // Archive DefaultValue
    if(_defaultValue!=null) e.add("default-value", DataUtils.convertValue(_defaultValue, Type.String));
    
    // Archive StringSize, NumberType, DateType
    if(getType()==Type.String && getStringSize()!=null && getStringSize()!=StringSize.Medium)
        e.add("string-size", getStringSize());
    if(getType()==Type.Number && getNumberType()!=null) e.add("number-type", getNumberType());
    if(getType()==Type.Date && getDateType()!=null && getDateType()!=DateType.DateOnly)
        e.add("date-type", getDateType());

    // Archive EnumValues
    if(getType()==Type.Enum && getEnumStrings()!=null) e.add("enum-values", getEnumsString());
    
    // Archive Join
    if(isRelation())
        e.add(getJoin().toXML(anArchiver));
    
    // Return element
    return e;
}

/**
 * XML unarchival.
 */
public Property fromXML(XMLArchiver anArchiver, XMLElement anElement)
{
    // Unarchive Name, Type
    _name = anElement.getAttributeValue("name", anElement.getName());
    if(anElement.hasAttribute("type")) setTypeName(anElement.getAttributeValue("type"));
    
    // Unarchive AutoGen, Nullable
    if(anElement.hasAttribute("AutoGen")) setAutoGen(anElement.getAttributeBoolValue("AutoGen"));
    if(anElement.hasAttribute("Nullable")) setNullable(anElement.getAttributeBoolValue("Nullable"));
    
    // Unarchive Primary, Private
    _primary = anElement.getAttributeBoolValue("primary");
    _private = anElement.getAttributeBoolValue("private");

    // Unrchive DefaultValue
    if(anElement.hasAttribute("default-value")) setDefaultValue(anElement.getAttributeValue("default-value"));
    
    // Unarchive StringSize, NumberType, DateType
    if(anElement.hasAttribute("string-size"))
        setStringSize(StringSize.valueOf(anElement.getAttributeValue("string-size")));
    if(anElement.hasAttribute("number-type"))
        setNumberType(NumberType.valueOf(anElement.getAttributeValue("number-type")));
    if(anElement.hasAttribute("date-type")) setDateType(DateType.valueOf(anElement.getAttributeValue("date-type")));
    
    // Unarchive EnumValues
    if(anElement.hasAttribute("enum-values")) setEnumsString(anElement.getAttributeValue("enum-values"));
    
    // Unarchive Join
    XMLElement joinXML = anElement.getElement("Join");
    if(joinXML!=null)
        setJoin(new Join().fromXML(anArchiver, joinXML));
    
    // Return this property
    return this;
}

/**
 * Returns JSON keys.
 */
public List <String> getJSONKeys()
{
    List list = Arrays.asList("Name", "Type", "Primary", "Private", "StringSize", "NumberType", "DateType",
            "AutoGen", "Nullable", "DefaultValue");
    if(getType()==Type.Relation) { list = new ArrayList(list); list.add("Join"); }
    if(getType()==Type.Enum) { list = new ArrayList(list); list.add("EnumStrings"); }
    return list;
}

/**
 * Returns a string representation of this property (its name).
 */
public String toString()  { return getName(); }

}
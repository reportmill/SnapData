package snapdata.data;
import java.util.*;
import snap.util.StringUtils;

/**
 * Some useful methods for DataSite.
 */
public class DataSiteUtils {

/**
 * Returns a string of datasite tables.
 */
public static String showTables(DataSite aDataSite)
{
    StringBuffer sb = new StringBuffer();
    for(Entity entity : aDataSite.getSchema().getEntities())
        sb.append(entity.getName()).append("\n");
    return sb.toString();
}

/**
 * Execute select command.
 */
public static String executeSelect(DataSite aDataSite, String aCommand)
{
    // Get from index
    int from = StringUtils.indexOfIC(aCommand, "from");
    if(from<0)
        return "Syntax error";
    
    // Get table
    String tableName = aCommand.substring(from + 4).trim();
    DataTable table = aDataSite.getTable(tableName);
    if(table==null)
        return "Table not found";
    
    // Get entity and properties
    Entity entity = table.getEntity();
    List <Property> props = new ArrayList();
    String propNames[] = aCommand.substring(0, from).split(",");
    for(String pname : propNames) {
        if(pname.trim().equals("*")) {
            props.addAll(entity.getProperties());
            break;
        }
        Property prop = entity.getProperty(pname.trim());
        if(prop!=null)
            props.add(prop);
    }
    
    // Create string buffer
    StringBuffer sb = new StringBuffer();
    
    // Append headers
    for(Property prop : props)
        sb.append(prop.getName()).append("\t");
    if(props.size()>0) sb.delete(sb.length()-1, sb.length());
    sb.append("\n");

    // Get rows and append values
    List <Row> rows = table.getRows();
    for(Row row : rows) {
        for(Property prop : props)
            sb.append(row.get(prop.getName())).append("\t");
        if(props.size()>0) sb.delete(sb.length()-1, sb.length());
        sb.append("\n");
    }
    
    // Return string
    return sb.toString();
}

}
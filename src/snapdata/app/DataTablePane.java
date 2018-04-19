package snapdata.app;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.WebFile;

/**
 * A class to provide UI editing of a DataTable.
 */
public class DataTablePane extends ViewOwner {

/**
 * Opens the given source.
 */
public DataTablePane open(Object aSource)  { return this; }

/**
 * Provides a WebPage version of DataTablePane.
 */
public static class Page extends WebPage {

    /**
     * Override to wrap ReportPage in pane with EditButton.
     */
    protected View createUI()  { return new DataTablePane().open(getFile()).getUI(); }
    
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

}
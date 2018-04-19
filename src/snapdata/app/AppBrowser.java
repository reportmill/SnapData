package snapdata.app;
import snap.viewx.*;
import snap.web.*;

/**
 * A browser for the Snap app.
 */
public class AppBrowser extends WebBrowser {

    // The AppPane that owns this browser
    AppPane              _appPane;
    
/**
 * Returns the AppPane.
 */
public AppPane getAppPane()  { return _appPane; }

/**
 * Sets the AppPane.
 */
public void setAppPane(AppPane anAppPane)  { _appPane = anAppPane; }

/**
 * Override to make sure that AppPane is in sync.
 */
/*public void setPage(WebPage aPage)
{
    // Do normal version
    if(aPage==getPage()) return;
    super.setPage(aPage);
    
    // Forward to AppPane and AppPaneToolBar
    WebFile file = aPage!=null? aPage.getFile() : null;
    getAppPane().setSelectedFile(file);
    getAppPane().getToolBar().setSelectedFile(file);
    
    // Update ShowSideBar
    boolean showSideBar = !SnapUtils.boolValue(aPage!=null? aPage.getUI().getProp("HideSideBar") : null);
    getAppPane().setShowSideBar(showSideBar);
}*/

/**
 * Creates a WebPage for given file.
 */
protected Class <? extends WebPage> getPageClass(WebResponse aResp)
{
    // Get file and data
    WebFile file = aResp.getFile();
    String type = aResp.getPathType();
    
    // Handle Project Root directory
    //if(file!=null && file.isRoot() && getAppPane().getSites().contains(file.getSite())) return SitePane.SitePage.class;
    
    //if(type.equals("rpt")) return getPageClass("com.reportmill.app.ReportPageEditor", TextPage.class);
    //if(type.equals("snp")) return snapbuild.app.EditorPage.class;
    if(type.equals("table")) return DataTablePane.Page.class;
    
    // Do normal version
    return super.getPageClass(aResp);
}

}
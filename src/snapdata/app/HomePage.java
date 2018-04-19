package snapdata.app;
import snap.gfx.*;
import snap.view.*;
import snap.viewx.WebPage;
import snap.web.WebFile;

/**
 * A WebPage subclass that is the default homepage for SnapData apps.
 */
public class HomePage extends WebPage {
    
    // The AppPane
    AppPane       _appPane;
    
    // Whether to do stupid animation (rotate buttons on mouse enter)
    boolean       _stupidAnim;

/**
 * Creates a new HomePage for given AppPane.
 */
public HomePage(AppPane anAP)  { _appPane = anAP; }

/**
 * Returns the AppPane.
 */
public AppPane getAppPane()  { return _appPane; }

/**
 * Returns the AppPane RootSite.
 */
//public WebSite getRootSite()  { return getAppPane().getRootSite(); }

/**
 * Override to put in Page pane.
 */
protected View createUI()  { return  new ScrollView(super.createUI()); }

/**
 * Initialize UI.
 */
public void initUI()
{
    enableEvents("Header", MouseRelease);
    enableEvents("NewDataTable", MouseEvents);
    enableEvents("NewDataView", MouseEvents);
    enableEvents("NewQuery", MouseEvents);
    enableEvents("NewReport", MouseEvents);
    //enableEvents("NewJavaFile", MouseEvents);
    //enableEvents("NewFile", MouseEvents);
    enableEvents("SnapDocs", MouseEvents);
    enableEvents("RMDocs", MouseEvents);
}

/**
 * RespondUI.
 */
public void respondUI(ViewEvent anEvent)
{
    // Trigger animations on main buttons for MouseEntered/MouseExited
    if(anEvent.isMouseEnter()) {
        if(_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1.12).getRoot(1000).setRotate(180).play();
        else anEvent.getView().getAnimCleared(200).setScale(1.12).play(); }
    if(anEvent.isMouseExit()) {
        if(_stupidAnim) anEvent.getView().getAnimCleared(200).setScale(1).getRoot(1000).setRotate(0).play();
        else anEvent.getView().getAnimCleared(200).setScale(1).play(); }
        
    // Handle Header: Play click anim and toggle StupidAnim
    if(anEvent.equals("Header")) { View hdr = anEvent.getView(); _stupidAnim = !_stupidAnim;
        hdr.setBorder(_stupidAnim? Color.MAGENTA : Color.BLACK, 1);
        hdr.setScale(1.05); hdr.getAnimCleared(200).setScale(1).play();
    }

    // Handle NewDataTable
    if(anEvent.equals("NewDataTable") && anEvent.isMouseRelease()) {
        
        // Create proxy file and page
        WebFile file = getSite().createFile("/Untitled.table", false);
        WebPage page = getBrowser().createPage(file);
        
        // Use to create real file and save
        file = page.showNewFilePanel(_appPane.getUI(), file); if(file==null) return;
        try { file.save(); }
        catch(Exception e) { _appPane.getBrowser().showException(file.getURL(), e); return; }
    
        // Select file and show in tree
        _appPane.setSelectedFile(file);
        //showInTree(file);
    }
    
    // Handle NewDataView
    if(anEvent.equals("NewDataView") && anEvent.isMouseRelease()) {
        //ProjectPane ppane = ProjectPane.get(getRootSite());
        //ppane.addProject("SnapKit", "https://github.com/reportmill/SnapKit.git");
        //addSceneFiles(getRootSite(), "Scene1");
    }
    
    // Handle NewJavaFile
    //if(anEvent.equals("NewJavaFile") && anEvent.isMouseRelease()) getAppPane().showNewFilePanel();
    
    // Handle NewFile
    //if(anEvent.equals("NewFile") && anEvent.isMouseRelease()) getAppPane().showNewFilePanel();
    
    // Handle NewQuery
    if(anEvent.equals("NewQuery") && anEvent.isMouseRelease()) {
        //ProjectPane ppane = ProjectPane.get(getRootSite());
        //ppane.addProject("SnapKit", "https://github.com/reportmill/SnapKit.git");
    }

    // Handle AddSnapTea
    //if(anEvent.equals("AddSnapTea") && anEvent.isMouseRelease()) { }

    // Handle SnapDocs
    if(anEvent.equals("SnapDocs") && anEvent.isMouseRelease())
        GFXEnv.getEnv().openURL("http://www.reportmill.com/snap1/javadoc");

    // Handle RMDocs
    if(anEvent.equals("RMDocs") && anEvent.isMouseRelease())
        GFXEnv.getEnv().openURL("http://www.reportmill.com/support");
        //getBrowser().setURLString("http://www.reportmill.com/support/UserGuide.pdf");
}

/**
 * Override to suppress.
 */
public void reload()  { }

/**
 * Return better title.
 */
public String getTitle()  { return "Home Page"; }//getRootSite().getName() + " Home Page"; }

}
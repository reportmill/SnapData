package snapdata.app;
import snap.view.*;
import snap.viewx.WebBrowser;
import snap.web.*;

/**
 * A class to manage a SnapData App and files.
 */
public class AppPane extends ViewOwner {
    
    // The SourceURL
    WebURL               _url = WebURL.getURL("/tmp/MyApp");
    
    // The Site
    WebSite              _site;
    
    // The selected file
    WebFile              _selFile;
    
    // The AppPaneToolBar
    AppPaneToolBar       _toolBar = new AppPaneToolBar(this);
    
    // The FilesPane
    AppFilesPane         _filesPane = new AppFilesPane(this);
    
    // The main SplitView that holds sidebar and browser
    SplitView            _mainSplit;
    
    // The SplitView that holds FilesPane and ProcPane
    SplitView            _sideBarSplit;
    
    // The AppBroswer for displaying editors
    WebBrowser           _browser;
    
    // The pane that the browser sits in
    SplitView            _browserBox;
    
    // The HomePage
    HomePage             _homePage;

/**
 * Returns the URL to the app.
 */
public WebURL getSourceURL()  { return _url; }

/**
 * Returns the Site.
 */
public WebSite getSite()
{
    // If already set, just return
    if(_site!=null) return _site;
    
    // Set from URL
    _site = _url.getAsSite();
    
    // If rood dir doesn't exist, save
    if(!_site.getRootDir().getExists())
        _site.getRootDir().save();
        
    // Return site
    return _site;
}

/**
 * Returns the selected file.
 */
public WebFile getSelectedFile()  { return _selFile; }

/**
 * Sets the selected site file.
 */
public void setSelectedFile(WebFile aFile)
{
    // If file already set, just return
    if(aFile==null || aFile==getSelectedFile()) return;
    _selFile = aFile;
    
    // Set selected file and update tree
    if(_selFile!=null && _selFile.isFile() || _selFile.isRoot())
        getBrowser().setFile(_selFile);
    _filesPane.resetLater();
}

/**
 * Returns the browser.
 */
public WebBrowser getBrowser()  { return _browser; }

/**
 * Creates a new document.
 */
public AppPane newDocument()  { return this; }

/**
 * Opens a new document using open panel.
 */
public AppPane open(View aView)  { return this; }

/**
 * Opens a new document from given source.
 */
public AppPane open(Object aSource)  { return this; }

/**
 * Shows the AppPane window.
 */
public void show()
{
    // Set AppPane as OpenSite and show window
    getUI();
    //getWindow().setSaveName("AppPane");
    //getWindow().setSaveSize(true);
    getWindow().setVisible(true);
    
    // Open site and show home page
    showHomePage();
}

/**
 * Returns the HomePage.
 */
public HomePage getHomePage()
{
    if(_homePage!=null) return _homePage;
    _homePage = new HomePage(this);
    getBrowser().setPage(_homePage.getURL(), _homePage);
    return _homePage;
}

/**
 * Returns whether is showing SideBar (holds FilesPane and ProcPane).
 */
public boolean isShowSideBar()  { return _showSideBar; } boolean _showSideBar = true;

/**
 * Sets whether to show SideBar (holds FilesPane and ProcPane).
 */
public void setShowSideBar(boolean aValue)
{
    if(aValue==isShowSideBar()) return;
    _showSideBar = aValue;
    if(aValue)
        _mainSplit.addItemWithAnim(_sideBarSplit,220,0);
    else _mainSplit.removeItemWithAnim(_sideBarSplit);
}

/**
 * Returns the HomePageURL.
 */
public WebURL getHomePageURL()  { return getHomePage().getURL(); }

/**
 * Shows the home page.
 */
public void showHomePage()  { getBrowser().setURL(getHomePageURL()); }

/**
 * Creates the UI.
 */
protected View createUI()
{
    _mainSplit = (SplitView)super.createUI();
    ColView vbox = new ColView(); vbox.setFillWidth(true);
    vbox.setChildren(_toolBar.getUI(), _mainSplit);
    return vbox;
}

/**
 * Initializes UI panel.
 */
protected void initUI()
{
    // Get AppBrowser
    _browser = getView("Browser", WebBrowser.class);
    //_browser.setAppPane(this);
    
    // Listen to Browser PropChanges, to update ActivityText, ProgressBar, Window.Title
    _browser.addPropChangeListener(pc -> resetLater());
    
    // Get SideBarSplit and add FilesPane, ProcPane
    _sideBarSplit = getView("SideBarSplitView", SplitView.class); _sideBarSplit.setBorder(null);
    View filesPaneUI = _filesPane.getUI(); filesPaneUI.setGrowHeight(true);
    //View procPaneUI = _procPane.getUI(); procPaneUI.setPrefHeight(250);
    _sideBarSplit.setItems(filesPaneUI);//, procPaneUI);
    _sideBarSplit.setClipToBounds(true);
    
    // Get browser box
    _browserBox = getView("BrowserBox", SplitView.class);
    _browserBox.setGrowWidth(true); _browserBox.setBorder(null);
    for(View c : _browserBox.getChildren()) c.setBorder(null);
    _browserBox.getChild(0).setGrowHeight(true); // So support tray has constant size
    
    // Add key binding to OpenMenuItem and CloseWindow
    addKeyActionHandler("OpenMenuItem", "meta O");
    addKeyActionHandler("CloseFileAction", "meta W");

    // Configure Window
    getWindow().setTitle("SnapData App");
    //getRootView().setMenuBar(getMenuBar());
    
    // Register for WelcomePanel on close
    enableEvents(getWindow(), WinClose);
    
    // Remove StatusBar
    getView("StatusBar").getHost().removeGuest(getView("StatusBar"));
}

}
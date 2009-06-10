/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*******************************************************************************/

package org.eclipse.imp.java.hosted.wizards;

import java.io.File;
import java.util.Observable;
import java.util.Observer;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.imp.java.hosted.wizards.fields.DialogField;
import org.eclipse.imp.java.hosted.wizards.fields.IDialogFieldListener;
import org.eclipse.imp.java.hosted.wizards.fields.IStringButtonAdapter;
import org.eclipse.imp.java.hosted.wizards.fields.SelectionButtonDialogField;
import org.eclipse.imp.java.hosted.wizards.fields.StringButtonDialogField;
import org.eclipse.imp.java.hosted.wizards.fields.StringDialogField;
import org.eclipse.imp.java.hosted.wizards.utils.LayoutUtil;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

public class NewProjectWizardFirstPage extends WizardPage {
    // TODO Does the JDT export this from a public place? It's available from an internal class, but we don't want to ref that.
    // (See org.eclipse.jdt.internal.ui.preferences.NewJavaProjectPreferencePage)
    public static final String BuildPathPreferencePageID= "org.eclipse.jdt.ui.preferences.BuildPathPreferencePage"; //$NON-NLS-1$

    private NameGroup fNameGroup;

    private LocationGroup fLocationGroup;

    private LayoutGroup fLayoutGroup;

    //    private JREGroup fJREGroup;
    //    private DetectGroup fDetectGroup;
    private Validator fValidator;

    protected String fInitialName;

    public NewProjectWizardFirstPage(String pageName) {
	super(pageName);
    }

    public NewProjectWizardFirstPage(String pageName, String title, ImageDescriptor titleImage) {
	super(pageName, title, titleImage);
	// TODO Auto-generated constructor stub
    }

    /**
     * Request a project name. Fires an event whenever the text field is
     * changed, regardless of its content.
     */
    public final class NameGroup extends Observable implements IDialogFieldListener {

	protected final StringDialogField fNameField;

	public NameGroup(Composite composite, String initialName) {
	    final Composite nameComposite= new Composite(composite, SWT.NONE);
	    nameComposite.setFont(composite.getFont());
	    nameComposite.setLayout(initGridLayout(new GridLayout(2, false), false));
	    nameComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

	    // text field for project name
	    fNameField= new StringDialogField();
	    fNameField.setLabelText("Name:");
	    fNameField.setDialogFieldListener(this);

	    setName(initialName);

	    fNameField.doFillIntoGrid(nameComposite, 2);
	    LayoutUtil.setHorizontalGrabbing(fNameField.getTextControl(null));
	}

	protected void fireEvent() {
	    setChanged();
	    notifyObservers();
	}

	public String getName() {
	    return fNameField.getText().trim();
	}

	public void postSetFocus() {
	    fNameField.postSetFocusOnDialogField(getShell().getDisplay());
	}

	public void setName(String name) {
	    fNameField.setText(name);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
	 */
	public void dialogFieldChanged(DialogField field) {
	    fireEvent();
	}
    }

    /**
     * Request a location. Fires an event whenever the checkbox or the location
     * field is changed, regardless of whether the change originates from the
     * user or has been invoked programmatically.
     */
    public final class LocationGroup extends Observable implements Observer, IStringButtonAdapter, IDialogFieldListener {

	protected final SelectionButtonDialogField fWorkspaceRadio;

	protected final SelectionButtonDialogField fExternalRadio;

	protected final StringButtonDialogField fLocation;

	private String fPreviousExternalLocation;

	//	private static final String DIALOGSTORE_LAST_EXTERNAL_LOC= JavaUI.ID_PLUGIN + ".last.external.project"; //$NON-NLS-1$

	public LocationGroup(Composite composite) {

	    final int numColumns= 3;

	    final Group group= new Group(composite, SWT.NONE);
	    group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    group.setLayout(initGridLayout(new GridLayout(numColumns, false), true));
	    group.setText("Project Location:");

	    fWorkspaceRadio= new SelectionButtonDialogField(SWT.RADIO);
	    fWorkspaceRadio.setDialogFieldListener(this);
	    fWorkspaceRadio.setLabelText("Workspace");

	    fExternalRadio= new SelectionButtonDialogField(SWT.RADIO);
	    fExternalRadio.setLabelText("External");

	    fLocation= new StringButtonDialogField(this);
	    fLocation.setDialogFieldListener(this);
	    fLocation.setLabelText("Location");
	    fLocation.setButtonLabel("Browse...");

	    fExternalRadio.attachDialogField(fLocation);

	    fWorkspaceRadio.setSelection(true);
	    fExternalRadio.setSelection(false);

	    fPreviousExternalLocation= ""; //$NON-NLS-1$

	    fWorkspaceRadio.doFillIntoGrid(group, numColumns);
	    fExternalRadio.doFillIntoGrid(group, numColumns);
	    fLocation.doFillIntoGrid(group, numColumns);
	    LayoutUtil.setHorizontalGrabbing(fLocation.getTextControl(null));
	}

	protected void fireEvent() {
	    setChanged();
	    notifyObservers();
	}

	protected String getDefaultPath(String name) {
	    final IPath path= Platform.getLocation().append(name);
	    return path.toOSString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Observer#update(java.util.Observable,
	 *      java.lang.Object)
	 */
	public void update(Observable o, Object arg) {
	    if (isInWorkspace()) {
		fLocation.setText(getDefaultPath(fNameGroup.getName()));
	    }
	    fireEvent();
	}

	public IPath getLocation() {
	    if (isInWorkspace()) {
		return Platform.getLocation();
	    }
	    return Path.fromOSString(fLocation.getText().trim());
	}

	public boolean isInWorkspace() {
	    return fWorkspaceRadio.isSelected();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IStringButtonAdapter#changeControlPressed(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
	 */
	public void changeControlPressed(DialogField field) {
	    final DirectoryDialog dialog= new DirectoryDialog(getShell());
	    dialog.setMessage("???");
	    String directoryName= fLocation.getText().trim();
	    if (directoryName.length() == 0) {
		//		String prevLocation= JavaPlugin.getDefault().getDialogSettings().get(DIALOGSTORE_LAST_EXTERNAL_LOC);
		//		if (prevLocation != null) {
		//		    directoryName= prevLocation;
		//		}
	    }

	    if (directoryName.length() > 0) {
		final File path= new File(directoryName);
		if (path.exists())
		    dialog.setFilterPath(directoryName);
	    }
	    final String selectedDirectory= dialog.open();
	    if (selectedDirectory != null) {
		fLocation.setText(selectedDirectory);
		//		JavaPlugin.getDefault().getDialogSettings().put(DIALOGSTORE_LAST_EXTERNAL_LOC, selectedDirectory);
	    }
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.internal.ui.wizards.dialogfields.IDialogFieldListener#dialogFieldChanged(org.eclipse.jdt.internal.ui.wizards.dialogfields.DialogField)
	 */
	public void dialogFieldChanged(DialogField field) {
	    if (field == fWorkspaceRadio) {
		final boolean checked= fWorkspaceRadio.isSelected();
		if (checked) {
		    fPreviousExternalLocation= fLocation.getText();
		    fLocation.setText(getDefaultPath(fNameGroup.getName()));
		} else {
		    fLocation.setText(fPreviousExternalLocation);
		}
	    }
	    fireEvent();
	}
    }

    /**
     * Request a project layout.
     */
    public final class LayoutGroup implements Observer, SelectionListener {

	private final SelectionButtonDialogField fStdRadio, fSrcBinRadio;

	private final Group fGroup;

	private final Link fPreferenceLink;

	public LayoutGroup(Composite composite) {

	    fGroup= new Group(composite, SWT.NONE);
	    fGroup.setFont(composite.getFont());
	    fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	    fGroup.setLayout(initGridLayout(new GridLayout(3, false), true));
	    //fGroup.setText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_title); 	// <= 3.3
	    //fGroup.setText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_title);	// >= 3.4
	    fGroup.setText("Project layout");
	    
	    fStdRadio= new SelectionButtonDialogField(SWT.RADIO);
	    //fStdRadio.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_option_oneFolder);	// <= 3.3
	    //fStdRadio.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_option_oneFolder);	// >= 3.4
	    fStdRadio.setLabelText("&Use project folder as root for sources and class files");

	    fSrcBinRadio= new SelectionButtonDialogField(SWT.RADIO);
	    //fSrcBinRadio.setLabelText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_option_separateFolders);	// <= 3.3
	    //fSrcBinRadio.setLabelText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_option_separateFolders);	// >= 3.4
	    fSrcBinRadio.setLabelText("&Create separate folders for sources and class files");
	    
	    fStdRadio.doFillIntoGrid(fGroup, 3);
	    LayoutUtil.setHorizontalGrabbing(fStdRadio.getSelectionButton(null));

	    fSrcBinRadio.doFillIntoGrid(fGroup, 2);

	    fPreferenceLink= new Link(fGroup, SWT.NONE);
	    //fPreferenceLink.setText(NewWizardMessages.JavaProjectWizardFirstPage_LayoutGroup_link_description);	// <= 3.3
	    //fPreferenceLink.setText(NewWizardMessages.NewJavaProjectWizardPageOne_LayoutGroup_link_description);	// >= 3.4
	    fPreferenceLink.setText("<a>Configure d&efault...</a>");
	    fPreferenceLink.setLayoutData(new GridData(GridData.END, GridData.END, false, false));
	    fPreferenceLink.addSelectionListener(this);

	    // RMF 7/25/2006 - Default to "src/bin" folders; the builder can't yet handle the project otherwise...
	    boolean useSrcBin= true; // PreferenceConstants.getPreferenceStore().getBoolean(PreferenceConstants.SRCBIN_FOLDERS_IN_NEWPROJ);
	    fSrcBinRadio.setSelection(useSrcBin);
	    fStdRadio.setSelection(!useSrcBin);
	}

	public void update(Observable o, Object arg) {
	    final boolean detect= false; // fDetectGroup.mustDetect();
	    fStdRadio.setEnabled(!detect);
	    fSrcBinRadio.setEnabled(!detect);
	    fPreferenceLink.setEnabled(!detect);
	    fGroup.setEnabled(!detect);
	}

	public boolean isSrcBin() {
	    return fSrcBinRadio.isSelected();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetSelected(SelectionEvent e) {
	    widgetDefaultSelected(e);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.SelectionListener#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
	 */
	public void widgetDefaultSelected(SelectionEvent e) {
	    String id= BuildPathPreferencePageID;
	    PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, null).open();
//	    fDetectGroup.handleComplianceChange();
//	    fJREGroup.handlePossibleComplianceChange();
	}
    }

    /**
     * Validate this page and show appropriate warnings and error NewWizardMessages.
     */
    public final class Validator implements Observer {

	public void update(Observable o, Object arg) {

	    final IWorkspace workspace= ResourcesPlugin.getWorkspace();

	    final String name= fNameGroup.getName();

	    // check wether the project name field is empty
	    if (name.length() == 0) { //$NON-NLS-1$
		setErrorMessage(null);
		setMessage("Enter a project name");
		setPageComplete(false);
		return;
	    }

	    // check whether the project name is valid
	    final IStatus nameStatus= workspace.validateName(name, IResource.PROJECT);
	    if (!nameStatus.isOK()) {
		setErrorMessage(nameStatus.getMessage());
		setPageComplete(false);
		return;
	    }

	    // check whether project already exists
	    final IProject handle= getProjectHandle();
	    if (handle.exists()) {
		setErrorMessage("Project already exists");
		setPageComplete(false);
		return;
	    }

	    final String location= fLocationGroup.getLocation().toOSString();

	    // check whether location is empty
	    if (location.length() == 0) {
		setErrorMessage(null);
		setMessage("Enter location");
		setPageComplete(false);
		return;
	    }

	    // check whether the location is a syntactically correct path
	    if (!Path.EMPTY.isValidPath(location)) { //$NON-NLS-1$
		setErrorMessage("Invalid directory");
		setPageComplete(false);
		return;
	    }

	    // check whether the location has the workspace as prefix
	    IPath projectPath= Path.fromOSString(location);
	    if (!fLocationGroup.isInWorkspace() && Platform.getLocation().isPrefixOf(projectPath)) {
		setErrorMessage("Cannot create in workspace");
		setPageComplete(false);
		return;
	    }

	    // If we do not place the contents in the workspace validate the
	    // location.
	    if (!fLocationGroup.isInWorkspace()) {
		final IStatus locationStatus= workspace.validateProjectLocation(handle, projectPath);
		if (!locationStatus.isOK()) {
		    setErrorMessage(locationStatus.getMessage());
		    setPageComplete(false);
		    return;
		}
	    }

	    setPageComplete(true);

	    setErrorMessage(null);
	    setMessage(null);
	}
    }

    /**
     * Creates a project resource handle for the current project name field
     * value.
     * <p>
     * This method does not create the project resource; this is the
     * responsibility of <code>IProject::create</code> invoked by the new
     * project resource wizard.
     * </p>
     * 
     * @return the new project resource handle
     */
    public IProject getProjectHandle() {
	return ResourcesPlugin.getWorkspace().getRoot().getProject(fNameGroup.getName());
    }

    /**
     * Returns the current project location path as entered by the user, or its
     * anticipated initial value. Note that if the default has been returned
     * the path in a project description used to create a project should not be
     * set.
     * 
     * @return the project location path or its anticipated initial value.
     */
    public IPath getLocationPath() {
	return fLocationGroup.getLocation();
    }

    public boolean isInWorkspace() {
        final String location= fLocationGroup.getLocation().toOSString();
        IPath projectPath= Path.fromOSString(location);
        return Platform.getLocation().isPrefixOf(projectPath);
    }

    public boolean getDetect() {
	return false; // fDetectGroup.mustDetect();
    }

    public boolean isSrcBin() {
	return fLayoutGroup.isSrcBin();
    }

    public String getJRECompliance() {
	return JavaCore.VERSION_1_4; // fJREGroup.getJRECompliance();
    }

    public void createControl(Composite parent) {
	initializeDialogUnits(parent);

	final Composite composite= new Composite(parent, SWT.NULL);
	composite.setFont(parent.getFont());
	composite.setLayout(initGridLayout(new GridLayout(1, false), true));
	composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

	// create UI elements
	fNameGroup= new NameGroup(composite, fInitialName);
	fLocationGroup= new LocationGroup(composite);
	//	fJREGroup= new JREGroup(composite);
	fLayoutGroup= new LayoutGroup(composite);
	//	fDetectGroup= new DetectGroup(composite);

	// establish connections
	fNameGroup.addObserver(fLocationGroup);
	//	fDetectGroup.addObserver(fLayoutGroup);
	//	fDetectGroup.addObserver(fJREGroup);
	//	fLocationGroup.addObserver(fDetectGroup);

	// initialize all elements
	fNameGroup.notifyObservers();

	// create and connect validator
	fValidator= new Validator();
	fNameGroup.addObserver(fValidator);
	fLocationGroup.addObserver(fValidator);

	setControl(composite);
	Dialog.applyDialogFont(composite);
    }

    /**
     * Initialize a grid layout with the default Dialog settings.
     */
    protected GridLayout initGridLayout(GridLayout layout, boolean margins) {
	layout.horizontalSpacing= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_SPACING);
	layout.verticalSpacing= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_SPACING);
	if (margins) {
	    layout.marginWidth= convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
	    layout.marginHeight= convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
	} else {
	    layout.marginWidth= 0;
	    layout.marginHeight= 0;
	}
	return layout;
    }
}

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

package org.eclipse.imp.java.hosted.debug;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.imp.runtime.RuntimePlugin;
import org.eclipse.imp.smapi.LineMapBuilder;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.debug.core.IJavaLineBreakpoint;
import org.eclipse.jdt.debug.core.JDIDebugModel;

public class BreakpointUtils {
    public static boolean validateLineNumber(IFile origSrcFile, Integer origSrcLineNumber) {
        LineMapBuilder lmb= new LineMapBuilder(origSrcFile.getRawLocation().removeFileExtension().toString());
        Map lineMap= lmb.getLineMap();

        if (lineMap.containsKey(origSrcLineNumber))
            return true;
        
        return false;
    }

    public static  IFile javaFileForRootSourceFile(IFile rootSrcFile) {
        IProject project = rootSrcFile.getProject();
        String rootSrcName= rootSrcFile.getName();

        return project.getFile(rootSrcFile.getProjectRelativePath().removeLastSegments(1).append(
                rootSrcName.substring(0, rootSrcName.lastIndexOf('.')) + ".java"));
    }

    // MV -- This method is called from smapifier to reset the breakpoint in the
    // Java file when a new build has been done.
    // RMF 5/9/2009 - doesn't seem to be true (at least for LEG) - this method is
    // never called, and yet the breakpoint markers' line #'s get updated properly.
    public static String getTypeName(IFile origSrcFile) {
        IProject project = origSrcFile.getProject();
        IJavaProject javaProj= JavaCore.create(project);
        
        String pathPrefix = project.getWorkspace().getRoot().getRawLocation() + project.getFullPath().toString();
        IPath projPath= project.getFullPath();
        //MV Note: javaProj.getOutputLocation returns a workspace relative path
        boolean projectIsSrcBin;

        try {
            projectIsSrcBin = (javaProj.getOutputLocation().matchingFirstSegments(projPath) == projPath.segmentCount()) && 
                                     (javaProj.getOutputLocation().segmentCount() == projPath.segmentCount());
            if (!projectIsSrcBin) {
                String temp = origSrcFile.getRawLocation().toString().substring(pathPrefix.length()).substring(1);
                pathPrefix = pathPrefix + "/" + temp.substring(0,temp.indexOf("/"));
            }
        } catch (JavaModelException e) {
            RuntimePlugin.getInstance().logException("Error determining path of file " + origSrcFile.getFullPath(), e);
        }
        
        String temp = origSrcFile.getRawLocation().toString().substring(pathPrefix.length()).replaceAll("/", ".");
        return temp.substring(1,temp.lastIndexOf("."));
    }

    public static void resetJavaBreakpoints(IFile origSrcFile) {
    	IFile javaFile = javaFileForRootSourceFile(origSrcFile);
    	//first record which lines in original source file need to have a breakpoint
    	//this is needed because when we remove the breakpoints from the java file,
    	//the markers in the original file get deleted.
    	
    	Set<Integer> lineNumbers = new HashSet<Integer>();
    	try {
    		IMarker[] markers = origSrcFile.findMarkers(IBreakpoint.LINE_BREAKPOINT_MARKER, /*false*/true, IResource.DEPTH_INFINITE);
    		for (int i = 0; i < markers.length; i++) {
    			Integer num = (Integer) markers[i].getAttribute(IMarker.LINE_NUMBER);
    			if (validateLineNumber(origSrcFile,num))
    				lineNumbers.add(num);
    		}
    	} catch (CoreException e){
    		System.err.println(e);
    	}
    	
    	//remove all breakpoints from java file, this also removes the original markers
    	IBreakpoint[] breakpoints = DebugPlugin.getDefault().getBreakpointManager().getBreakpoints();
    	for (int i = 0; i < breakpoints.length ; i++){
    		IResource res = breakpoints[i].getMarker().getResource();
    		if (res.equals((IResource)/*javaFile*/origSrcFile)){
    			try {
    				DebugPlugin.getDefault().getBreakpointManager().removeBreakpoint(breakpoints[i], true);
    			} catch (CoreException e) {
    				e.printStackTrace();
    			}
    
    		}
    	}
    	
    	//now add new breakpoints to the java file, and create corresponding marker in orig source file
    	try {
    		for(Integer origSrcLineNumber: lineNumbers) {
    			String typeName = getTypeName(origSrcFile);
    			Map bkptAttributes= new HashMap();
//            bkptAttributes.put("org.eclipse.jdt.debug.core.sourceName", typeName);
//            final IBreakpoint bkpt= JDIDebugModel.createLineBreakpoint(javaFile, typeName, origSrcLineNumber.intValue(), -1, -1, 0, true,
//                  bkptAttributes);
//    			bkptAttributes.put("org.eclipse.jdt.debug.core.sourceName", origSrcFile.getFullPath().toString());
    			final IBreakpoint bkpt= JDIDebugModel.createStratumBreakpoint(origSrcFile , "x10", origSrcFile.getName(), /*origSrcFile.getFullPath().toString()*/null, null, origSrcLineNumber.intValue(), -1, -1, 0, true, bkptAttributes);
    
// mmk 7/29/08: removal of breakpoint doesn't appear to remove original source marker.  So we shouldn't "re"-create it -- yields duplicates on recompile
//            // create the marker
//            final IMarker javaMarker= bkpt.getMarker();
//            IMarker origSrcMarker= origSrcFile.createMarker(IBreakpoint.LINE_BREAKPOINT_MARKER);
//            
//    
//            Map javaMarkerAttrs= javaMarker.getAttributes();
//            for(Iterator iter= javaMarkerAttrs.keySet().iterator(); iter.hasNext();) {
//                String key= (String) iter.next();
//                Object value= javaMarkerAttrs.get(key);
//                if (key.equals(IMarker.LINE_NUMBER)) {
//                    value= origSrcLineNumber;
//                }
//                if (key.equals(IMarker.CHAR_END) || key.equals(IMarker.CHAR_START))
//                    continue;
//                origSrcMarker.setAttribute(key, value);
//                
//                
//            }
//            origSrcMarker.setAttribute(IMarker.LINE_NUMBER, origSrcLineNumber);
//            
    		}
    	} catch (CoreException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
    	
    	//finally recreate the markers
    }

    // mmk: slightly modified from JDIDebugModel to support stratum line breakpoints
	/**
	 * Returns a Java line breakpoint that is already registered with the breakpoint
	 * manager for a type with the given name at the given line number in the given resource.
	 * 
	 * @param resource the resource
	 * @param typeName fully qualified type name
	 * @param lineNumber line number
	 * @return a Java line breakpoint that is already registered with the breakpoint
	 *  manager for a type with the given name at the given line number or <code>null</code>
	 * if no such breakpoint is registered
	 * @exception CoreException if unable to retrieve the associated marker
	 * 	attributes (line number).
	 * @since 3.1
	 */
	public static IJavaLineBreakpoint lineBreakpointExists(IResource resource, String typeName, int lineNumber) throws CoreException {
		String modelId= JDIDebugModel.getPluginIdentifier();
		String markerType= /*JavaLineBreakpoint.getMarkerType()*/"org.eclipse.jdt.debug.javaStratumLineBreakpointMarker";
		IBreakpointManager manager= DebugPlugin.getDefault().getBreakpointManager();
		IBreakpoint[] breakpoints= manager.getBreakpoints(modelId);
		for (int i = 0; i < breakpoints.length; i++) {
			if (!(breakpoints[i] instanceof IJavaLineBreakpoint)) {
				continue;
			}
			IJavaLineBreakpoint breakpoint = (IJavaLineBreakpoint) breakpoints[i];
			IMarker marker = breakpoint.getMarker();
			if (marker != null && marker.exists() && marker.getType().equals(markerType)) {
				String breakpointTypeName = breakpoint.getTypeName();
				if (
//					(breakpointTypeName.equals(typeName) || breakpointTypeName.startsWith(typeName + '$')) &&
					breakpoint.getLineNumber() == lineNumber &&
					resource.equals(marker.getResource())) {
						return breakpoint;
				}
			}
		}
		return null;
	}	
}

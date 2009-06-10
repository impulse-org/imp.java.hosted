/*******************************************************************************
* Copyright (c) 2009 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*******************************************************************************/
package org.eclipse.imp.java.hosted;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.imp.core.ErrorHandler;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.model.ICompilationUnit;
import org.eclipse.imp.model.IPathEntry;
import org.eclipse.imp.model.ISourceProject;
import org.eclipse.imp.model.ModelFactory;
import org.eclipse.imp.model.IPathEntry.PathEntryType;
import org.eclipse.imp.model.ModelFactory.IFactoryExtender;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class ProjectUtils {
    // TODO Rewrite the following as an IFactoryExtender, installed via ModelFactory.installExtender().
    // A Java-hosted language can then install it as needed on projects having the appropriate nature.

    public void addExtenderForJavaHostedProjects(Language lang) {
        ModelFactory.getInstance().installExtender(new IFactoryExtender() {
            public void extend(ISourceProject project) {
                initializeBuildPathFromJavaProject(project);
            }

            public void extend(ICompilationUnit unit) { }

            /**
             * Read the IJavaProject classpath configuration and populate the ISourceProject's
             * build path accordingly.
             */
            public void initializeBuildPathFromJavaProject(ISourceProject project) {
                IJavaProject javaProject= JavaCore.create(project.getRawProject());
                if (javaProject.exists()) {
                    try {
                        IClasspathEntry[] cpEntries= javaProject.getResolvedClasspath(true);
                        List<IPathEntry> buildPath= new ArrayList<IPathEntry>(cpEntries.length);
                        for(int i= 0; i < cpEntries.length; i++) {
                            IClasspathEntry entry= cpEntries[i];
                            IPathEntry.PathEntryType type;
                            IPath path= entry.getPath();

                            switch (entry.getEntryKind()) {
                            case IClasspathEntry.CPE_CONTAINER:
                                type= PathEntryType.CONTAINER;
                                break;
                            case IClasspathEntry.CPE_LIBRARY:
                                type= PathEntryType.ARCHIVE;
                                break;
                            case IClasspathEntry.CPE_PROJECT:
                                type= PathEntryType.PROJECT;
                                break;
                            case IClasspathEntry.CPE_SOURCE:
                                type= PathEntryType.SOURCE_FOLDER;
                                break;
                            default:
                         // case IClasspathEntry.CPE_VARIABLE:
                                throw new IllegalArgumentException("Encountered variable class-path entry: " + entry.getPath().toPortableString());
                            }
                            IPathEntry pathEntry= ModelFactory.createPathEntry(type, path);
                            buildPath.add(pathEntry);
                        }
                        project.setBuildPath(buildPath);
                    } catch (JavaModelException e) {
                        ErrorHandler.reportError(e.getMessage(), e);
                    }
                }
            }
        }, lang);
    }
}

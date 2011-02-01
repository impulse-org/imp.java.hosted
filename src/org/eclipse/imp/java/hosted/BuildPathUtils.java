/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
 *******************************************************************************/

package org.eclipse.imp.java.hosted;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * A set of helper methods for handling inclusion/exclusion clauses in a Java
 * project's classpath.
 */
public class BuildPathUtils {
    /**
     * @param filePath
     * @param srcEntry
     * @return true if the given file is excluded from the given IClasspathEntry
     */
    public static boolean isExcluded(final IPath filePath, final IClasspathEntry srcEntry) {
        final IPath relFilePath= filePath.makeRelativeTo(srcEntry.getPath());
        final IPath[] inclusionPatterns= srcEntry.getInclusionPatterns();
        if (inclusionPatterns != null && inclusionPatterns.length != 0) {
            boolean foundMatch= false;
            for(IPath pattern : inclusionPatterns) {
                if (matches(relFilePath, pattern)) {
                    foundMatch= true;
                    break;
                }
            }
            if (!foundMatch) {
                return true;
            }
        }
        final IPath[] exclusionPatterns= srcEntry.getExclusionPatterns();
        if (exclusionPatterns != null && exclusionPatterns.length != 0) {
            for(IPath pattern : exclusionPatterns) {
                if (matches(relFilePath, pattern)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @param filePath
     * @param project
     * @return true if the given file is excluded from the source path of the given project
     */
    public static boolean isExcluded(IPath filePath, final IJavaProject project) {
        try {
            // --- Determine the classpath source entry that corresponds to file.
            if (project == null) {
                return true; // --- If there is no java project associated with this file, then the file must be
                             // excluded.
            }
            // --- If filePath is not relative to the workspace, make it so.
            IPath workspace= project.getProject().getLocation().removeLastSegments(1);
            if (workspace.isPrefixOf(filePath)) {
                filePath= filePath.makeRelativeTo(workspace);
            }
            for(final IClasspathEntry cpEntry : project.getRawClasspath()) {
                if (cpEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE && cpEntry.getPath().isPrefixOf(filePath) && !isExcluded(filePath, cpEntry)) {
                    return false;
                }
            }
        } catch (JavaModelException e) {
            return true;
        }
        return true;
    }

    /**
     * This method takes a file and a pattern and determines if there is a match. The patterns have the same format as
     * ANT patterns:
     * <ul>
     *   <li> '*' matches zero or more characters.
     *   <li> '?' matches one character.
     *   <li> '/' is used to separate folders: This means the first segment in the pattern is matched against the most
     *   outer folder name in the path to match, the second segment with the second, and so on.
     *   <li> '**' matches any number of folders.
     * </ul>
     * 
     * @param file The file we are matching.
     * @param pattern The pattern to match.
     * @return true if there is a match.
     */
    private static boolean matches(final IPath path, final IPath pattern) {
        if (path.equals(pattern)) {
            return true;
        }
        if (pattern.segmentCount() == 1 && pattern.segment(0).equals("**")) {
            return true;
        }
        if (path.isEmpty() && !pattern.isEmpty()) { // --- In this case, there is a match only if pattern is of the form
                                                    // **/**/**...
            if (!pattern.segment(0).equals("**"))
                return false;
            return matches(path, pattern.removeFirstSegments(1));
        }
        if (!path.isEmpty() && pattern.isEmpty()) {
            return false;
        }

        if (pattern.isPrefixOf(path)) {
            return true;
        }
        String pathFirst= path.segment(0);
        String patternFirst= pattern.segment(0);
        if (patternFirst.equals("**")) {
            IPath patternNext= pattern.removeFirstSegments(1);
            for(int i= 0; i < path.segmentCount(); i++) {
                if (matches(path.removeFirstSegments(i), patternNext)) {
                    return true;
                }
            }
            return false;
        } else {
            if (!segMatches(pathFirst, patternFirst)) {
                return false;
            }
            return matches(path.removeFirstSegments(1), pattern.removeFirstSegments(1));
        }
    }

    /**
     * This method determines whether 2 string segments match. We assume that the pattern does not contain '**'.
     * 
     * @param seg The segment to match
     * @param pattern The pattern to match
     * @return true if the segment and pattern match
     */
    private static boolean segMatches(final String seg, final String pattern) {
        if (seg.equals(pattern) || pattern.equals("*")) {
            return true;
        }
        if (seg.equals("") && !pattern.equals("")) { // --- In this case, there is a match only if pattern is of the
                                                     // form ***...
            if (pattern.charAt(0) != '*') {
                return false;
            }
            return segMatches(seg, pattern.substring(1));
        }
        if (!seg.equals("") && pattern.equals("")) {
            return false;
        }
        char patternFirst= pattern.charAt(0);
        char segFirst= seg.charAt(0);
        if (patternFirst == '*') {
            String patternNext= pattern.substring(1);
            for(int i= 0; i < seg.length(); i++) {
                if (segMatches(seg.substring(i), patternNext)) {
                    return true;
                }
            }
            return false;
        }
        if (patternFirst == '?') {
            return segMatches(seg.substring(1), pattern.substring(1));
        } else {
            if (patternFirst != segFirst) {
                return false;
            }
            return segMatches(seg.substring(1), pattern.substring(1));
        }
    }
}

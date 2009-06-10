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

/*
 * Created on Feb 6, 2006
 */
package org.eclipse.imp.java.hosted.wizards.fields;

/**
 * Change listener used by <code>StringButtonDialogField</code>
 */
public interface IStringButtonAdapter {

    void changeControlPressed(DialogField field);

}

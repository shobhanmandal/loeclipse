/*************************************************************************
 *
 * $RCSfile: RegistrationHelper.java,v $
 *
 * $Revision: 1.3 $
 *
 * last change: $Author: cedricbosdo $ $Date: 2007/12/07 07:30:55 $
 *
 * The Contents of this file are made available subject to the terms of
 * the GNU Lesser General Public License Version 2.1
 *
 * Sun Microsystems Inc., October, 2000
 *
 *
 * GNU Lesser General Public License Version 2.1
 * =============================================
 * Copyright 2000 by Sun Microsystems, Inc.
 * 901 San Antonio Road, Palo Alto, CA 94303, USA
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License version 2.1, as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 * MA 02111-1307 USA
 *
 * The Initial Developer of the Original Code is: Sun Microsystems, Inc..
 *
 * Copyright: 2002 by Sun Microsystems, Inc.
 *
 * All Rights Reserved.
 *
 * Contributor(s): Cedric Bosdonnat
 *
 *
 ************************************************************************/
package org.libreoffice.ide.eclipse.java.registration;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.libreoffice.ide.eclipse.core.PluginLogger;
import org.libreoffice.ide.eclipse.core.model.IUnoidlProject;
import org.libreoffice.ide.eclipse.java.utils.TemplatesHelper;
import org.eclipse.swt.widgets.Display;

/**
 * This class provides utility methods to generate the class and files needed
 * by the UNO services implementation registration.
 */
public abstract class RegistrationHelper {

    public static final String CLASS_FILENAME = "RegistrationHandler"; //$NON-NLS-1$

    /**
     * Creates all the necessary files for the java registration of UNO services
     * implementations to the <code>regcomp</code> tool.
     *
     * @param pProject the project where to create the registration handler
     */
    public static void generateFiles(IUnoidlProject pProject) {
        // Copy the RegistrationHandler.java.tpl file
        TemplatesHelper.copyTemplate(pProject, CLASS_FILENAME + TemplatesHelper.JAVA_EXT,
            RegistrationHelper.class, new String());

        // Create the empty RegistrationHandler.classes file
        ByteArrayInputStream empty = new ByteArrayInputStream(new byte[0]);
        try {
            File listFile = getClassesListFile(pProject).getLocation().toFile();
            listFile.createNewFile();
        } catch (IOException e) {
            PluginLogger.error(Messages.getString("RegistrationHelper.WriteClassesListError"), e); //$NON-NLS-1$
        } finally {
            try {
                empty.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Add a UNO service implementation to the list of the project ones.
     *
     * @param pProject the project where to add the implementation
     * @param pImplName the fully qualified name of the implementation to add,
     *         eg: <code>org.libreoffice.comp.test.MyServiceImpl</code>
     */
    public static void addImplementation(IUnoidlProject pProject, String pImplName) {
        Vector<String> classes = readClassesList(pProject);
        if (!classes.contains(pImplName)) {
            classes.add(pImplName);
        }
        writeClassesList(pProject, classes);
    }

    /**
     * remove a UNO service implementation from the list of the project ones.
     *
     * @param pProject the project where to remove the implementation
     * @param pImplName the fully qualified name of the implementation to remove,
     *         eg: <code>org.libreoffice.comp.test.MyServiceImpl</code>
     */
    public static void removeImplementation(IUnoidlProject pProject, String pImplName) {
        Vector<String> classes = readClassesList(pProject);
        classes.remove(pImplName);
        writeClassesList(pProject, classes);
    }

    /**
     * Computes the registration class name for the given Uno project.
     *
     * The registration class name is generally
     * <code>&lt;COMPANY.PREFIX&gt;.&lt;OUTPUTEXT&gt;.RegistrationHandler</code>.
     *
     * @param pProject the project for which to compute the class name
     * @return the registration class name
     */
    public static String getRegistrationClassName(IUnoidlProject pProject) {
        // Compute the name of the main implementation class
        String implPkg = pProject.getCompanyPrefix() + "." + pProject.getOutputExtension(); //$NON-NLS-1$
        return implPkg + "." + CLASS_FILENAME; //$NON-NLS-1$
    }

    /**
     * Read the implementation classes list of the given UNO project.
     *
     * @param pProject the UNO project
     *
     * @return the implementation classes list
     */
    public static Vector<String> readClassesList(IUnoidlProject pProject) {
        Vector<String> classes = new Vector<String>();

        IFile list = getClassesListFile(pProject);
        File file = list.getLocation().toFile();
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                PluginLogger.error(Messages.getString("RegistrationHelper.WriteClassesListError"), e); //$NON-NLS-1$
            }
        }

        // First read all the lines
        FileInputStream in = null;
        BufferedReader reader = null;
        try {
            in = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(in));

            String line = reader.readLine();
            while (line != null) {
                if (!classes.contains(line)) {
                    classes.add(line);
                }
                line = reader.readLine();
            }
        } catch (Exception e) {

        } finally {
            try {
                reader.close();
                in.close();
            } catch (Exception e) {
            }
        }
        return classes;
    }

    /**
     * Writes the implementation classes list to the UNO project.
     *
     * @param pProject the project for which to write the list
     * @param pClasses the classes to write
     */
    private static void writeClassesList(IUnoidlProject pProject, Vector<String> pClasses) {
        IFile list = getClassesListFile(pProject);
        File file = list.getLocation().toFile();

        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            for (String implClass : pClasses) {
                writer.append(implClass + "\n"); //$NON-NLS-1$
            }
        } catch (IOException e) {
            PluginLogger.error(Messages.getString("RegistrationHelper.WriteClassesListError"), e); //$NON-NLS-1$
        } finally {
            try {
                writer.close();
            } catch (Exception e) {
            }
        }

        // update the list file in the workspace
        new FileRefreshJob(list).schedule();
    }

    /**
     * Get the classes list file for the given UNO project.
     *
     * @param pProject the UNO project to get the list file from
     *
     * @return the implementation classes file of the project.
     */
    private static IFile getClassesListFile(IUnoidlProject pProject) {
        // Get the path where to place the class and the implementations list
        IPath relPath = pProject.getImplementationPath();
        IFolder dest = pProject.getFolder(relPath);
        return dest.getFile("RegistrationHandler.classes"); //$NON-NLS-1$
    }

    /**
        * Check if the RegistrationHandler.classes file is empty
        * <code>org.libreoffice.ide.eclipse.java.JavaResourceDeltaVisitor</code>
        *
        * @param pProject the project where to check empty file
        */
    public static void isFileEmpty(IUnoidlProject pProject) {
        IFile list = getClassesListFile(pProject);
        File file = list.getLocation().toFile();
        boolean checkFileEmpty = true;
        FileInputStream in = null;
        BufferedReader reader = null;
        try {
            in = new FileInputStream(file);
            reader = new BufferedReader(new InputStreamReader(in));
            if (reader.readLine() == null) {
                checkFileEmpty = true;
            } else {
                checkFileEmpty = false;
            }
        } catch (Exception e) {
            checkFileEmpty = true;

        } finally {
            try {
                reader.close();
                in.close();
            } catch (Exception e) {
            }
        }
        if (checkFileEmpty) {
            PluginLogger.error(Messages.getString("RegistrationHelper.RegistrationHandlerEmptyClassError"));
            MessageDialog.openError(Display.getDefault().getActiveShell(), "Error",
                Messages.getString("RegistrationHelper.RegistrationHandlerEmptyClassError"));
        }
    }
}

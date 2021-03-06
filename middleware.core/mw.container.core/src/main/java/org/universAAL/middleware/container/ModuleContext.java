/*
        Copyright 2011-2014 Fraunhofer IGD, http://www.igd.fraunhofer.de
        Fraunhofer-Gesellschaft - Institute for Computer Graphics Research

        Copyright 2007-2014 CNR-ISTI, http://isti.cnr.it
        Institute of Information Science and Technologies
        of the Italian National Research Council

        See the NOTICE file distributed with this work for additional
        information regarding copyright ownership

        Licensed under the Apache License, Version 2.0 (the "License");
        you may not use this file except in compliance with the License.
        You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

        Unless required by applicable law or agreed to in writing, software
        distributed under the License is distributed on an "AS IS" BASIS,
        WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        See the License for the specific language governing permissions and
        limitations under the License.
 */
package org.universAAL.middleware.container;

import java.io.File;

/**
 * Represents the container-specific context of modules that either build up the
 * universAAL platform or use this platform. Its design is inspired by the
 * <a href="http://en.wikipedia.org/wiki/OSGi#Life-cycle"> OSGi life-cycle</a>
 * as well as container-specific general-purpose code in the universAAL
 * middleware, and the requirements of the Space Admin Gateway with regard to
 * node-level admin.
 *
 * @author mtazari
 * @author <a href="mailto:stefano.lenzi@isti.cnr.it">Stefano Lenzi</a>
 */
public interface ModuleContext {
	/**
	 * Returns true if (1) the associated module has a status equivalent to the
	 * OSGi ACTIVE , (2) it fulfills all prerequisites for being started, and
	 * (3) the given requester is allowed to start it. Otherwise, it returns
	 * false.
	 *
	 * Note: Confer the OSGi Bundle State <code>RESOLVED</code> with regard to
	 * the first two conditions above.
	 */
	public boolean canBeStarted(ModuleContext requester);

	/**
	 * Returns true if (1) the associated module would respond to a
	 * <code>stop</code> request (cf. the OSGi Bundle State <code>ACTIVE</code>
	 * ), and (2) the given requester is allowed to stop it. Otherwise, it
	 * returns false.
	 */
	public boolean canBeStopped(ModuleContext requester);

	/**
	 * Returns true if (1) the associated module is in a state equivalent to the
	 * OSGi Bundle States <code>INSTALLED</code> or <code>RESOLVED</code>, and
	 * (2) the given requester is allowed to uninstall it. Otherwise, it returns
	 * false.
	 */
	public boolean canBeUninstalled(ModuleContext requester);

	/**
	 * Returns the value associated with a certain attribute that has been set
	 * previously by a component that has access to this module context. It
	 * provides means for specifying container-specific additional info that has
	 * not been introduced by this interface.
	 */
	public Object getAttribute(String attrName);

	/**
	 * Returns the {@link Container} object that contains the module associated
	 * with this ModuleContext.
	 */
	public Container getContainer();

	public String getID();

	/**
	 * Returns the list of all config files associated with this module that
	 * follow the container conventions and have been registered previously, if
	 * the given requester is allowed to access them.
	 *
	 * @deprecated
	 */
	public File[] listConfigFiles(ModuleContext requester);

	/**
	 * Provides a standard way for using container-specific loggers, in this
	 * case for logging debug messages.
	 *
	 * @param tag
	 *            the log tag, for example the tag of android.util.Log
	 * @param message
	 *            the log message
	 * @param t
	 *            An optional {@link Throwable} object like an exception that
	 *            might have caused the log request
	 */
	public void logDebug(String tag, String message, Throwable t);

	/**
	 * Provides a standard way for using container-specific loggers, in this
	 * case for logging error messages.
	 *
	 * @param tag
	 *            the log tag, for example the tag of android.util.Log
	 * @param message
	 *            the log message
	 * @param t
	 *            An optional {@link Throwable} object like an exception that
	 *            might have caused the log request
	 */
	public void logError(String tag, String message, Throwable t);

	/**
	 * Provides a standard way for using container-specific loggers, in this
	 * case for logging info messages.
	 *
	 * @param tag
	 *            the log tag, for example the tag of android.util.Log
	 * @param message
	 *            the log message
	 * @param t
	 *            An optional {@link Throwable} object like an exception that
	 *            might have caused the log request
	 */
	public void logInfo(String tag, String message, Throwable t);

	/**
	 * Provides a standard way for using container-specific loggers, in this
	 * case for logging warnings.
	 *
	 * @param tag
	 *            the log tag, for example the tag of android.util.Log
	 * @param message
	 *            the log message
	 * @param t
	 *            An optional {@link Throwable} object like an exception that
	 *            might have caused the log request
	 */
	public void logWarn(String tag, String message, Throwable t);

	/**
	 * Provides a standard way for using container-specific loggers, in this
	 * case for logging trace messages.
	 *
	 * @param tag
	 *            the log tag, for example the tag of android.util.Log
	 * @param message
	 *            the log message
	 * @param t
	 *            An optional {@link Throwable} object like an exception that
	 *            might have caused the log request
	 */
	public void logTrace(String tag, String message, Throwable t);

	/** Is the logger instance enabled for the ERROR level? */
	public boolean isLogErrorEnabled();

	/** Is the logger instance enabled for the WARN level? */
	public boolean isLogWarnEnabled();

	/** Is the logger instance enabled for the INFO level? */
	public boolean isLogInfoEnabled();

	/** Is the logger instance enabled for the DEBUG level? */
	public boolean isLogDebugEnabled();

	/** Is the logger instance enabled for the TRACE level? */
	public boolean isLogTraceEnabled();

	/**
	 * Modules can use this method of their context to enrich it with info about
	 * those config files of them that follow the container conventions. Only
	 * registered config files are supposed to be editable by standard admin
	 * tools that are planned to be developed by universAAL.
	 *
	 * @deprecated
	 */
	public void registerConfigFile(Object[] configFileParams);

	/**
	 * Concrete containers can use this possibility to enrich the module context
	 * with container-specific additional info that has not been introduced by
	 * this interface.
	 */
	public void setAttribute(String attrName, Object attrValue);

	/**
	 * An authorized requester can use this method to start the current module.
	 * Returns true if {@link #canBeStarted(ModuleContext)} returns true AND the
	 * start action does not lead to any unexpected problem, otherwise false.
	 */
	public boolean start(ModuleContext requester);

	/**
	 * An authorized requester can use this method to stop the current module.
	 * Returns true if {@link #canBeStopped(ModuleContext)} returns true AND the
	 * stop action does not lead to any unexpected problem, otherwise false.
	 */
	public boolean stop(ModuleContext requester);

	/**
	 * An authorized requester can use this method to uninstall the current
	 * module. Returns true if {@link #canBeUninstalled(ModuleContext)} returns
	 * true AND the uninstall action does not lead to any unexpected problem,
	 * otherwise false.
	 */
	public boolean uninstall(ModuleContext requester);

	/**
	 * Return the current value of an property or properties that is defined in
	 * this container
	 *
	 * @param name
	 *            the name of the property requested
	 * @return the current value of the requested property, it returns
	 *         <code>null</code> if no property set
	 * @since 1.3.2
	 */
	public Object getProperty(String name);

	/**
	 * Return the current value of an attribute or properties that is defined in
	 * this container
	 *
	 * @param name
	 *            the name of the property requested
	 * @param def
	 *            the default value to return in case that the property is not
	 *            set
	 * @return the current value of the requested property
	 * @since 1.3.2
	 */
	public Object getProperty(String name, Object def);

	/**
	 * Get the value of an entry from the manifest of this module.
	 *
	 * @param name
	 *            the name of the manifest entry.
	 * @return the value of the manifest entry.
	 * @since 2.0.1
	 */
	public String getManifestEntry(String name);

	/**
	 * Get the value of an entry from a custom manifest of this module.
	 *
	 * @param manifest
	 *            identification of the custom manifest.
	 * @param name
	 *            the name of the manifest entry.
	 * @return the value of the manifest entry.
	 * @since 2.0.1
	 */
	public String getManifestEntry(String manifest, String name);

	/**
	 * Get the Configuration Folder for this module.
	 *
	 * @return The {@link File} pointing to the correct configuration folder of
	 *         this module, The folder will be created for the module to use
	 *         directly.
	 * @since 3.1.1
	 */
	public File getConfigHome();

	/**
	 * Get the Data folder for this module, the general storage space where data
	 * generated by the module should be stored. This folder may be cleared by
	 * system Admin.
	 *
	 * @return a File pointing to the corresponding reserved folder for data for
	 *         this module, The folder will be created for the module to use
	 *         directly.
	 * @since 3.1.1
	 */
	public File getDataFolder();
}

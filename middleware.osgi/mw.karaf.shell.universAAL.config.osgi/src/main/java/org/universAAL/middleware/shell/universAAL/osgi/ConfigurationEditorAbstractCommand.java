/*******************************************************************************
 * Copyright 2014 Universidad Politécnica de Madrid
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.universAAL.middleware.shell.universAAL.osgi;

import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.osgi.framework.ServiceReference;
import org.universAAL.middleware.managers.api.ConfigurationEditor;

/**
 * @author amedrano
 *
 */
public abstract class ConfigurationEditorAbstractCommand extends
	OsgiCommandSupport {

    protected ConfigurationEditor configurationEditor;

    /**
     * 
     */
    public ConfigurationEditorAbstractCommand() {
	log.debug("Initialising...");
	ServiceReference ref = bundleContext
		.getServiceReference(ConfigurationEditor.class.getName());
	if (ref != null) {
	    configurationEditor = (ConfigurationEditor) bundleContext.getService(ref);
	} else {
	    throw new IllegalArgumentException("unable to locate the Configuration Editor...");
	}
    }

}

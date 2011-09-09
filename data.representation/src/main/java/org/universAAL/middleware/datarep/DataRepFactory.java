/*	
	Copyright 2007-2014 Fraunhofer IGD, http://www.igd.fraunhofer.de
	Fraunhofer-Gesellschaft - Institut f�r Graphische Datenverarbeitung
	
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
package org.universAAL.middleware.datarep;

import org.universAAL.middleware.rdf.PropertyPath;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.rdf.ResourceRegistry;
import org.universAAL.middleware.rdf.impl.ResourceFactoryImpl;

/**
 * 
 * @author Carsten Stockloew
 *
 */
public class DataRepFactory extends ResourceFactoryImpl {
    
    DataRepFactory() {
	// The classes derived from Resource but not ManagedIndividual do not
	// have ontological information, there is no OntClassInfo for them, so
	// the registration of the factory is not done automatically
	// -> we have to do the registration here.
	
	ResourceRegistry.getInstance().registerResourceFactory(PropertyPath.TYPE_PROPERTY_PATH, this, 0);
    }

    public Resource createInstance(String classURI, String instanceURI,
	    int factoryIndex) {
	
	switch (factoryIndex) {
	case 0:
	    return new PropertyPath(instanceURI);
	}
	
	return null;
    }

}
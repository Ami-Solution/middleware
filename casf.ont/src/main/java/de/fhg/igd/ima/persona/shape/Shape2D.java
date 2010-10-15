/*
	Copyright 2008-2010 Fraunhofer IGD, http://www.igd.fraunhofer.de
	Fraunhofer-Gesellschaft - Institute of Computer Graphics Research 
	
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
package de.fhg.igd.ima.persona.shape;

import org.persona.middleware.TypeMapper;
import org.persona.ontology.expr.Restriction;
import org.persona.platform.casf.ontology.location.PlaceType;
import org.persona.ontology.ComparableIndividual;

import de.fhg.igd.ima.persona.location.position.CoordinateSystem;
import de.fhg.igd.ima.persona.location.position.Point;

/**
 * 
 * @author chwirth
 *
 */

public abstract class Shape2D extends Shape {
	
	public static final String MY_URI;
		
	static {
		MY_URI = PERSONA_SHAPE_NAMESPACE + "Shape2D";
		register(Shape2D.class);
	}
	
	
	/**
	 * Creates a Shape object
	 * @param uri the object URI
	 */
	public Shape2D(String uri,CoordinateSystem system) {
		super(uri,system);
	}
	
	/**
	 * Creates a Shape object
	 */
	public Shape2D(CoordinateSystem system) {
		super(system);
	}
	
	public Shape2D() {
		super();
	}
	
	public Shape2D(String uri) {
		super(uri);
	}
		
	/**
	 * Returns a human readable description on the essence of this ontology class.
	 */
	public static String getRDFSComment() {
		return "The root class for all 2d shapes.";
	}
	
	/**
	 * Returns a label with which this ontology class can be introduced to human users.
	 */
	public static String getRDFSLabel() {
		return "Shape2D";
	}
			
}

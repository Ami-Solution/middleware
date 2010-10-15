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
package org.persona.ontology;

import org.persona.ontology.expr.Restriction;

/**
 * @author mtazari
 *
 */
public class AccessImpairment extends ManagedIndividual {
	public static final String MY_URI;
	public static final String PROP_IMPAIRMENT_LEVEL;
	
	static {
		MY_URI = PERSONA_VOCABULARY_NAMESPACE + "AccessImpairment";
		PROP_IMPAIRMENT_LEVEL = PERSONA_VOCABULARY_NAMESPACE + "impairmentLevel";
		register(AccessImpairment.class);
	}
	
	public static Restriction getClassRestrictionsOnProperty(String propURI) {
		if (PROP_IMPAIRMENT_LEVEL.equals(propURI))
			return Restriction.getAllValuesRestrictionWithCardinality(
					propURI, LevelRating.MY_URI, 1, 1);
		return ManagedIndividual.getClassRestrictionsOnProperty(propURI);
	}
	
	public static String getRDFSComment() {
		return "General concept for representing impairments of the users in accessing the PERSONA system.";
	}
	
	public static String getRDFSLabel() {
		return "Access Impairment";
	}
	
	public static String[] getStandardPropertyURIs() {
		return new String[] {
				PROP_IMPAIRMENT_LEVEL
		};
	}
	
	/**
	 * The constructor for (de-)serializers.
	 */
	public AccessImpairment() {
		super();
	}
	
	/**
	 * The constructor for use by applications.
	 */
	public AccessImpairment(LevelRating impairmentLevel) {
		super();
		props.put(PROP_IMPAIRMENT_LEVEL, impairmentLevel);
	}
	
	public LevelRating getImpaimentLevel() {
		return (LevelRating) props.get(PROP_IMPAIRMENT_LEVEL);
	}

	/* (non-Javadoc)
	 * @see org.persona.ontology.ManagedIndividual#getPropSerializationType(java.lang.String)
	 */
	public int getPropSerializationType(String propURI) {
		return (PROP_IMPAIRMENT_LEVEL.equals(propURI))?
			PROP_SERIALIZATION_REDUCED : PROP_SERIALIZATION_OPTIONAL;
	}

	/* (non-Javadoc)
	 * @see org.persona.ontology.ManagedIndividual#isWellFormed()
	 */
	public boolean isWellFormed() {
		return props.containsKey(PROP_IMPAIRMENT_LEVEL);
	}
}

/*
	Copyright 2008-2010 ITACA-TSB, http://www.tsb.upv.es
	Instituto Tecnologico de Aplicaciones de Comunicacion 
	Avanzadas - Grupo Tecnologias para la Salud y el 
	Bienestar (TSB)
	
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
package org.persona.platform.casf.ontology.medication;

import org.persona.ontology.expr.Restriction;
import org.persona.platform.casf.ontology.PhysicalContainer;

/**
 * @author <a href="mailto:alfiva@itaca.upv.es">Alvaro Fides Valero</a>
 *
 */
public class PillDispenser extends PhysicalContainer {
	public static final String MY_URI;
	
	static{
		MY_URI=DrugPackage.PERSONA_MEDICATION_NAMESPACE+"PillDispenser";
		register(DrugPackage.class);
	}
	
	public static Restriction getClassRestrictionsOnProperty(String propURI) {
		return Restriction.getPropertyBanningRestriction(propURI);
	}
	
	public PillDispenser(){
	}
	
	public PillDispenser(String uri){
		super(uri);
	}
	
	public static String getRDFSComment() {
		return "An automatic pill dispenser, container of drug packages";
	}
	
	public static String getRDFSLabel() {
		return "Pill Dispenser";
	}

	public boolean isWellFormed() {
		return true;
	}


}

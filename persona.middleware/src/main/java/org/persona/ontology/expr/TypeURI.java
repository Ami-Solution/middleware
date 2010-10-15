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
package org.persona.ontology.expr;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.persona.middleware.PResource;
import org.persona.middleware.TypeMapper;
import org.persona.middleware.service.process.ProcessParameter;
import org.persona.ontology.ManagedIndividual;
import org.persona.ontology.PClassExpression;

/**
 * @author mtazari
 *
 */
public class TypeURI extends PClassExpression {
	static {
		register(TypeURI.class, null, null, null);
	}
	
	public static TypeURI asTypeURI(Object o) {
		if (o == null  ||  o instanceof TypeURI)
			return (TypeURI) o;
		
		if (o instanceof PResource  &&  !((PResource) o).isAnon()) {
			java.util.Enumeration e = ((PResource) o).getPropertyURIs();
			if (e != null  &&  e.hasMoreElements()) {
				if (PROP_RDF_TYPE.equals(e.nextElement())  &&  !e.hasMoreElements()) {
					Object tmp = ((PResource) o).getProperty(PROP_RDF_TYPE);
					if (tmp instanceof List  &&  ((List) tmp).size() == 1)
						tmp = ((List) tmp).get(0);
					if (tmp instanceof PResource)
						tmp = ((PResource) tmp).getURI();
					if (OWL_CLASS.equals(tmp))
						return new TypeURI(((PResource) o).getURI(), false);
					else if (tmp == null)
						if (ManagedIndividual.isRegisteredClassURI(((PResource) o).getURI()))
							return new TypeURI(((PResource) o).getURI(), false);
						else if (TypeMapper.isRegisteredDatatypeURI(((PResource) o).getURI()))
							return new TypeURI(((PResource) o).getURI(), true);
				}		
			} else if (TypeMapper.isRegisteredDatatypeURI(((PResource) o).getURI()))
				return new TypeURI(((PResource) o).getURI(), true);
			else if (ManagedIndividual.isRegisteredClassURI(((PResource) o).getURI()))
				return new TypeURI(((PResource) o).getURI(), false);
		} else if (o instanceof String)
			if (TypeMapper.isRegisteredDatatypeURI((String) o))
				return new TypeURI((String) o, true);
			else if (ManagedIndividual.isRegisteredClassURI((String) o))
				return new TypeURI((String) o, false);
		
		return null;
	}

	public TypeURI(String uri, boolean isDatatypeURI) {
		super(uri);
		if (isDatatypeURI)
			props.remove(PROP_RDF_TYPE);
	}
	
	/**
	 * No {@link PClassExpression} instances are stored in this class, so we do not need to clone.
	 */
	public PClassExpression copy() {
		return this;
	}
	
	public String[] getNamedSuperclasses() {
		return new String[] {getURI()};
	}
	
	public PClassExpression getRestrictionOnProperty(String propURI) {
		return ManagedIndividual.getClassRestrictionsOnProperty(uri, propURI);
	}
	
	public Object[] getUpperEnumeration() {
		ManagedIndividual[] answer = ManagedIndividual.getEnumerationMembers(getURI());
		return (answer == null)? new Object[0] : answer;
	}
	
	public boolean hasMember(Object value, Hashtable context) {
		if (uri.equals(TYPE_OWL_THING))
			return true;
		
		// TODO: 1. could variables be used in constructing class names?
		//       2. what if variables are used not only as values but also within values
		if (value instanceof Collection) {
			for (Iterator i = ((Collection) value).iterator();  i.hasNext();) {
				Object val = ProcessParameter.resolveVarRef(i.next(), context);
				if (val == null  ||  !ManagedIndividual.checkMembership(uri, val))
					return false;
			}
			return true;
		} else {
			value = ProcessParameter.resolveVarRef(value, context);
			return value != null  &&  ManagedIndividual.checkMembership(uri, value);
		}
	}

	public boolean matches(PClassExpression subtype, Hashtable context) {
		if (uri.equals(TYPE_OWL_THING))
			return subtype != null;
		
		if (subtype instanceof Enumeration)
			return ((Enumeration) subtype).hasSupertype(this, context);

		if (subtype instanceof TypeURI)
			return ManagedIndividual.checkCompatibility(uri, ((TypeURI) subtype).uri);

		if (subtype instanceof Union) {
			Hashtable cloned = (context == null)? null : (Hashtable) context.clone();
			for (Iterator i = ((Union) subtype).types(); i.hasNext();)
				if (!matches((PClassExpression) i.next(), cloned))
					return false;
			synchronize(context, cloned);
			return true;
		}
		
		if (subtype instanceof Intersection) {
			for (Iterator i = ((Intersection) subtype).types(); i.hasNext();)
				if (matches((PClassExpression) i.next(), context))
					return true;
			// TODO: there is still a chance to return true...
			// so fall through to the general case at the end
		} else if (subtype instanceof Restriction) {
			Restriction r = ManagedIndividual.getClassRestrictionsOnProperty(uri,
					((Restriction) subtype).getOnProperty());
			return r == null  ||  r.matches(subtype, context);
		}
		// a last try
		Object[] members = (subtype == null)? null : subtype.getUpperEnumeration();
		if (members != null  &&  members.length > 0) {
			Hashtable cloned = (context == null)? null : (Hashtable) context.clone();
			for (int i=0; i<members.length; i++)
				if (!hasMember(members[i], cloned))
					return false;
			synchronize(context, cloned);
			return true;
		}
		// in case of complements, it is unlikely and otherwise difficult to decide
		return false;
	}

	public boolean isDisjointWith(PClassExpression other, Hashtable context) {
		if (uri.equals(TYPE_OWL_THING))
			return false;
		
		if (other instanceof Complement)
			return ((Complement) other).getComplementedClass().matches(this, context);
		
		if (other instanceof TypeURI)
			return !ManagedIndividual.checkCompatibility(uri, ((TypeURI) other).uri)
			    && !ManagedIndividual.checkCompatibility(((TypeURI) other).uri, uri);
		
		if (other instanceof Restriction) {
			Restriction r = ManagedIndividual.getClassRestrictionsOnProperty(uri,
					((Restriction) other).getOnProperty());
			return r != null  &&  ((Restriction) other).isDisjointWith(r, context);
		}
		
		if (other != null)
			return other.isDisjointWith(this, context);

		return false;
	}
	
	public boolean isWellFormed() {
		return true;
	}

	public void setProperty(String propURI, Object o) {
		// ignore
	}

}

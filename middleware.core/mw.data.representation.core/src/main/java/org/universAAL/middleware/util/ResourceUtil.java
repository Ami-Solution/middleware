/*
	Copyright 2007-2014 Fraunhofer IGD, http://www.igd.fraunhofer.de
	Fraunhofer-Gesellschaft - Institute for Computer Graphics Research

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
package org.universAAL.middleware.util;

import java.util.Iterator;

import org.universAAL.middleware.owl.AllValuesFromRestriction;
import org.universAAL.middleware.owl.ExactCardinalityRestriction;
import org.universAAL.middleware.owl.HasValueRestriction;
import org.universAAL.middleware.owl.Intersection;
import org.universAAL.middleware.owl.MaxCardinalityRestriction;
import org.universAAL.middleware.owl.MinCardinalityRestriction;
import org.universAAL.middleware.owl.TypeExpression;
import org.universAAL.middleware.owl.TypeURI;
import org.universAAL.middleware.rdf.Variable;

/**
 * Some helper methods for debugging.
 *
 * @author Carsten Stockloew
 */
public final class ResourceUtil {

	/** Private constructor. Use static methods. */
	private ResourceUtil() {
	}

	/**
	 * Create a String representation from a given {@link TypeExpression}. Calls
	 * {@link #toString(TypeExpression, String)} with null as prefix.
	 *
	 * Note: not all expressions are realized yet.
	 *
	 * @param e
	 *            the expression.
	 * @return the String representation.
	 */
	public static String toString(TypeExpression e) {
		return toString(e, null);
	}

	/**
	 * Create a String representation from a given {@link TypeExpression}.
	 *
	 * Note: not all expressions are realized yet.
	 *
	 * @param e
	 *            the expression.
	 * @param prefix
	 *            a prefix for each line. Can be null.
	 * @return the String representation.
	 */
	public static String toString(TypeExpression e, String prefix) {
		if (prefix == null)
			prefix = "";

		if (e instanceof AllValuesFromRestriction) {
			AllValuesFromRestriction all = (AllValuesFromRestriction) e;
			Object o = all.getConstraint();
			String prefix2 = prefix + "AllValuesFrom (for property " + all.getOnProperty() + ")";
			if (o instanceof TypeExpression)
				return prefix2 + "\n" + ResourceUtil.toString((TypeExpression) o, prefix + "  ");
			else
				return prefix2 + " - unknown: " + o.getClass().getName() + "\n";
		} else if (e instanceof TypeURI) {
			return prefix + "Type: " + ((TypeURI) e).getURI() + "\n";
		} else if (e instanceof HasValueRestriction) {
			HasValueRestriction has = (HasValueRestriction) e;
			Object o = has.getConstraint();
			String prefix2 = prefix + "HasValue (for property " + has.getOnProperty() + ")";
			if (o instanceof TypeExpression)
				return prefix2 + "\n" + ResourceUtil.toString((TypeExpression) o, prefix + "  ");
			else {
				if (Variable.isVarRef(o)) {
					// input variable
					return prefix2 + "\n" + prefix + "  Variable\n";
				} else {
					// TODO: data values (e.g. int)
					return prefix2 + "\n" + prefix + "  - unknown: " + o.getClass().getName() + "\n";
				}
			}
		} else if (e instanceof ExactCardinalityRestriction) {
			ExactCardinalityRestriction c = (ExactCardinalityRestriction) e;
			return prefix + "ExactCardinality " + c.getValue() + "(for property " + c.getOnProperty() + ")" + "\n";
		} else if (e instanceof MinCardinalityRestriction) {
			MinCardinalityRestriction c = (MinCardinalityRestriction) e;
			return prefix + "MinCardinality " + c.getValue() + "(for property " + c.getOnProperty() + ")" + "\n";
		} else if (e instanceof MaxCardinalityRestriction) {
			MaxCardinalityRestriction c = (MaxCardinalityRestriction) e;
			return prefix + "MaxCardinality " + c.getValue() + "(for property " + c.getOnProperty() + ")" + "\n";
		} else if (e instanceof Intersection) {
			Intersection in = (Intersection) e;
			String ret = prefix + "Intersection\n";
			Iterator<?> it = in.types();
			while (it.hasNext()) {
				Object el = it.next();
				if (el instanceof TypeExpression) {
					ret += ResourceUtil.toString((TypeExpression) el, prefix + "  ");
				}
			}
			return ret;
		}

		return prefix + "unknown: " + e.getClass().getName() + "\n";
	}
}

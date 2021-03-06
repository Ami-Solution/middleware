/*
	Copyright 2008-2014 Fraunhofer IGD, http://www.igd.fraunhofer.de
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
package org.universAAL.middleware.service.owl;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.universAAL.middleware.owl.PropertyRestriction;
import org.universAAL.middleware.owl.ManagedIndividual;
import org.universAAL.middleware.owl.MergedRestriction;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.rdf.TypeMapper;
import org.universAAL.middleware.service.owls.process.ProcessInput;
import org.universAAL.middleware.service.owls.process.ProcessOutput;
import org.universAAL.middleware.service.owls.profile.ServiceProfile;

/**
 * The root of the hierarchy of service classes in universAAL.
 * <p>
 * All subclasses must follow the conventions declared by
 * {@link ManagedIndividual}.
 * <p>
 * The main characteristic of services is that they specify their view on their
 * domain by restricting some of the relevant properties from the domain
 * ontology that are reachable from the service class using a sequence of
 * properties. The universAAL middleware calls such sequences a
 * {@link org.universAAL.middleware.rdf.PropertyPath}. Subclasses must define a
 * static repository for such restrictions as an empty instance of
 * {@link java.util.Hashtable} and add their class-level restrictions to this
 * repository a static code segment using the help method
 * {@link #addRestriction(MergedRestriction, String[], Hashtable)}.
 * <p>
 * In addition to class-level restrictions, concrete instances can add
 * instance-level restrictions using references to their input parameters (see
 * {@link org.universAAL.middleware.service.owls.process.ProcessInput#asVariableReference()}
 * ). The help method
 * {@link #addInstanceLevelRestriction(MergedRestriction, String[])} facilitates
 * the addition of such instance-level restrictions.
 *
 * @author mtazari - <a href="mailto:Saied.Tazari@igd.fraunhofer.de">Saied
 *         Tazari</a>
 *
 */
public abstract class Service extends ManagedIndividual {

	public static final String OWLS_NAMESPACE_PREFIX = "http://www.daml.org/services/owl-s/1.1/";

	public static final String OWLS_SERVICE_NAMESPACE = OWLS_NAMESPACE_PREFIX + "Service.owl#";
	public static final String PROP_INSTANCE_LEVEL_RESTRICTIONS = VOCABULARY_NAMESPACE
			+ "instanceLevelRestrictions";
	public static final String PROP_NUMBER_OF_VALUE_RESTRICTIONS = VOCABULARY_NAMESPACE
			+ "numberOfValueRestrictions";

	/**
	 * The OWL-S property
	 * http://www.daml.org/services/owl-s/1.1/Service.owl#presents
	 */
	public static final String PROP_OWLS_PRESENTS;

	/**
	 * The OWL-S property
	 * http://www.daml.org/services/owl-s/1.1/Service.owl#presentedBy
	 */
	public static final String PROP_OWLS_PRESENTED_BY;

	public static final String MY_URI;
	static {
		PROP_OWLS_PRESENTS = OWLS_SERVICE_NAMESPACE + "presents";
		PROP_OWLS_PRESENTED_BY = OWLS_SERVICE_NAMESPACE + "presentedBy";
		MY_URI = OWLS_SERVICE_NAMESPACE + "Service";
	}

	/**
	 * A help method for subclasses to manage their restrictions on properties
	 * (from the domain ontology) that are reachable from the subclass, provided
	 * that they have a static Hashtable for gathering them.
	 *
	 * @param r
	 *            the restriction to be added on the last element of the path
	 *            given by 'toPath'.
	 * @param toPath
	 *            the path to which the given restriction must be bound. It must
	 *            start with a property from the service class and address a
	 *            reachable property from the domain ontology; the last element
	 *            of the path must be equal to <code>r.getOnProperty()</code>.
	 * @param restrictions
	 *            a class-level static hash-table for managing restrictions
	 * @return true, if all constraints held and the restriction could be added;
	 *         otherwise, false.
	 */
	protected static final boolean addRestriction(MergedRestriction r, String[] toPath, Hashtable restrictions) {
		if (toPath == null || toPath.length == 0 || restrictions == null || r == null)
			return false;
		MergedRestriction root = (MergedRestriction) restrictions.get(toPath[0]);
		MergedRestriction tmp = r.appendTo(root, toPath);
		if (tmp == null)
			return false;
		if (root == null)
			restrictions.put(toPath[0], tmp);
		return true;
	}

	/**
	 * A restriction previously added by
	 * {@link #addRestriction(MergedRestriction, String[], Hashtable)} to the
	 * given <code>propPath</code> using the same hash-table of
	 * <code>restrictions</code> will be returned by this method.
	 */
	protected static final MergedRestriction getRestrictionOnPropPath(Hashtable restrictions, String[] propPath) {
		if (propPath == null || propPath.length == 0 || restrictions == null)
			return null;
		MergedRestriction m = (MergedRestriction) restrictions.get(propPath[0]);
		return m == null ? null : m.getRestrictionOnPath(propPath);
	}

	/**
	 * Get a list of all simple restrictions ({@link PropertyRestriction}) from
	 * the given hash table that have been added by
	 * {@link #addRestriction(MergedRestriction, String[], Hashtable)}
	 */
	private static final ArrayList getRestrictions(Hashtable restrictions) {
		ArrayList list = new ArrayList();
		Iterator it = restrictions.values().iterator();
		while (it.hasNext()) {
			MergedRestriction m = (MergedRestriction) it.next();
			list.addAll(m.getRestrictions());
		}
		return list;
	}

	protected Service() {
		super();
	}

	protected Service(String uri) {
		super(uri);
		if (uri != null && !Resource.isAnon(uri)) {
			myProfile = new ServiceProfile(this, uri + "Process");
			props.put(PROP_OWLS_PRESENTS, myProfile);
		}
	}

	/**
	 * The instance-level repository of defined restrictions on property paths.
	 * For adding instance-level restrictions to this repository, the method
	 * {@link #addRestriction(MergedRestriction, String[], java.util.Hashtable)}
	 * must be used.
	 */
	protected final Hashtable instanceLevelRestrictions = new Hashtable();
	protected int numberOfValueRestrictions = 0;
	protected ServiceProfile myProfile;

	/**
	 * A method for adding instance-level restrictions.
	 *
	 * @see #instanceLevelRestrictions
	 * @see #addRestriction(MergedRestriction, String[], Hashtable)
	 */
	public final boolean addInstanceLevelRestriction(MergedRestriction r, String[] toPath) {
		if (addRestriction(r, toPath, instanceLevelRestrictions)) {
			if (r.getConstraint(MergedRestriction.hasValueID) != null)
				props.put(PROP_NUMBER_OF_VALUE_RESTRICTIONS, new Integer(++numberOfValueRestrictions));
			props.put(PROP_INSTANCE_LEVEL_RESTRICTIONS, getRestrictions(instanceLevelRestrictions));
			return true;
		}
		return false;
	}

	public final Object getInstanceLevelFixedValueOnProp(String propURI) {
		if (propURI == null)
			return null;
		MergedRestriction r = (MergedRestriction) instanceLevelRestrictions.get(propURI);
		return (r == null) ? null : r.getConstraint(MergedRestriction.hasValueID);
	}

	/**
	 * Returns the restriction on the given <code>propPath</code>, if it was
	 * previously added to {@link #instanceLevelRestrictions} using
	 * {@link #addRestriction(MergedRestriction, String[], Hashtable)}.
	 */
	public final MergedRestriction getInstanceLevelRestrictionOnProp(String propURI) {
		return (MergedRestriction) instanceLevelRestrictions.get(propURI);
	}

	public final int getNumberOfValueRestrictions() {
		return numberOfValueRestrictions;
	}

	public final ServiceProfile getProfile() {
		return myProfile;
	}

	/**
	 * Creates an input from the given URI and cardinality
	 */
	public final ProcessInput createInput(String inParamURI, String typeURI, int minCardinality, int maxCardinality) {
		ProcessInput in = new ProcessInput(inParamURI);
		in.setParameterType(typeURI);
		in.setCardinality(maxCardinality, minCardinality);
		myProfile.addInput(in);
		return in;
	}

	/**
	 * Adds a restriction to a given input
	 */
	public final void addFilteringInput(String inParamURI, String typeURI, int minCardinality, int maxCardinality,
			String[] propPath) {
		ProcessInput in = createInput(inParamURI, typeURI, minCardinality, maxCardinality);
		addInstanceLevelRestriction(
				MergedRestriction.getFixedValueRestriction(propPath[propPath.length - 1], in.asVariableReference()),
				propPath);
	}

	/**
	 * Adds a restriction to a given input
	 */
	public final void addFilteringType(String inParamURI, String[] propPath) {
		ProcessInput in = createInput(inParamURI, TypeMapper.getDatatypeURI(Resource.class), 1, 1);
		String[] pp = new String[propPath.length + 1];
		for (int i = 0; i < propPath.length; i++)
			pp[i] = propPath[i];
		pp[propPath.length] = Resource.PROP_RDF_TYPE;
		addInstanceLevelRestriction(
				MergedRestriction.getFixedValueRestriction(Resource.PROP_RDF_TYPE, in.asVariableReference()), pp);
	}

	/**
	 * Adds an add effect to default profile
	 */
	public final void addInputWithAddEffect(String inParamURI, String typeURI, int minCardinality, int maxCardinality,
			String[] propPath) {
		ProcessInput in = createInput(inParamURI, typeURI, minCardinality, maxCardinality);
		myProfile.addAddEffect(propPath, in.asVariableReference());
	}

	/**
	 * Adds a change effect to default profile
	 */
	public final void addInputWithChangeEffect(String inParamURI, String typeURI, int minCardinality,
			int maxCardinality, String[] propPath) {
		ProcessInput in = createInput(inParamURI, typeURI, minCardinality, maxCardinality);
		myProfile.addChangeEffect(propPath, in.asVariableReference());
	}

	/**
	 * Adds a remove effect to default profile
	 */
	public final void addInputWithRemoveEffect(String inParamURI, String typeURI, int minCardinality,
			int maxCardinality, String[] propPath) {
		addFilteringInput(inParamURI, typeURI, minCardinality, maxCardinality, propPath);
		myProfile.addRemoveEffect(propPath);
	}

	/**
	 * Adds configured output to default profile
	 */
	public final void addOutput(String outParamURI, String typeURI, int minCardinality, int maxCardinality,
			String[] propPath) {
		ProcessOutput out = new ProcessOutput(outParamURI);
		out.setParameterType(typeURI);
		out.setCardinality(maxCardinality, minCardinality);
		myProfile.addOutput(out);
		myProfile.addSimpleOutputBinding(out, propPath);
		addInstanceLevelRestriction(MergedRestriction.getAllValuesRestrictionWithCardinality(
				propPath[propPath.length - 1], typeURI, minCardinality, maxCardinality), propPath);
	}

	/**
	 * @see ManagedIndividual#getPropSerializationType(java.lang.String)
	 */
	public int getPropSerializationType(String propURI) {
		return (PROP_OWLS_PRESENTS.equals(propURI) || PROP_INSTANCE_LEVEL_RESTRICTIONS.equals(propURI)
				|| PROP_NUMBER_OF_VALUE_RESTRICTIONS.equals(propURI)) ? PROP_SERIALIZATION_FULL
						: PROP_SERIALIZATION_UNDEFINED;
	}

	/**
	 * Returns the set of properties restricted at instance level.
	 */
	public final String[] getRestrictedPropsOnInstanceLevel() {
		return (String[]) instanceLevelRestrictions.keySet().toArray(new String[instanceLevelRestrictions.size()]);
	}

	public boolean setProperty(String propURI, Object value) {
		if (PROP_OWLS_PRESENTS.equals(propURI) && value instanceof ServiceProfile
				&& (myProfile == null || myProfile.isEmpty())) {
			myProfile = (ServiceProfile) value;
			props.put(PROP_OWLS_PRESENTS, myProfile);
			return true;
		} else if (PROP_NUMBER_OF_VALUE_RESTRICTIONS.equals(propURI) && value instanceof Integer
				&& numberOfValueRestrictions == 0) {
			numberOfValueRestrictions = ((Integer) value).intValue();
			props.put(PROP_NUMBER_OF_VALUE_RESTRICTIONS, new Integer(numberOfValueRestrictions));
			return true;
		} else if (PROP_INSTANCE_LEVEL_RESTRICTIONS.equals(propURI) && value != null
				&& !props.containsKey(PROP_INSTANCE_LEVEL_RESTRICTIONS)) {
			if (value instanceof List)
				for (Iterator i = ((List) value).iterator(); i.hasNext();) {
					Object o = i.next();
					if (o instanceof PropertyRestriction) {
						PropertyRestriction res = (PropertyRestriction) o;
						// add res as MergedRestriction to
						// instanceLevelRestrictions
						MergedRestriction m = (MergedRestriction) instanceLevelRestrictions.get(res.getOnProperty());
						if (m == null)
							m = new MergedRestriction(res.getOnProperty());
						m.addRestriction(res);
						instanceLevelRestrictions.put(res.getOnProperty(), m);
					} else
						return false;
				}
			else if (value instanceof PropertyRestriction) {
				PropertyRestriction res = (PropertyRestriction) value;
				MergedRestriction m = new MergedRestriction(res.getOnProperty());
				m.addRestriction(res);
				instanceLevelRestrictions.put(res.getOnProperty(), m);
				List aux = new ArrayList(1);
				aux.add(value);
				value = aux;
			} else
				return false;
			props.put(propURI, value);
			return true;
		} else
			return super.setProperty(propURI, value);
	}
}

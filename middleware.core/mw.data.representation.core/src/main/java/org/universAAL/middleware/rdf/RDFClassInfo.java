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
package org.universAAL.middleware.rdf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.universAAL.middleware.container.utils.LogUtils;
import org.universAAL.middleware.datarep.SharedResources;
import org.universAAL.middleware.owl.ManagedIndividual;
import org.universAAL.middleware.owl.TypeExpression;
import org.universAAL.middleware.owl.OntClassInfo;
import org.universAAL.middleware.owl.Ontology;
import org.universAAL.middleware.owl.OntologyManagement;

/**
 * Definition of an RDF class. The creation is separated from the usage; for
 * every RDFClassInfo there is exactly one {@link RDFClassInfoSetup} where all
 * the characteristics of this class are defined.
 *
 * To create a new {@link RDFClassInfo}, define a subclass of {@link Ontology}
 * and overwrite the {@link Ontology#create()} method.
 *
 * @author Carsten Stockloew
 * @see RDFClassInfoSetup
 * @see org.universAAL.middleware.owl.OntClassInfo
 * @see org.universAAL.middleware.owl.OntClassInfoSetup
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "EQ_DOESNT_OVERRIDE_EQUALS", justification = "This is implemented in Resource based on URI and props.")
public class RDFClassInfo extends FinalizedResource {

	/**
	 * The set of URIs of all super classes.
	 */
	protected volatile HashSet namedSuperClasses = new HashSet();

	/**
	 * Repository of all known (non-anonymous) instances. It maps the URI of the
	 * instance to the instance itself.
	 */
	protected HashMap instances = new HashMap();

	/**
	 * The combined list of all super classes as set in the RDF graph (contains
	 * named super classes and restrictions).
	 */
	protected volatile ArrayList combinedSuperClasses = new ArrayList();

	/**
	 * The set of super classes. Members are instances of {@link TypeExpression}
	 * .
	 */
	protected ArrayList superClasses = new ArrayList();

	/**
	 * The factory to create new instances of this class.
	 */
	protected ResourceFactory factory;

	/**
	 * Factory index to be given to the {@link ResourceFactory} to create new
	 * instances of this class.
	 */
	protected int factoryIndex;

	/**
	 * The {@link Ontology} which defines or extends this class.
	 */
	protected Ontology ont;

	/**
	 * Determines whether this class is locked. If it is locked, no new
	 * information can be stored here.
	 */
	protected boolean locked = false;

	/**
	 * Certain classes will be allowed to add named instances even after the setup has been finished and locked.
	 */
	protected HashSet exemptedClasses4addInstance = new HashSet();

	/**
	 * The setup interface.
	 */
	protected PrivateRDFSetup rdfsetup = null;

	/**
	 * Implementation of the setup interface. For security reasons, this is
	 * realized as a protected nested class so that only the creator of an
	 * {@link Ontology} has access to it and can make changes.
	 */
	protected class PrivateRDFSetup implements RDFClassInfoSetup {
		/** The info object. */
		private RDFClassInfo info;

		public PrivateRDFSetup(RDFClassInfo info) {
			this.info = info;
		}

		/** {@inheritDoc} */
		public final void addInstance(Resource instance) {

			if (instance == null) {
				LogUtils.logWarn(SharedResources.moduleContext, PrivateRDFSetup.class, "addInstance",
						new Object[] { "Called with null." }, null);
				return;
			}
			
			if (locked  &&  !exemptedClasses4addInstance.contains(instance.getClass()))
				return;

			if (instance.isAnon()) {
				if (instance instanceof ManagedIndividual) {
					ManagedIndividual m = (ManagedIndividual) instance;
					LogUtils.logError(SharedResources.moduleContext, PrivateRDFSetup.class, "addInstance",
							new Object[] {
									"A resource instance in an ontology has an anonymous URI; this does not make sense. Type of instance: ",
									instance.getClass().getName(), ". Class URI: ", m.getClassURI(), ". URI: ",
									instance.getURI() },
							null);
				} else {
					LogUtils.logError(SharedResources.moduleContext, PrivateRDFSetup.class, "addInstance",
							new Object[] {
									"A resource instance in an ontology has an anonymous URI; this does not make sense. Type of instance: ",
									instance.getClass().getName(), ". URI: ", instance.getURI() },
							null);
				}
				return;
			}

			if (!instances.containsKey(instance.getURI()))
				instances.put(instance.getURI(), instance);
		}

		/** {@inheritDoc} */
		public final void addSuperClass(TypeExpression superClass) {
			if (locked)
				return;
			if (superClass != null) {
				superClasses.add(superClass);
				// if (superClass instanceof Restriction) {
				// // TODO: will we have subclasses of Restriction? If yes,
				// how to handle subclasses?
				// String propURI = ((Restriction)
				// superClass).getOnProperty();
				// MergedRestriction existing = (MergedRestriction)
				// propRestriction
				// .get(propURI);
				// if (existing == null) {
				// existing = new MergedRestriction((Restriction)
				// superClass);
				// propRestriction.put(propURI, existing);
				// } else
				// existing.addRestriction((Restriction) superClass);

				// currently, only one Restriction possible!
				// propRestriction.put(((Restriction) superClass)
				// .getOnProperty(), superClass);
				// }
				// TODO: should we inherit all properties from superclasses
			}
		}

		/** {@inheritDoc} */
		public final void addSuperClass(String namedSuperClass) {
			if (locked)
				return;
			if (namedSuperClass == null)
				return;

			// test if the given super class has this class as super class
			// because this would create a cycle
			if (getURI().equals(namedSuperClass)) {
				LogUtils.logError(SharedResources.moduleContext, PrivateRDFSetup.class, "addSuperClass",
						new Object[] { "The class cannot be its own super class. Ignoring call." }, null);
				// throw new IllegalArgumentException(
				// "The class cannot be its own super class.");
				return;
			}
			Set<String> subClasses = OntologyManagement.getInstance().getNamedSubClasses(getURI(), true, true);
			for (String s : subClasses) {
				if (s.equals(namedSuperClass)) {
					LogUtils.logError(SharedResources.moduleContext, PrivateRDFSetup.class, "addSuperClass",
							new Object[] {
									"The class to add as super class is already a sub class. This would create a cycle in class hierarchy. Ignoring call." },
							null);
					return;
				}
			}

			// add to local variable
			HashSet tmp = new HashSet(namedSuperClasses);
			tmp.add(namedSuperClass);
			namedSuperClasses = tmp;

			// add to RDF graph
			ArrayList al = new ArrayList(combinedSuperClasses);
			al.add(new Resource(namedSuperClass));
			combinedSuperClasses = al;
			setProperty(TypeExpression.PROP_RDFS_SUB_CLASS_OF, Collections.unmodifiableList(combinedSuperClasses));
		}

		/** {@inheritDoc} */
		public void allowInstancesOf(Class c) {
			if (!locked  &&  c != null)
				exemptedClasses4addInstance.add(c);
		}

		/** {@inheritDoc} */
		public final RDFClassInfo getInfo() {
			return info;
		}

		/** {@inheritDoc} */
		public final void setResourceComment(String comment) {
			if (locked)
				return;
			info.setResourceComment(comment);
		}

		/** {@inheritDoc} */
		public final void setResourceLabel(String label) {
			if (locked)
				return;
			info.setResourceLabel(label);
		}
	}

	/**
	 * Create a new RDF Class.
	 *
	 * @param classURI
	 *            The URI of the class.
	 * @param ont
	 *            The {@link Ontology} that creates this instance.
	 * @param factory
	 *            A factory to create new instances; it is <i>null</i> iff the
	 *            class is abstract.
	 * @param factoryIndex
	 *            An index to be given to the <code>factory</code>. If the
	 *            <code>factory</code> is <i>null</i>, this parameter is
	 *            ignored.
	 */
	protected RDFClassInfo(String classURI, Ontology ont, ResourceFactory factory, int factoryIndex) {
		super(classURI);
		if (classURI == null)
			throw new NullPointerException("The class URI must be not null.");

		if (isAnon(classURI))
			throw new IllegalArgumentException("The class URI must be not anonymous.");

		if (classURI.startsWith(Resource.RDF_NAMESPACE))
			throw new IllegalArgumentException("The class URI can not start with the protected namespace of RDF.");

		if (classURI.startsWith(Resource.RDFS_NAMESPACE))
			throw new IllegalArgumentException("The class URI can not start with the protected namespace of RDFS.");

		if (classURI.startsWith(TypeExpression.OWL_NAMESPACE))
			throw new IllegalArgumentException("The class URI can not start with the protected namespace of OWL.");

		this.factory = factory;
		this.factoryIndex = factoryIndex;
		this.ont = ont;
		rdfsetup = new PrivateRDFSetup(this);
		addType(Resource.TYPE_RDFS_CLASS, true);
	}

	/**
	 * Create a new RDF Class. This method can only be called from an
	 * {@link Ontology}.
	 *
	 * @param classURI
	 *            The URI of the class.
	 * @param ont
	 *            The {@link Ontology} that creates this instance.
	 * @param factory
	 *            A factory to create new instances; it is <i>null</i> iff the
	 *            class is abstract.
	 * @param factoryIndex
	 *            An index to be given to the <code>factory</code>. If the
	 *            <code>factory</code> is <i>null</i>, this parameter is
	 *            ignored.
	 * @return The setup interface to set all information of this class.
	 */
	public static RDFClassInfoSetup create(String classURI, Ontology ont, ResourceFactory factory, int factoryIndex) {
		if (ont == null)
			throw new NullPointerException("The ontology must be not null.");
		if (!ont.checkPermission(classURI))
			throw new IllegalAccessError("The given class URI is not defined in the context of the given ontology.");
		RDFClassInfo info = new RDFClassInfo(classURI, ont, factory, factoryIndex);
		return info.rdfsetup;
	}

	/**
	 * Determines whether this class is an abstract class. It is an abstract
	 * class iff the factory is not set.
	 *
	 * @return true, if this is an abstract class.
	 */
	public final boolean isAbstract() {
		return factory == null;
	}

	/**
	 * Get the factory.
	 *
	 * @see ResourceFactory
	 * @return the factory.
	 */
	public final ResourceFactory getFactory() {
		return factory;
	}

	/**
	 * Get the factory index.
	 *
	 * @see ResourceFactory
	 * @return the factory index.
	 */
	public final int getFactoryIndex() {
		return factoryIndex;
	}

	/**
	 * Determines whether the given class is a super class of this class. If
	 * <code>inherited</code> is false, then only <i>direct</i> super classes
	 * are considered.
	 *
	 * @param classURI
	 *            The URI of the super class.
	 * @param inherited
	 *            false, iff only <i>direct</i> super classes should be
	 *            considered.
	 * @return true, if the given class is a super class of this class.
	 */
	public final boolean hasSuperClass(String classURI, boolean inherited) {
		if (namedSuperClasses.contains(classURI))
			return true;
		if (!inherited)
			return false;

		Iterator it = namedSuperClasses.iterator();
		while (it.hasNext()) {
			String superClassURI = (String) it.next();
			OntClassInfo superInfo = OntologyManagement.getInstance().getOntClassInfo(superClassURI);
			if (superInfo != null)
				if (superInfo.hasSuperClass(classURI, inherited))
					return true;
		}
		return false;
	}

	/**
	 * Get the set of URIs of all named super classes.
	 *
	 * @param inherited
	 *            false, iff only <i>direct</i> super classes should be
	 *            returned.
	 * @param includeAbstractClasses
	 *            true, iff abstract classes should be returned.
	 * @return The set of URIs of all named super classes.
	 */
	public final String[] getNamedSuperClasses(boolean inherited, boolean includeAbstractClasses) {

		ArrayList al = new ArrayList();

		if (includeAbstractClasses)
			al.addAll(namedSuperClasses);
		else {
			// add only non-abstract super classes
			Iterator it = namedSuperClasses.iterator();
			while (it.hasNext()) {
				String superClassURI = (String) it.next();
				OntClassInfo info = OntologyManagement.getInstance().getOntClassInfo(superClassURI);
				if (info != null)
					if (!info.isAbstract())
						al.add(superClassURI);
			}
		}

		if (inherited) {
			// add parent super classes
			Iterator it = namedSuperClasses.iterator();
			while (it.hasNext()) {
				String superClassURI = (String) it.next();
				OntClassInfo info = OntologyManagement.getInstance().getOntClassInfo(superClassURI);
				if (info != null) {
					String[] res = info.getNamedSuperClasses(inherited, includeAbstractClasses);
					for (int i = 0; i < res.length; i++)
						al.add(res[i]);
				}
			}
		}

		return (String[]) al.toArray(new String[al.size()]);
	}

	/**
	 * Get the set of all non-named super classes.
	 *
	 * @return the set of all non-named super classes.
	 */
	public final TypeExpression[] getSuperClasses() {
		return (TypeExpression[]) superClasses.toArray(new TypeExpression[superClasses.size()]);
	}

	/**
	 * Get the set of all registered instances of this class. To add new
	 * instances, call {@link RDFClassInfoSetup#addInstance(Resource)}
	 *
	 * @return the set of all registered instances of this class.
	 */
	public final Resource[] getInstances() {
		return (Resource[]) instances.values().toArray(new Resource[instances.size()]);
	}

	/**
	 * Get a specific registered instance of this class. To add new instances,
	 * call {@link RDFClassInfoSetup#addInstance(Resource)}
	 *
	 * @param uri
	 *            The URI of the instance.
	 * @return The instance, if registered.
	 */
	public final Resource getInstanceByURI(String uri) {
		return (Resource) instances.get(uri);
	}

	/**
	 * Lock this instance. After it is locked, no changes can be made. The class
	 * is automatically locked when the {@link Ontology} that defines the class
	 * is registered at the {@link OntologyManagement}.
	 */
	public final void lock() {
		locked = true;
	}

	/**
	 * @see Resource#isClosedCollection(String)
	 */
	public final boolean isClosedCollection(String propURI) {
		if (TypeExpression.PROP_RDFS_SUB_CLASS_OF.equals(propURI))
			return false;
		return super.isClosedCollection(propURI);
	}
}

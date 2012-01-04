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
package org.universAAL.middleware.rdf;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.universAAL.middleware.container.utils.StringUtils;
import org.universAAL.middleware.util.ResourceComparator;

/**
 * The base class for all RDF and OWL classes.
 * 
 * @author mtazari - <a href="mailto:Saied.Tazari@igd.fraunhofer.de">Saied
 *         Tazari</a>
 * @author Carsten Stockloew
 */
public class Resource {

    /** URI prefix for anonymous Resources. */
    protected static final String ANON_URI_PREFIX = "urn:anonymous:";

    /** URI of RDF namespace. */
    public static final String RDF_NAMESPACE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /** URI of RDFS namespace. */
    public static final String RDFS_NAMESPACE = "http://www.w3.org/2000/01/rdf-schema#";

    /** URI for empty RDF lists, or the end of an RDF list. */
    public static final String RDF_EMPTY_LIST = RDF_NAMESPACE + "nil";

    /** URI of the first element of an RDF list. */
    public static final String PROP_RDF_FIRST = RDF_NAMESPACE + "first";

    /** URI of the remaining elements of an RDF list. */
    public static final String PROP_RDF_REST = RDF_NAMESPACE + "rest";

    /** URI for rdf:type. */
    public static final String PROP_RDF_TYPE = RDF_NAMESPACE + "type";

    /** URI for RDF comments. */
    public static final String PROP_RDFS_COMMENT = RDFS_NAMESPACE + "comment";

    /** URI for RDF labels. */
    public static final String PROP_RDFS_LABEL = RDFS_NAMESPACE + "label";

    /** URI for RDF lists. */
    public static final String TYPE_RDF_LIST = RDF_NAMESPACE + "List";

    /** URI prefix for the universAAL namespace. */
    public static final String uAAL_NAMESPACE_PREFIX = "http://ontology.universAAL.org/";

    /** URI for the universAAL service namespace. */
    public static final String uAAL_SERVICE_NAMESPACE = uAAL_NAMESPACE_PREFIX
	    + "Service.owl#";

    /** URI for the universAAL namespace. */
    public static final String uAAL_VOCABULARY_NAMESPACE = uAAL_NAMESPACE_PREFIX
	    + "uAAL.owl#";

    /** URI for properties linking to the user involved. */
    public static final String PROP_uAAL_INVOLVED_HUMAN_USER = uAAL_VOCABULARY_NAMESPACE
	    + "theInvolvedHumanUser";

    /**
     * Legal return values for {@link #getPropSerializationType(String)}.
     * <code>PROP_SERIALIZATION_UNDEFINED</code> says that, when serializing an
     * instance of this class in a minimized way, it is undefined whether a
     * property can be ignored.
     */
    public static final int PROP_SERIALIZATION_UNDEFINED = 0;

    /**
     * Legal return values for {@link #getPropSerializationType(String)}.
     * <code>PROP_SERIALIZATION_OPTIONAL</code> says that, when serializing an
     * instance of this class in a minimized way, a property can be ignored.
     */
    public static final int PROP_SERIALIZATION_OPTIONAL = 1;

    /**
     * Legal return values for {@link #getPropSerializationType(String)}.
     * <code>PROP_SERIALIZATION_REDUCED</code> says that, when serializing an
     * instance of this class in a minimized way, a property must be included
     * but the value can be represented in its reduced form.
     */
    public static final int PROP_SERIALIZATION_REDUCED = 2;

    /**
     * Legal return values for {@link #getPropSerializationType(String)}.
     * <code>PROP_SERIALIZATION_FULL</code> says that, when serializing an
     * instance of this class in a minimized way, a property must be included in
     * its full form.
     */
    public static final int PROP_SERIALIZATION_FULL = 3;

    /**
     * Used for deserialization: all classes derived from Resource have to
     * register to the Resource base class with their URI by calling
     * {@link #addResourceClass(String uri, Class clz)}. The deserializer can
     * then get an instance of a class given its URI by calling
     * {@link #getResource(String classURI, String instanceURI)}
     */
    // private static Hashtable uriRsrcClass = new Hashtable();

    /**
     * Registration of specialized objects. When getting a Resource (e.g. by a
     * serializer), either the specialized Resource is retrieved according to
     * the URI of the specific instance, or a new object is created which is
     * derived from Resource according to the class URI. Register an object by
     * calling {@link #addSpecialResource(Resource r)}. The registered instance
     * - given its instance URI - can then be retrieved by calling
     * {@link #getResource(String classURI, String instanceURI)}
     */
    // private static Hashtable uriResource = new Hashtable();

    /**
     * URI of this resource, or URIref (URI plus fragment identifier, separated
     * by the symbol '#').
     */
    protected final String uri;

    /**
     * For a given URIref, 'ns_delim_index' is the index of the delimiter (the
     * symbol '#'), or -1 if there is no delimiter.
     */
    protected final int ns_delim_index;

    /**
     * The properties denote the RDF triples of this resource, realized as
     * Hashtable. The RDF subject is this Resource itself, the key of the
     * Hashtable is the RDF predicate and the value of the Hashtable is the RDF
     * object, which can be a literal or another resource. See
     * {@link #setProperty(String propURI, Object value)} for more information.
     */
    protected final Hashtable props = new Hashtable();

    /**
     * A resource can have one or more RDF types which are represented in this
     * class as a property with key (RDF predicate) 'rdf:type' and an ArrayList
     * as value (RDF object). Types are added by calling
     * {@link #addType(String typeURI, boolean blockFurtherTypes)}. If only one
     * type is possible, this attribute indicates that not more than one type
     * can be added.
     */
    protected boolean blockAddingTypes = false;

    /** true, if this resource is an XML Literal. */
    protected boolean isXMLLiteral = false;

    /*
     * --------------------------------------------------------------
     * Constructors
     * --------------------------------------------------------------
     */

    /** Constructor to create a new Resource with anonymous URI. */
    public Resource() {
	uri = Resource.generateAnonURI();
	ns_delim_index = -1;
    }

    /**
     * Constructor to create a new Resource with anonymous URI. The Resource may
     * be an XML Literal.
     */
    public Resource(boolean isXMLLiteral) {
	uri = Resource.generateAnonURI();
	ns_delim_index = -1;
	this.isXMLLiteral = isXMLLiteral;
    }

    /** Constructor to create a new Resource with the specified URI. */
    public Resource(String uri) {
	if (uri == null) {
	    this.uri = Resource.generateAnonURI();
	    ns_delim_index = -1;
	} else {
	    this.uri = uri;
	    ns_delim_index = isQualifiedName(uri) ? uri.lastIndexOf('#') : -1;
	}
    }

    /**
     * Constructor to create a new Resource with the specified URI. The Resource
     * may be an XML Literal.
     */
    public Resource(String uri, boolean isXMLLiteral) {
	if (uri == null) {
	    this.uri = Resource.generateAnonURI();
	    ns_delim_index = -1;
	} else {
	    this.uri = uri;
	    ns_delim_index = isQualifiedName(uri) ? uri.lastIndexOf('#') : -1;
	}
	this.isXMLLiteral = isXMLLiteral;
    }

    /**
     * Creates an instance of Resource with a URI that is created by appending a
     * unique ID to the given 'uriPrefix'. This constructor has a pseudo
     * parameter 'numProps' in order to make it distinct from the other
     * constructor that also takes a string. Later versions of Resource may
     * decide to make some use of numProps in some way, however.
     * 
     * @param uriPrefix
     *            Prefix of the URI.
     * @param numProps
     *            Not used.
     */
    protected Resource(String uriPrefix, int numProps) {
	uri = uriPrefix + StringUtils.createUniqueID();
	ns_delim_index = isQualifiedName(uri) ? uri.lastIndexOf('#') : -1;
    }

    /*
     * -------------------------------------------------------------- Methods
     * --------------------------------------------------------------
     */

    /**
     * Register a new Resource class. This can be used for deserialisation: when
     * a message arrives with this URI, a new instance of this class can be
     * generated.
     */
    // protected static final void addResourceClass(String uri, Class clz) {
    // if (StringUtils.isNonEmpty(uri) && clz != null
    // && !uriRsrcClass.containsKey(uri))
    // uriRsrcClass.put(uri, clz);
    // }

    /**
     * Register a new specialized Resource instance (instead of a Resource
     * class). The instance can be retrieved by calling
     * {@link #getResource(String classURI, String instanceURI)}
     * 
     * @param r
     *            The Resource to register.
     */
    // protected static final void addSpecialResource(Resource r) {
    // if (r != null && !r.isAnon())
    // uriResource.put(r.uri, r);
    // }

    /**
     * Creates a new Resource instance which is treated as an RDF list and
     * contains the specified list of elements.
     * 
     * @param members
     *            The elements of the list. This will be treated as separate
     *            Resources to be part of the resulting list.
     * @param isXMLLiteral
     *            true, if the resources in the list are XML Literals.
     * @return A Resource which represents the list.
     */
    public static final Resource asRDFList(List members, boolean isXMLLiteral) {
	if (members == null || members.isEmpty())
	    return new Resource(RDF_EMPTY_LIST, isXMLLiteral);
	Resource result = new Resource(isXMLLiteral);
	result.addType(TYPE_RDF_LIST, true);
	result.props.put(PROP_RDF_FIRST, members.remove(0));
	result.props.put(PROP_RDF_REST, members);
	return result;
    }

    /**
     * Create a new anonymous URI. The URI starts with the typical String for
     * anonymous URIs followed by a unique ID.
     */
    public static final String generateAnonURI() {
	return ANON_URI_PREFIX + StringUtils.createUniqueID();
    }

    /**
     * Get a Resource with the given class and instance URI.
     * 
     * @param classURI
     *            The URI of the class.
     * @param instanceURI
     *            The URI of the instance.
     * @return The Resource object with the given 'instanceURI', or a new
     *         Resource, if it does not exist.
     * @see #addSpecialResource(Resource)
     */
    public static Resource getResource(String classURI, String instanceURI) {

	return ResourceRegistry.getInstance()
		.getResource(classURI, instanceURI);

	// if (classURI == null)
	// return null;
	//
	// Class clz = (Class) uriRsrcClass.get(classURI);
	// if (clz == null)
	// return null;
	//
	// try {
	// if (Resource.isAnonymousURI(instanceURI))
	// return (Resource) clz.newInstance();
	// else {
	// Object o = uriResource.get(instanceURI);
	// if (o instanceof Resource && o.getClass() == clz)
	// return (Resource) o;
	// return (Resource) clz.getConstructor(
	// new Class[] { String.class }).newInstance(
	// new Object[] { instanceURI });
	// }
	// } catch (Exception e) {
	// return null;
	// }
    }

    /**
     * Determines if the specified URI is an anonymous URI, i.e. it is either
     * null or starts with the the String typical for anonymous URIs.
     * 
     * @see #ANON_URI_PREFIX
     */
    public static final boolean isAnonymousURI(String uri) {
	return uri == null || uri.startsWith(ANON_URI_PREFIX);
    }

    /**
     * Determines if the specified URI is a qualified name (see
     * {@link org.universAAL.middleware.util.StringUtils#isQualifiedName(String)}
     * ) and is not an anonymous URI.
     */
    public static final boolean isQualifiedName(String uri) {
	return StringUtils.isQualifiedName(uri)
		&& !uri.startsWith(ANON_URI_PREFIX);
    }

    /**
     * Set or add the type of this Resource. The type complies to rdf:type. A
     * Resource can have multiple types.
     * 
     * @param typeURI
     *            URI of the type.
     * @param blockFurtherTypes
     *            If true, no further types can be added.
     */
    public final void addType(String typeURI, boolean blockFurtherTypes) {
	if (!this.blockAddingTypes) {
	    if (typeURI != null) {
		Object o = props.get(PROP_RDF_TYPE);
		Resource type = new Resource(typeURI);
		if (o instanceof List && !((List) o).contains(type))
		    ((List) o).add(type);
		else {
		    List l = new ArrayList(2);
		    if (o instanceof Resource && !type.equals(o))
			l.add(o);
		    l.add(type);
		    props.put(PROP_RDF_TYPE, l);
		}
	    }
	    this.blockAddingTypes = blockFurtherTypes;
	}
    }

    /**
     * If this Resource represents an RDF, retrieve the elements as
     * {@link java.util.List}.
     * 
     * @return The list containing the elements of this RDF list.
     */
    public List asList() {
	String type = getType();
	if (type == null || !type.equals(TYPE_RDF_LIST))
	    return null;
	List result = new ArrayList();
	asList(result);
	return result;
    }

    /**
     * If this Resource represents an RDF, retrieve the elements as
     * {@link java.util.List}.
     * 
     * @param l
     *            The list to store the elements of this RDF list.
     */
    public void asList(List l) {
	if (!uri.equals(RDF_EMPTY_LIST)) {
	    Object o = props.get(PROP_RDF_FIRST);
	    if (o != null) {
		l.add(o);
		o = props.get(PROP_RDF_REST);
		if (o instanceof Resource) {
		    String type = ((Resource) o).getType();
		    if (type != null && type.equals(TYPE_RDF_LIST))
			((Resource) o).asList(l);
		    // TODO: log that rest must be a list or rdf:nil
		} else if (o instanceof List)
		    // the rest is already a list object
		    l.addAll((List) o);
		// TODO: add a last 'else' with log that rest must be either a
		// List, or a Resource of type rdf:List, or rdf:nil
	    }
	}
    }

    /**
     * Change the value (RDF object) of the specified property (RDF predicate)
     * to the given object. If the value can't be set, it is ensured that the
     * original value is restored.
     * 
     * @param propURI
     *            The value has to be changed for this property.
     * @param value
     *            The new value.
     * @return true, if the value could be set.
     */
    public boolean changeProperty(String propURI, Object value) {
	if (propURI != null) {
	    Object o = props.remove(propURI);
	    if (value == null)
		return true;

	    setProperty(propURI, value);
	    if (props.get(propURI) == value)
		return true;
	    if (o != null)
		props.put(propURI, o);
	}
	return false;
    }

    /** If this object is an XML Literal, create a copy of it. */
    public Resource copyAsXMLLiteral() {
	Resource copy = new Resource(uri, true);
	for (Enumeration e = props.keys(); e.hasMoreElements();) {
	    Object key = e.nextElement();
	    copy.props.put(key, props.get(key));
	}
	return copy;
    }

    /**
     * Create a deep copy of this Resource, i.e. create a new Resource for this
     * object (only a Resource, but not a derived class) and for the resources
     * of all properties.<br>
     * Currently, only resources are copies, but not list of resources.
     * 
     * @return The copied Resource.
     */
    // TODO: only resources are copies, but not list of resources.
    // TODO: will create an infinite loop for cycles.
    public Resource deepCopy() {
	Resource copy = new Resource(uri, isXMLLiteral);
	copy.blockAddingTypes = blockAddingTypes;
	for (Enumeration e = props.keys(); e.hasMoreElements();) {
	    Object key = e.nextElement();
	    Object value = props.get(key);
	    if (value instanceof Resource)
		value = ((Resource) value).deepCopy();
	    copy.props.put(key, value);
	}
	return copy;
    }

    /** Determines if this Resource equals the specified Resource. */
    public boolean equals(Object other) {
	return (this == other) ? true
		: (other instanceof Resource) ? ((!isAnon() && uri
			.equals(((Resource) other).uri)) || new ResourceComparator()
			.areEqual(this, (Resource) other))
			: false;
    }

    /** Get the Resource comment. Convenient method to retrieve rdfs:comment. */
    public String getResourceComment() {
	Object val = props.get(PROP_RDFS_COMMENT);
	return (val instanceof String) ? (String) val : null;
    }

    /** Get the Resource label. Convenient method to retrieve rdfs:label. */
    public String getResourceLabel() {
	Object val = props.get(PROP_RDFS_LABEL);
	return (val instanceof String) ? (String) val : null;
    }

    /**
     * If this resource has no original label, constructs one for it without
     * changing the resource itself.
     * 
     * @param type
     *            The optional type to be used instead of the return value of
     *            'getType()' when constructing a label
     * @return if there is an original label, that one is returned; otherwise a
     *         label constructed on-the-fly will be returned
     */
    public String getOrConstructLabel(String type) {
	Object val = props.get(PROP_RDFS_LABEL);
	if (val instanceof String)
	    return (String) val;
	if (type == null)
	    type = StringUtils.deriveLabel(getType());
	return isAnon() ? "a(n) " + type : type + " \""
		+ (hasQualifiedName() ? StringUtils.deriveLabel(uri) : uri)
		+ "\"";
    }

    /**
     * Get the local name which is the part of the URI after the delimiter
     * ('#').
     * 
     * @see #getNamespace()
     */
    public final String getLocalName() {
	return (ns_delim_index < 0) ? null : uri.substring(ns_delim_index + 1);
    }

    /**
     * Get the namespace of the URI which is the start of the URI including the
     * delimiter ('#'). It is the URI without the local name.
     * 
     * @see #getLocalName()
     */
    public final String getNamespace() {
	return (ns_delim_index < 0) ? null : uri.substring(0,
		ns_delim_index + 1);
    }

    /** Get the RDF object for a specified property. */
    public final Object getProperty(String propURI) {
	return props.get(propURI);
    }

    /** Get all properties, i.e. all RDF predicates for this Resource. */
    public final Enumeration getPropertyURIs() {
	return props.keys();
    }

    /**
     * Answers if the given property has to be considered when serializing this
     * individual in a minimized way, and if not ignore-able, whether its value
     * should be presented in its full form or can be reduced. The return value
     * must be one of {@link #PROP_SERIALIZATION_OPTIONAL},
     * {@link #PROP_SERIALIZATION_REDUCED}, or {@link #PROP_SERIALIZATION_FULL}.
     * It can be assumed that the given property is one of those returned by
     * {@link #getPropertyURIs()}. <br>
     * Decision criterion should be if the value of this property is absolutely
     * necessary when this resource is being sent to a remote node. If the
     * subclass rates it as unlikely that the receiver side would need this
     * info, the answer should be <code>PROP_SERIALIZATION_OPTIONAL</code> in
     * favor of lower communication traffic and higher performance even at risk
     * of a possible additional query on the receiver side for fetching this
     * info. With the same rationale, if a property should be included in the
     * process of serialization, it is preferable to include it in a reduced
     * form; in this case the return value should be
     * <code>PROP_SERIALIZATION_REDUCED</code>, otherwise
     * <code>PROP_SERIALIZATION_FULL</code> can be returned.
     * 
     * Subclasses should normally overwrite this method as this default
     * implementation returns always <code>PROP_SERIALIZATION_FULL</code>.
     */
    public int getPropSerializationType(String propURI) {
	return PROP_SERIALIZATION_FULL;
    }

    /**
     * Helper method to get the static field of the java class with the given
     * field name. If the field is not defined in this class, the given default
     * value is returned.
     * 
     * @param fieldName
     *            Name of the static field of the java class to retrieve.
     * @param defaultValue
     *            Default value, if the field could not be retrieved.
     * @return The value of the field.
     */
    public Object getStaticFieldValue(String fieldName, Object defaultValue) {
	try {
	    Object o = getClass().getDeclaredField(fieldName).get(null);
	    return (o == null) ? defaultValue : o;
	} catch (Exception e) {
	    return null;
	}
    }

    /**
     * Returns the URI of the first type added to the list of types of this
     * resource.
     */
    public final String getType() {
	Object o = props.get(PROP_RDF_TYPE);
	if (o instanceof List)
	    if (((List) o).isEmpty())
		return null;
	    else
		return ((List) o).get(0).toString();
	else if (o instanceof Resource)
	    return o.toString();
	else
	    return null;
    }

    /**
     * Returns the URIs of all known types of this resource.
     */
    public final String[] getTypes() {
	Object o = props.get(PROP_RDF_TYPE);
	if (o instanceof List) {
	    String[] types = new String[((List) o).size()];
	    for (int i = 0; i < ((List) o).size(); i++)
		types[i] = ((List) o).get(i).toString();
	    return types;
	} else if (o instanceof Resource)
	    return new String[] { o.toString() };
	else
	    return new String[0];
    }

    /** Get the URI. */
    public final String getURI() {
	return uri;
    }

    /** Get the has code for this Resource, calculated from the URI. */
    public int hashCode() {
	return uri.hashCode();
    }

    /** Determines if this Resource has the specified property. */
    public boolean hasProperty(String propURI) {
	return (propURI != null && props.containsKey(propURI));
    }

    /**
     * Determines if this Resource has a qualified, i.e. the URI has a delimiter
     * ('#').
     */
    public final boolean hasQualifiedName() {
	return ns_delim_index >= 0;
    }

    /** Determines if this Resource has an anonymous URI. */
    public final boolean isAnon() {
	return uri.startsWith(ANON_URI_PREFIX);
    }

    /**
     * Returns true if the value of the given property should be treated as an
     * rdf:List. Serializers can use this to determine if a multi-valued
     * property should be serialized using the concept of rdf:List or the
     * property should appear as often as the number of values assigned to the
     * property. The default behavior is that a property associated with an
     * instance of {@link java.util.List} is assumed to be a closed collection.
     * Subclasses can change this, if needed.
     */
    public boolean isClosedCollection(String propURI) {
	if (PROP_RDF_TYPE.equals(propURI) || propURI == null)
	    return false;

	return (props.get(propURI) instanceof List);
    }

    /**
     * Returns true, if the state of the resource is valid, otherwise false.
     * <p>
     * Subclasses should overwrite this methods as the default implementation
     * returns always true.
     */
    public boolean isWellFormed() {
	return true;
    }

    /**
     * Returns the number of properties, i.e. the number of RDF predicates for
     * this Resource.
     */
    public final int numberOfProperties() {
	return props.size();
    }

    public boolean representsQualifiedURI() {
	return ns_delim_index > 0 && props.size() == 0;
    }

    /**
     * Resources to be serialized and parsed as rdf:XMLLiteral must overwrite
     * this method and return true. Serializers and parsers can use this as a
     * hint.
     */
    public boolean serializesAsXMLLiteral() {
	return isXMLLiteral;
    }

    /** Set the Resource comment. Convenient method to set rdfs:comment. */
    public void setResourceComment(String comment) {
	if (comment != null && !props.containsKey(PROP_RDFS_COMMENT))
	    props.put(PROP_RDFS_COMMENT, comment);
    }

    /** Set the Resource label. Convenient method to set rdfs:label. */
    public void setResourceLabel(String label) {
	if (label != null && !props.containsKey(PROP_RDFS_LABEL))
	    props.put(PROP_RDFS_LABEL, label);
    }

    /**
     * Adds a statement with this resource as the subject, the given
     * <code>propURI</code> as the predicate and the given value as the object.
     * Subclasses must override this in order to decide if the statement to be
     * added fits the general class constraints. If not, the call of this method
     * should be ignored. For each property only one single call may be made to
     * this method, unless subsequent calls to this method for setting the value
     * of the same property are treated as an update for an update-able
     * property. Multi-valued properties must be set using an instance of
     * {@link java.util.List}. The differentiation, if a such list should be
     * treated as an rdf:List, can be made with the help of
     * {@link #isClosedCollection(String)}. The default implementation here
     * accepts all property-value pairs blindly except for rdf:type which is
     * handled if the value is a type URI, a Resource or a java.util.List of
     * them.
     * <p>
     * Note: The setting of the property rdf:type is being handled by this class
     * via the final methods {@link #addType(String, boolean)},
     * {@link #getType()}, and {@link #getTypes()}. Although these methods give
     * the view of handling type URIs as strings, but in reality the types are
     * stored as direct instances of this class. So, the subclasses should
     * ignore calls for setting rdf:type; if not, then the subclass must pay
     * attention that the value should be a {@link List} of direct instances of
     * this class so that (1) the {@link #toString()} method returns just the
     * URI and (2) the serializers get no problems with the value. Also,
     * settings via subclasses may be overwritten by this class if a subsequent
     * call to {@link #addType(String, boolean)} is made.
     */
    public void setProperty(String propURI, Object value) {
	if (propURI != null && value != null)
	    if (PROP_RDF_TYPE.equals(propURI)) {
		if (value instanceof String)
		    addType((String) value, false);
		else if (value instanceof Resource)
		    addType(((Resource) value).uri, false);
		else if (value instanceof List)
		    for (int i = 0; i < ((List) value).size(); i++)
			setProperty(propURI, ((List) value).get(i));
	    } else if (PROP_RDFS_COMMENT.equals(propURI)
		    || PROP_RDFS_LABEL.equals(propURI)) {
		if (!props.containsKey(propURI) && value instanceof String)
		    props.put(propURI, value);
	    } else
		props.put(propURI, value);
    }

    /**
     * Set the given value at the end of the given property path, but does not
     * force the setting.
     * 
     * @see #setPropertyPathFromOffset(String[], int, Object, boolean)
     */
    public boolean setPropertyPath(String[] propPath, Object value) {
	return setPropertyPathFromOffset(propPath, 0, value, false);
    }

    /**
     * Set the given value at the end of the given property path.
     * 
     * @see #setPropertyPathFromOffset(String[], int, Object, boolean)
     */
    public boolean setPropertyPath(String[] propPath, Object value,
	    boolean force) {
	return setPropertyPathFromOffset(propPath, 0, value, force);
    }

    /**
     * Change or add the Resource at the end of the given property path to the
     * given value. This method starts from this Resource and follows the given
     * property path through the RDF graph. If a property from the path does not
     * yet exist, a new anonymous Resource is automatically created. At the end
     * of the property path, the given value is set as RDF object with the last
     * property from the path as RDF predicate.
     * 
     * @param propPath
     *            The set of properties defining the path through the RDF graph.
     * @param fromIndex
     *            The property path is evaluated from this index on; if
     *            'fromIndex' is greater than zero, then some entries at the
     *            beginning are just ignored.
     * @param value
     *            The value to set at the end of the property path
     * @param force
     *            Determines if setting the value has to be forced. If true,
     *            {@link #changeProperty(String, Object)} is called, otherwise
     *            {@link #setProperty(String, Object)} is called.
     * @return true, if the operation was successful.
     */
    // TODO: check if fromIndex > propPath.length (otherwise results in infinite
    // loop)
    public boolean setPropertyPathFromOffset(String[] propPath, int fromIndex,
	    Object value, boolean force) {
	try {
	    if (fromIndex == propPath.length - 1) {
		if (force)
		    return changeProperty(propPath[fromIndex], value);
		setProperty(propPath[fromIndex], value);
		return props.get(propPath[fromIndex]) == value;
	    }

	    Object tmp = props.get(propPath[fromIndex]);
	    if (tmp == null) {
		if (value == null)
		    return true;
		tmp = new Resource();
		props.put(propPath[fromIndex], tmp);
	    } else if (!(tmp instanceof Resource))
		return false;

	    return ((Resource) tmp).setPropertyPathFromOffset(propPath,
		    fromIndex + 1, value, force);
	} catch (Exception e) {
	    return false;
	}
    }

    /** Get a String representation of this Resource; returns the URI. */
    public String toString() {
	return uri;
    }

    /**
     * Debug method: get a string of this RDF graph.
     * 
     * @return The graph as string.
     */
    public String toStringRecursive() {
	return toStringRecursive("", true, null);
    }

    /**
     * Debug method: get a string of this RDF graph.
     * 
     * @param prefix
     *            Indention string that every line starts with.
     * @param prefixAtStart
     *            True iff the first line should start with the prefix string.
     * @return The graph as string.
     */
    public String toStringRecursive(String prefix, boolean prefixAtStart,
	    Hashtable visitedElements) {
	if (visitedElements == null)
	    visitedElements = new Hashtable();

	String s = new String();
	if (prefixAtStart)
	    s += prefix;
	s += this.getClass().getName() + "\n";
	prefix += "  ";
	s += prefix + "URI: " + getURI();

	Resource visited = (Resource) visitedElements.get(this);
	if (visited != null) {
	    if (visited == this) {
		// this element has been visited before
		s += " --> \n";
		return s;
	    }
	}
	visitedElements.put(this, this);
	s += "\n";

	s += prefix + "Properties (Key-Value): " + "(size: " + props.size()
		+ ")\n";
	Enumeration e = props.keys();
	while (e.hasMoreElements()) {
	    String key = (String) e.nextElement();
	    Object val = props.get(key);
	    s += prefix + "* K " + key + "\n";
	    s += prefix + "* V ";
	    if (val instanceof Resource)
		s += ((Resource) val).toStringRecursive(prefix + "    ", false,
			visitedElements);
	    else if (val instanceof List) {
		s += "List" + "\n";
		// for (Object o : (List)val) {
		Iterator iter = ((List) val).iterator();
		while (iter.hasNext()) {
		    Object o = iter.next();
		    if (o instanceof Resource)
			s += ((Resource) o).toStringRecursive(
				prefix + "      ", true, visitedElements);
		    else
			s += prefix + "      " + "unknown: "
				+ val.getClass().getName() + "\n";
		}
	    } else {
		String type = TypeMapper.getDatatypeURI(val);
		if (type == null)
		    s += "unknown: " + val.getClass().getName() + "\n";
		else
		    s += "Literal: " + type + " " + val + "\n";
	    }
	}
	return s;
    }
}
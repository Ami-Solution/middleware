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
package org.universAAL.middleware.owl;

import org.universAAL.middleware.rdf.TypeMapper;

/**
 * A {@link TypeExpression} ({@link BoundedValueRestriction}) that contains all
 * literals of type double with a given lower bound and/or upper bound.
 *
 * @author Carsten Stockloew
 */
public final class DoubleRestriction extends BoundedValueRestriction {

	/** URI of the data type <i>Double</i>. */
	public static final String DATATYPE_URI = TypeMapper.getDatatypeURI(Double.class);

	/** The smallest possible double value. */
	// substitutions for Double.MIN_NORMAL
	private static final double DOUBLE_SMALLEST_POSITIVE_VALUE = Double.longBitsToDouble(0x0010000000000000L);

	/** Standard constructor for exclusive use by serializers. */
	public DoubleRestriction() {
		super(DATATYPE_URI);
	}

	/**
	 * Creates a new restriction.
	 *
	 * @param min
	 *            The minimum value, or null if no minimum is defined.
	 * @param minInclusive
	 *            True, if the minimum value is included. Ignored, if min is
	 *            null.
	 * @param max
	 *            The maximum value, or null if no maximum is defined.
	 * @param maxInclusive
	 *            True, if the maximum value is included. Ignored, if max is
	 *            null.
	 */
	public DoubleRestriction(double min, boolean minInclusive, double max, boolean maxInclusive) {
		this(new Double(min), minInclusive, new Double(max), maxInclusive);
	}

	/**
	 * Creates a new restriction.
	 *
	 * @param min
	 *            The minimum value, or null if no minimum is defined.
	 * @param minInclusive
	 *            True, if the minimum value is included. Ignored, if min is
	 *            null.
	 * @param max
	 *            The maximum value, or null if no maximum is defined.
	 * @param maxInclusive
	 *            True, if the maximum value is included. Ignored, if max is
	 *            null.
	 */
	public DoubleRestriction(Double min, boolean minInclusive, Double max, boolean maxInclusive) {
		super(TypeMapper.getDatatypeURI(Double.class), min, minInclusive, max, maxInclusive);
	}

	/**
	 * Creates a new restriction.
	 *
	 * @param min
	 *            The minimum value, or a {@link Variable} reference, or null if
	 *            no minimum is defined.
	 * @param minInclusive
	 *            True, if the minimum value is included. Ignored, if min is
	 *            null.
	 * @param max
	 *            The maximum value, or a {@link Variable} reference, or null if
	 *            no maximum is defined.
	 * @param maxInclusive
	 *            True, if the maximum value is included. Ignored, if max is
	 *            null.
	 */
	public DoubleRestriction(Object min, boolean minInclusive, Object max, boolean maxInclusive) {
		super(TypeMapper.getDatatypeURI(Double.class), min, minInclusive, max, maxInclusive);
	}

	@Override
	protected boolean checkType(Object o) {
		if (o instanceof Double)
			return true;
		return super.checkType(o);
	}

	@Override
	protected Comparable getNext(Comparable c) {
		return new Double(((Double) c).doubleValue() + DOUBLE_SMALLEST_POSITIVE_VALUE);
	}

	@Override
	protected Comparable getPrevious(Comparable c) {
		return new Double(((Double) c).doubleValue() - DOUBLE_SMALLEST_POSITIVE_VALUE);
	}

	@Override
	public TypeExpression copy() {
		return copyTo(new DoubleRestriction());
	}
}

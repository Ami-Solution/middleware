/*
        Copyright 2007-2014 CNR-ISTI, http://isti.cnr.it
        Institute of Information Science and Technologies
        of the Italian National Research Council

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

package org.universAAL.middleware.managers.deploy.uapp.model;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * describes a simple requirement
 *
 * <p>
 * Java class for reqAtomType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within
 * this class.
 *
 * <pre>
 * &lt;complexType name="reqAtomType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="reqAtomName" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="reqAtomValue" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="reqCriteria" type="{http://www.universaal.org/aal-uapp/v1.0.2}logicalCriteriaType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "reqAtomType", propOrder = { "reqAtomName", "reqAtomValue", "reqCriteria" })
public class ReqAtomType implements Serializable {

	private final static long serialVersionUID = 12343L;
	@XmlElement(required = true)
	protected String reqAtomName;
	@XmlElement(required = true)
	protected String reqAtomValue;
	@XmlElement(defaultValue = "equal")
	protected LogicalCriteriaType reqCriteria;

	/**
	 * Gets the value of the reqAtomName property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getReqAtomName() {
		return reqAtomName;
	}

	/**
	 * Sets the value of the reqAtomName property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setReqAtomName(String value) {
		this.reqAtomName = value;
	}

	public boolean isSetReqAtomName() {
		return (this.reqAtomName != null);
	}

	/**
	 * Gets the value of the reqAtomValue property.
	 *
	 * @return possible object is {@link String }
	 *
	 */
	public String getReqAtomValue() {
		return reqAtomValue;
	}

	/**
	 * Sets the value of the reqAtomValue property.
	 *
	 * @param value
	 *            allowed object is {@link String }
	 *
	 */
	public void setReqAtomValue(String value) {
		this.reqAtomValue = value;
	}

	public boolean isSetReqAtomValue() {
		return (this.reqAtomValue != null);
	}

	/**
	 * Gets the value of the reqCriteria property.
	 *
	 * @return possible object is {@link LogicalCriteriaType }
	 *
	 */
	public LogicalCriteriaType getReqCriteria() {
		return reqCriteria;
	}

	/**
	 * Sets the value of the reqCriteria property.
	 *
	 * @param value
	 *            allowed object is {@link LogicalCriteriaType }
	 *
	 */
	public void setReqCriteria(LogicalCriteriaType value) {
		this.reqCriteria = value;
	}

	public boolean isSetReqCriteria() {
		return (this.reqCriteria != null);
	}

}

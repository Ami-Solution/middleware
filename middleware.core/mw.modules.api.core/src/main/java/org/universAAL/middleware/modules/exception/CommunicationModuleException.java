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
package org.universAAL.middleware.modules.exception;

/**
 * Communication Module Exception
 *
 * @author <a href="mailto:michele.girolami@isti.cnr.it">Michele Girolami</a>
 * @author <a href="mailto:francesco.furfari@isti.cnr.it">Francesco Furfari</a>
 * @author <a href="mailto:filippo.palumbo@isti.cnr.it">Filippo Palumbo</a>
 */
public class CommunicationModuleException extends RuntimeException {

	private String description;

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public short getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(short errorCode) {
		this.errorCode = errorCode;
	}

	private short errorCode;

	public CommunicationModuleException(short errorCode, String description) {
		super(description);
		this.description = description;
		this.errorCode = errorCode;
	}

	public CommunicationModuleException(short errorCode, String description, Throwable t) {
		super(description, t);
		this.description = description;
		this.errorCode = errorCode;
	}

}

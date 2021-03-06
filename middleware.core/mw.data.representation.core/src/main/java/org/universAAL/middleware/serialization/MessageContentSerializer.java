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
package org.universAAL.middleware.serialization;

/**
 * Classes implementing <code>StringSerializableParser</code> can serialize and
 * deserialize the content bus messages.
 *
 * @author mtazari - <a href="mailto:Saied.Tazari@igd.fraunhofer.de">Saied
 *         Tazari</a>
 *
 */
public interface MessageContentSerializer {

	/**
	 * Deserialize a bus message.
	 *
	 * @param serialized
	 *            serialized object
	 * @return Deserialized content of the bus message.
	 */
	public Object deserialize(String serialized);

	/**
	 * Serialize a bus message.
	 *
	 * @param messageContent
	 *            content to serialize
	 * @return Serialized representation of the given object.
	 */
	public String serialize(Object messageContent);
}

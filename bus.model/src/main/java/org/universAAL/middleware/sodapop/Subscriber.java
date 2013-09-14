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
package org.universAAL.middleware.sodapop;

import org.universAAL.middleware.sodapop.msg.Message;

/**
 * Subscribes to an event-bus for events to handle them and simultaneously may
 * re-ask the publishers on the same bus for more info; hence, it must be able
 * to handle replies, too.
 * 
 * @author mtazari - <a href="mailto:Saied.Tazari@igd.fraunhofer.de">Saied
 *         Tazari</a>
 */
public interface Subscriber extends Caller {

    // TODO describe return value
    /**
     * 
     * @param m
     *            message
     * @return
     */
    public boolean eval(Message m);

    /**
     * 
     * @param m
     *            message (answer) to handle
     */
    public void handleEvent(Message m);
}
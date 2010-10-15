/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.persona.mw.acl.upnp.exporter.actions;

import java.util.Dictionary;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import de.fhg.igd.ima.sodapop.p2p.SodaPopPeer;

/* 
* @author <a href="mailto:francesco.furfari@isti.cnr.it">Francesco Furfari</a>
*/

public class JoinBusAction implements UPnPAction {

	public final static String NAME = "JoinBus";
	public final static String BUS_NAME = "BusName";
	public final static String JOINING_PEER = "JoiningPeer";
	final private String[] IN_ARG_NAMES = new String[]{BUS_NAME,JOINING_PEER};
	private UPnPStateVariable busName,joiningPeer;
	private SodaPopPeer localPeer;
	
	
	public JoinBusAction(SodaPopPeer localPeer,UPnPStateVariable busName, UPnPStateVariable joiningPeer){
		this.busName = busName;
		this.joiningPeer = joiningPeer;
		this.localPeer=localPeer;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getName()
	 */
	public String getName() {
		return NAME;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getReturnArgumentName()
	 */
	public String getReturnArgumentName() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getInputArgumentNames()
	 */
	public String[] getInputArgumentNames() {
		return IN_ARG_NAMES;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getOutputArgumentNames()
	 */
	public String[] getOutputArgumentNames() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#getStateVariable(java.lang.String)
	 */
	public UPnPStateVariable getStateVariable(String argumentName) {
		if (argumentName.equals(BUS_NAME))
			return busName;
		else if (argumentName.equals(JOINING_PEER))
			return joiningPeer;
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see org.osgi.service.upnp.UPnPAction#invoke(java.util.Dictionary)
	 */
	public synchronized Dictionary invoke(Dictionary args) throws Exception {
		String busName = (String) args.get(BUS_NAME);
		String joiningPeer = (String) args.get(JOINING_PEER);
		//System.out.println("LOCAL_PEER:: joinBus invoked ## "+busName +", "+joiningPeer);
		localPeer.joinBus(busName, joiningPeer);
		//System.out.println("LOCAL_PEER:: joinBus returning");
		return null;
	}
}

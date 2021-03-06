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

package org.universAAL.middleware.interfaces.mpa;

/**
 * Wrapper for peer ID and uApp part status:
 *
 * @author <a href="mailto:michele.girolami@isti.cnr.it">Michele Girolami</a>
 * @author <a href="mailto:francesco.furfari@isti.cnr.it">Francesco Furfari</a>
 * @author <a href="mailto:stefano.lenzi@isti.cnr.it">Stefano Lenzi</a>
 * @version $LastChangedRevision$ ( $LastChangedDate$ )
 */
public class Pair {
	String peerID;
	UAPPPartStatus partStatus;

	public Pair(String peerID, UAPPPartStatus partStatus) {
		this.peerID = peerID;
		this.partStatus = partStatus;
	}

	public String getPeerID() {
		return peerID;
	}

	public UAPPPartStatus getPartStatus() {
		return partStatus;
	}

	public void setPeerID(String peerID) {
		this.peerID = peerID;
	}

	public void setPartStatus(UAPPPartStatus partStatus) {
		this.partStatus = partStatus;
	}

}
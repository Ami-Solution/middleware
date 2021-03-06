/*
        Coyright 2007-2014 CNR-ISTI, http://isti.cnr.it
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
package org.universAAL.middleware.modules.communication;

import java.util.List;

import org.universAAL.middleware.connectors.CommunicationConnector;
import org.universAAL.middleware.connectors.exception.CommunicationConnectorException;
import org.universAAL.middleware.connectors.util.ChannelMessage;
import org.universAAL.middleware.connectors.util.ExceptionUtils;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.container.utils.LogUtils;
import org.universAAL.middleware.interfaces.PeerCard;
import org.universAAL.middleware.modules.listener.MessageListener;

/**
 * Thread for sending unicast messages
 *
 * @author <a href="mailto:michele.girolami@isti.cnr.it">Michele Girolami</a>
 * @author <a href="mailto:francesco.furfari@isti.cnr.it">Francesco Furfari</a>
 * @author <a href="mailto:filippo.palumbo@isti.cnr.it">Filippo Palumbo</a>
 * @author <a href="mailto:stefano.lenzi@isti.cnr.it">Stefano "Kismet" Lenzi</a>
 */
public class UnicastExecutor implements Runnable {

	private ChannelMessage message;
	private CommunicationConnector communicationConnector;
	// listeners interested in notifications about the broker message
	private List<MessageListener> listeners;
	private PeerCard receiver;
	private ModuleContext mc;

	public UnicastExecutor(ChannelMessage message, CommunicationConnector communicationConnector, PeerCard receiver,
			List<MessageListener> listeners, ModuleContext moduleContext) {
		super();
		this.message = message;
		this.communicationConnector = communicationConnector;
		this.receiver = receiver;
		this.listeners = listeners;
		this.mc = moduleContext;
	}

	public void run() {

		try {
			LogUtils.logInfo(mc, UnicastExecutor.class, "run()", new Object[] {
					"Preparing to send data to " + receiver.getPeerID() + " the message to deliver is " + message },
					null);
			communicationConnector.unicast(message, receiver.getPeerID());
		} catch (CommunicationConnectorException e) {
			for (MessageListener listener : listeners)
				listener.handleSendError(message, e);
		} catch (Throwable t) {
			final String msg = ExceptionUtils.stackTraceAsString(t);
			final CommunicationConnectorException e = new CommunicationConnectorException(-1, msg);
			for (MessageListener listener : listeners)
				listener.handleSendError(message, e);
		}
	}

}

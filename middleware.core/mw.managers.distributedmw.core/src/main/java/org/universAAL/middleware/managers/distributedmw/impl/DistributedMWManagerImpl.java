/*
	Copyright 2007-2016 Fraunhofer IGD, http://www.igd.fraunhofer.de
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
package org.universAAL.middleware.managers.distributedmw.impl;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.universAAL.middleware.brokers.message.distributedmw.DistributedMWMessage;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.container.utils.LogUtils;
import org.universAAL.middleware.interfaces.PeerCard;
import org.universAAL.middleware.managers.api.DistributedMWEventHandler;
import org.universAAL.middleware.managers.distributedmw.api.DistributedBusMemberListener;
import org.universAAL.middleware.managers.distributedmw.api.DistributedLogListener;
import org.universAAL.middleware.managers.distributedmw.api.DistributedBusMemberManager;
import org.universAAL.middleware.managers.distributedmw.api.DistributedLogManager;
import org.universAAL.middleware.rdf.Resource;

/**
 *
 * @author Carsten Stockloew
 *
 */
public class DistributedMWManagerImpl implements DistributedBusMemberManager, DistributedLogManager {
	// TODO: split this implementation in two for each interface for security
	// reasons

	public static final String NAMESPACE = Resource.NAMESPACE_PREFIX + "DistributedMWManager.rdf#";

	public static PeerCard myPeer;
	public static ModuleContext context;
	public static SharedObjectConnector shared;
	private LogListenerHandler logListenerHandler = null;
	private BusMemberListenerHandler busMemberListenerHandler = null;
	private Object[] removeParamsBMLMgmt;
	private Object[] removeParamsLLMgmt;
	private Object[] removeParamsEvtH;

	interface Handler {
		void handle(PeerCard sender, Resource r);
	};

	private DistributedMWEventHandler handler = null;

	private class MyDistributedMWEventHandler implements DistributedMWEventHandler {
		public HashMap<String, Handler> handlers = new HashMap<String, Handler>();

		{
			handlers.put(LogListenerHandler.TYPE_ADD_LOGLISTENER, logListenerHandler.new AddListenerHandler());
			handlers.put(LogListenerHandler.TYPE_REMOVE_LOGLISTENER, logListenerHandler.new RemoveListenerHandler());
			handlers.put(LogListenerHandler.TYPE_LOGLISTENER_MESSAGE,
					logListenerHandler.new LogListenerMessageHandler());

			handlers.put(BusMemberListenerHandler.TYPE_ADD_BUSMEMBER_LISTENER,
					busMemberListenerHandler.new AddListenerHandler());
			handlers.put(BusMemberListenerHandler.TYPE_REMOVE_BUSMEMBER_LISTENER,
					busMemberListenerHandler.new RemoveListenerHandler());
			handlers.put(BusMemberListenerHandler.TYPE_BUSMEMBER_ADDED,
					busMemberListenerHandler.new BusMemberAddedMessageHandler());
			handlers.put(BusMemberListenerHandler.TYPE_BUSMEMBER_REMOVED,
					busMemberListenerHandler.new BusMemberRemovedMessageHandler());
			handlers.put(BusMemberListenerHandler.TYPE_BUSMEMBER_PARAMS_ADDED,
					busMemberListenerHandler.new RegParamsAddedMessageHandler());
			handlers.put(BusMemberListenerHandler.TYPE_BUSMEMBER_PARAMS_REMOVED,
					busMemberListenerHandler.new RegParamsRemovedMessageHandler());
		}

		public void handleMessage(PeerCard sender, DistributedMWMessage msg) {
			Object obj = shared.getMessageContentSerializer().deserialize(msg.getPayload());
			if (!(obj instanceof Resource)) {
				LogUtils.logError(context, DistributedMWManagerImpl.class, "handleMessage",
						"Received an object that is not a Resource. Ignoring it.");
				return;
			}
			Resource r = (Resource) obj;
			String type = r.getType();
			Handler h = handlers.get(type);
			if (h != null) {
				h.handle(sender, r);
			} else {
				LogUtils.logWarn(context, DistributedMWManagerImpl.class, "handleMessage",
						"DistributedMWMessage has unknown type. Ignoring it.");
				return;
			}
		}
	};

	public DistributedMWManagerImpl(ModuleContext context, Object[] shareParamsBMLMgmt, Object[] removeParamsBMLMgmt,
			Object[] shareParamsLLMgmt, Object[] removeParamsLLMgmt, Object[] shareParamsEvtH,
			Object[] removeParamsEvtH) {
		DistributedMWManagerImpl.context = context;
		shared = new SharedObjectConnector(context);
		myPeer = shared.getSpaceManager().getMyPeerCard();
		this.removeParamsBMLMgmt = removeParamsBMLMgmt;
		this.removeParamsLLMgmt = removeParamsLLMgmt;
		this.removeParamsEvtH = removeParamsEvtH;

		logListenerHandler = new LogListenerHandler();
		busMemberListenerHandler = new BusMemberListenerHandler();
		handler = new MyDistributedMWEventHandler();

		// register manager as shared object
		context.getContainer().shareObject(context, this, shareParamsBMLMgmt);
		context.getContainer().shareObject(context, this, shareParamsLLMgmt);
		context.getContainer().shareObject(context, handler, shareParamsEvtH);
	}

	public void addListener(DistributedLogListener listener, List<PeerCard> nodes) {
		logListenerHandler.addListener(listener, nodes);
	}

	public void removeListener(DistributedLogListener listener, List<PeerCard> nodes) {
		logListenerHandler.removeListener(listener, nodes);
	}

	public void addListener(DistributedBusMemberListener listener, List<PeerCard> nodes) {
		busMemberListenerHandler.addListener(listener, nodes);
	}

	public void removeListener(DistributedBusMemberListener listener, List<PeerCard> nodes) {
		busMemberListenerHandler.removeListener(listener, nodes);
	}

	@SuppressWarnings("rawtypes")
	public void loadConfigurations(Dictionary configurations) {
	}

	public boolean init() {
		return true;
	}

	public void dispose() {
		context.getContainer().removeSharedObject(context, this, removeParamsLLMgmt);
		context.getContainer().removeSharedObject(context, this, removeParamsBMLMgmt);
		context.getContainer().removeSharedObject(context, handler, removeParamsEvtH);
		MySpaceListener.getInstance().stop();
	}

	public static void sendMessage(Resource r, Set<PeerCard> receivers) {
		sendMessage(r, new ArrayList<PeerCard>(receivers));
	}

	public static void sendMessage(Resource r, List<PeerCard> receivers) {
		if (r == null || receivers == null || receivers.size() == 0)
			return;

		String serialized = shared.getMessageContentSerializer().serialize(r);
		DistributedMWMessage msg = new DistributedMWMessage(serialized);

		shared.getControlBroker().sendMessage(msg, receivers);
	}

	public static void sendMessage(Resource r, PeerCard receiver) {
		List<PeerCard> receivers = new ArrayList<PeerCard>();
		receivers.add(receiver);
		sendMessage(r, receivers);
	}
}

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
import java.util.Arrays;
import java.util.Set;

import org.universAAL.middleware.bus.member.BusMember;
import org.universAAL.middleware.bus.member.BusMemberType;
import org.universAAL.middleware.bus.model.AbstractBus;
import org.universAAL.middleware.context.ContextBusFacade;
import org.universAAL.middleware.interfaces.PeerCard;
import org.universAAL.middleware.managers.distributedmw.api.DistributedBusMemberListener;
import org.universAAL.middleware.managers.distributedmw.impl.DistributedMWManagerImpl.Handler;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.service.ServiceBusFacade;
import org.universAAL.middleware.tracker.IBusMemberRegistry;
import org.universAAL.middleware.tracker.IBusMemberRegistry.BusType;
import org.universAAL.middleware.tracker.IBusMemberRegistryListener;
import org.universAAL.middleware.ui.UIBusFacade;

/**
 *
 * @author Carsten Stockloew
 *
 */
public class BusMemberListenerHandler extends ListenerHandler<DistributedBusMemberListener> {
	public static final String TYPE_ADD_BUSMEMBER_LISTENER = DistributedMWManagerImpl.NAMESPACE
			+ "addBusMemberListener";
	public static final String TYPE_REMOVE_BUSMEMBER_LISTENER = DistributedMWManagerImpl.NAMESPACE
			+ "removeBusMemberListener";
	public static final String TYPE_BUSMEMBER_ADDED = DistributedMWManagerImpl.NAMESPACE + "BusMemberAdded";
	public static final String TYPE_BUSMEMBER_REMOVED = DistributedMWManagerImpl.NAMESPACE + "BusMemberRemoved";
	public static final String TYPE_BUSMEMBER_PARAMS_ADDED = DistributedMWManagerImpl.NAMESPACE
			+ "BusMemberParamsAdded";
	public static final String TYPE_BUSMEMBER_PARAMS_REMOVED = DistributedMWManagerImpl.NAMESPACE
			+ "BusMemberParamsRemoved";

	public static final String PROP_BUS_NAME = DistributedMWManagerImpl.NAMESPACE + "busName";
	public static final String PROP_MEMBER_TYPE = DistributedMWManagerImpl.NAMESPACE + "memberType";
	public static final String PROP_PARAMS = DistributedMWManagerImpl.NAMESPACE + "params";
	public static final String MEMBER_TYPE_PREFIX = DistributedMWManagerImpl.NAMESPACE + "memberType_";

	private LocalListener localListener = new LocalListener();

	public class BusMemberAddedMessageHandler implements Handler {
		public void handle(PeerCard sender, Resource r) {
			// a remote peer, to which we subscribed, sent us a message
			// -> notify all listeners
			String busMemberID = r.getURI();
			String busName = (String) r.getProperty(PROP_BUS_NAME);
			String label = r.getResourceLabel();
			String comment = r.getResourceComment();
			BusMemberType memberType = BusMemberType
					.valueOf(((String) r.getProperty(PROP_MEMBER_TYPE)).substring(MEMBER_TYPE_PREFIX.length()));

			Set<DistributedBusMemberListener> st = null;
			synchronized (listeners) {
				st = listeners.get(sender);
				if (st == null || st.size() == 0)
					return;
				// dispatch message
				for (DistributedBusMemberListener l : st) {
					l.busMemberAdded(sender, busMemberID, busName, memberType, label, comment);
				}
			}
		}
	}

	public class BusMemberRemovedMessageHandler implements Handler {
		public void handle(PeerCard sender, Resource r) {
			// a remote peer, to which we subscribed, sent us a message
			// -> notify all listeners
			Set<DistributedBusMemberListener> st = null;
			synchronized (listeners) {
				st = listeners.get(sender);
				if (st == null || st.size() == 0)
					return;
				// dispatch message
				for (DistributedBusMemberListener l : st) {
					l.busMemberRemoved(sender, r.getURI());
				}
			}
		}
	}

	public class RegParamsAddedMessageHandler implements Handler {
		public void handle(PeerCard sender, Resource r) {
			// a remote peer, to which we subscribed, sent us a message
			// -> notify all listeners
			Set<DistributedBusMemberListener> st = null;
			synchronized (listeners) {
				st = listeners.get(sender);
				if (st == null || st.size() == 0)
					return;
				// dispatch message
				for (DistributedBusMemberListener l : st) {
					l.regParamsAdded(sender, r.getURI(), getParams(r));
				}
			}
		}
	}

	private Resource[] getParams(Resource r) {
		ArrayList<Object> arr = (ArrayList<Object>) r.getProperty(PROP_PARAMS);
		if (arr == null)
			return null;
		return arr.toArray(new Resource[0]);
	}

	public class RegParamsRemovedMessageHandler implements Handler {
		public void handle(PeerCard sender, Resource r) {
			// a remote peer, to which we subscribed, sent us a message
			// -> notify all listeners
			Set<DistributedBusMemberListener> st = null;
			synchronized (listeners) {
				st = listeners.get(sender);
				if (st == null || st.size() == 0)
					return;
				// dispatch message
				for (DistributedBusMemberListener l : st) {
					l.regParamsRemoved(sender, r.getURI(), getParams(r));
				}
			}
		}
	}

	public class LocalListener implements IBusMemberRegistryListener {
		public void busMemberAdded(BusMember member, BusType type) {
			// init info
			String busMemberID = member.getURI();
			String busName = "";
			// TODO: performance - make this only once
			switch (type) {
			case Service:
				busName = ((AbstractBus) (ServiceBusFacade.fetchBus(DistributedMWManagerImpl.context))).getBrokerName();
				break;
			case Context:
				busName = ((AbstractBus) (ContextBusFacade.fetchBus(DistributedMWManagerImpl.context))).getBrokerName();
				break;
			case UI:
				busName = ((AbstractBus) (UIBusFacade.fetchBus(DistributedMWManagerImpl.context))).getBrokerName();
				break;
			}
			String label = member.getLabel();
			String comment = member.getComment();
			BusMemberType memberType = member.getType();

			// local subscriptions
			synchronized (localListeners) {
				for (DistributedBusMemberListener l : localListeners) {
					l.busMemberAdded(DistributedMWManagerImpl.myPeer, busMemberID, busName, memberType, label, comment);
				}
			}

			// remote subscriptions
			synchronized (subscribers) {
				if (subscribers.size() != 0) {
					Resource r = new Resource(busMemberID);
					r.addType(TYPE_BUSMEMBER_ADDED, true);
					r.setProperty(PROP_BUS_NAME, busName);
					r.setProperty(PROP_MEMBER_TYPE, MEMBER_TYPE_PREFIX + memberType.toString());
					r.setResourceLabel(label);
					r.setResourceComment(comment);
					DistributedMWManagerImpl.sendMessage(r, subscribers);
				}
			}
		}

		public void busMemberRemoved(BusMember member, BusType type) {
			// local subscriptions
			synchronized (localListeners) {
				for (DistributedBusMemberListener l : localListeners) {
					l.busMemberRemoved(DistributedMWManagerImpl.myPeer, member.getURI());
				}
			}

			// remote subscriptions
			synchronized (subscribers) {
				if (subscribers.size() != 0) {
					Resource r = new Resource(member.getURI());
					r.addType(TYPE_BUSMEMBER_REMOVED, true);
					DistributedMWManagerImpl.sendMessage(r, subscribers);
				}
			}
		}

		public void regParamsAdded(String busMemberID, Resource[] params) {
			// local subscriptions
			synchronized (localListeners) {
				for (DistributedBusMemberListener l : localListeners) {
					l.regParamsAdded(DistributedMWManagerImpl.myPeer, busMemberID, params);
				}
			}

			// remote subscriptions
			synchronized (subscribers) {
				if (subscribers.size() != 0) {
					Resource r = new Resource(busMemberID);
					r.addType(TYPE_BUSMEMBER_PARAMS_ADDED, true);
					if (params != null) {
						r.setProperty(PROP_PARAMS, new ArrayList<Object>(Arrays.asList(params)));
					}
					DistributedMWManagerImpl.sendMessage(r, subscribers);
				}
			}
		}

		public void regParamsRemoved(String busMemberID, Resource[] params) {
			// local subscriptions
			synchronized (localListeners) {
				for (DistributedBusMemberListener l : localListeners) {
					l.regParamsRemoved(DistributedMWManagerImpl.myPeer, busMemberID, params);
				}
			}

			// remote subscriptions
			synchronized (subscribers) {
				if (subscribers.size() != 0) {
					Resource r = new Resource(busMemberID);
					r.addType(TYPE_BUSMEMBER_PARAMS_REMOVED, true);
					if (params != null) {
						r.setProperty(PROP_PARAMS, new ArrayList<Object>(Arrays.asList(params)));
					}
					DistributedMWManagerImpl.sendMessage(r, subscribers);
				}
			}
		}
	}

	public BusMemberListenerHandler() {
		super(TYPE_ADD_BUSMEMBER_LISTENER, TYPE_REMOVE_BUSMEMBER_LISTENER);
	}

	@Override
	protected void addListenerLocally() {
		synchronized (this) {
			IBusMemberRegistry registry = (IBusMemberRegistry) DistributedMWManagerImpl.context.getContainer()
					.fetchSharedObject(DistributedMWManagerImpl.context, IBusMemberRegistry.busRegistryShareParams);
			registry.addListener(localListener, true);
		}
	}

	@Override
	protected void removeListenerLocally() {
		synchronized (this) {
			IBusMemberRegistry registry = (IBusMemberRegistry) DistributedMWManagerImpl.context.getContainer()
					.fetchSharedObject(DistributedMWManagerImpl.context, IBusMemberRegistry.busRegistryShareParams);
			registry.removeListener(localListener);
		}
	}
}

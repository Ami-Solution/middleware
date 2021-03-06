/*******************************************************************************
 * Copyright 2013 Universidad Politécnica de Madrid
 * Copyright 2013 Fraunhofer-Gesellschaft - Institute for Computer Graphics Research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.universAAL.middleware.ui.impl.generic;

import java.util.Random;

import org.universAAL.middleware.bus.msg.BusMessage;
import org.universAAL.middleware.container.SharedObjectListener;
import org.universAAL.middleware.container.utils.LogUtils;
import org.universAAL.middleware.interfaces.PeerCard;
import org.universAAL.middleware.interfaces.space.SpaceDescriptor;
import org.universAAL.middleware.interfaces.space.SpaceStatus;
import org.universAAL.middleware.managers.api.SpaceListener;
import org.universAAL.middleware.managers.api.SpaceManager;
import org.universAAL.middleware.modules.CommunicationModule;
import org.universAAL.middleware.owl.Ontology;
import org.universAAL.middleware.owl.OntologyManagement;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.rdf.ResourceFactory;

/**
 * Strategy stack dedicated only to manage Coordination of the bus.
 *
 * <br>
 * This strategy consists of 3 basic events:
 * <ol>
 * <li>Coordinator_Broadcast : used to announce coordinator and respond to
 * coordinator requests
 * <li>Coordinator_Request: used to request a coordinator when there is not one
 * yet
 * <li>Coordinator_Resign: used by the coordinator instance to resign from being
 * a coordinator.
 * </ol>
 * <center>
 * <img style="background-color:white;" src="doc-files/CoordinatedStrategy.png"
 * alt="UIStrategy messages" width="70%"/> </center> The strategy subscribes to
 * the {@link SpaceManager} to listen to
 * {@link CoordinatedStrategy#peerLost(PeerCard) lost peers}, in case they are
 * the coordinator; or if the Coordinator has left the coordinated space, it
 * automatically surrenders coordination. Also
 * {@link CoordinatedStrategy#peerJoined(PeerCard) new Peers} joining the
 * space will proactively be informed about who is the coordinator.
 *
 * @author amedrano
 *
 */
public class CoordinatedStrategy extends CallBasedStrategy implements SpaceListener, SharedObjectListener {

	/**
	 *
	 */
	private static final int RESEND_INTERVAL = 10000;

	public static final String TYPE_UI_BUS_COORDINATOR_BROADCAST = Resource.VOCABULARY_NAMESPACE
			+ "IamCoordinator";

	public static final String TYPE_UI_BUS_COORDINATOR_REQUEST = Resource.VOCABULARY_NAMESPACE
			+ "WhoIsCoordinator";

	public static final String TYPE_UI_BUS_COORDINATOR_RESIGN = Resource.VOCABULARY_NAMESPACE
			+ "CoordinatorResign";

	private class CoordinatorAnnounceEvent extends Resource implements EventMessage<CoordinatedStrategy> {

		public CoordinatorAnnounceEvent() {
			addType(TYPE_UI_BUS_COORDINATOR_BROADCAST, true);
		}

		/** {@ inheritDoc} */
		public void onReceived(CoordinatedStrategy strategy, BusMessage m, String senderID) {

			// this should be a notification from the side of the
			// coordinator announcing its role
			// => this is the announcement
			PeerCard solicitor = m.getSender();
			if (solicitor != coordinatorPeer && iAmCoordinator()) {
				// WOW! another peer has asked to be coordinator!!
				LogUtils.logInfo(busModule, getClass(), "handle",
						"detected multiple peers requesing to be coordinator");
				Random r = new Random(bus.getPeerCard().getPeerID().hashCode());
				int waitms = 50 + r.nextInt() % 950;
				if (waitms < 50)
					waitms = 50;// Just in case it becomes <0 (!?)
				try {
					// wait between 50 and 1000 ms
					Thread.sleep(waitms);
					// then retry
					requestBecomeACoordinator();
				} catch (InterruptedException e) {
				} catch (CoordinatorAlreadyExistsException e) {
					LogUtils.logInfo(busModule, getClass(), "handle",
							"rejected on second attempt, could not become Coordinator");
					sendEventToRemoteBusMember(new CoordinatorResignEvent());
				}
			} else {
				synchronized (CoordinatedStrategy.this) {
					setCoordinatorPeer(solicitor);
					CoordinatedStrategy.this.notifyAll();
				}
			}
		}
	}

	private class CoordinatorRequestEvent extends Resource implements EventMessage<CoordinatedStrategy> {

		public CoordinatorRequestEvent() {
			addType(TYPE_UI_BUS_COORDINATOR_REQUEST, true);
		}

		/** {@ inheritDoc} */
		public void onReceived(CoordinatedStrategy strategy, BusMessage m, String senderID) {
			if (iAmCoordinator()) {
				sendEventToRemoteBusMember(new CoordinatorAnnounceEvent());
			}
		}
	}

	private class CoordinatorResignEvent extends Resource implements EventMessage<CoordinatedStrategy> {

		public CoordinatorResignEvent() {
			addType(TYPE_UI_BUS_COORDINATOR_RESIGN, true);
		}

		/** {@ inheritDoc} */
		public void onReceived(CoordinatedStrategy strategy, BusMessage m, String senderID) {
			if (coordinatorPeer == m.getSender()) {
				setCoordinatorPeer(null);
			}
		}
	}

	private class CoordinatorMessageFactory implements ResourceFactory {

		/** {@ inheritDoc} */
		public Resource createInstance(String classURI, String instanceURI, int factoryIndex) {
			switch (factoryIndex) {
			case 0:
				return new CoordinatorAnnounceEvent();
			case 1:
				return new CoordinatorResignEvent();
			case 2:
				return new CoordinatorRequestEvent();
			default:
				break;
			}
			return null;
		}

	}

	private class CoordinatorMessageOnt extends Ontology {
		private CoordinatorMessageFactory factory;

		/**
		 * @param ontURI
		 */
		public CoordinatorMessageOnt(String ontURI) {
			super(ontURI);
			factory = new CoordinatorMessageFactory();
		}

		/** {@ inheritDoc} */
		public void create() {
			createNewRDFClassInfo(TYPE_UI_BUS_COORDINATOR_BROADCAST, factory, 0);
			createNewRDFClassInfo(TYPE_UI_BUS_COORDINATOR_RESIGN, factory, 1);
			createNewRDFClassInfo(TYPE_UI_BUS_COORDINATOR_REQUEST, factory, 2);
		}
	}

	/**
	 * Exception indicating there is already a Coordinator.
	 *
	 * @author amedrano
	 */
	public static class CoordinatorAlreadyExistsException extends Exception {

		/**
		 *
		 */
		private static final long serialVersionUID = 1L;
		PeerCard existingCoordinator;

		/**
		 * @return the existingCoordinator
		 */
		public PeerCard getExistingCoordinator() {
			return existingCoordinator;
		}

		/**
		 * @param existingCoordinator
		 *            the existingCoordinator to set
		 */
		public void setExistingCoordinator(PeerCard existingCoordinator) {
			this.existingCoordinator = existingCoordinator;
		}

	}

	private PeerCard coordinatorPeer;

	/**
	 * @param coordinatorPeer
	 *            the coordinatorPeer to set
	 */
	private final void setCoordinatorPeer(PeerCard coordinatorPeer) {
		this.coordinatorPeer = coordinatorPeer;
		if (this.coordinatorPeer == null) {
			lostCoordinator();
		}
	}

	/**
	 * Extending classes may override this method to perform any opertation
	 * needed when the coordinator is lost.
	 */
	protected void lostCoordinator() {
	}

	private CoordinatorMessageOnt ontology;

	/**
	 * @param commModule
	 */
	public CoordinatedStrategy(CommunicationModule commModule) {
		super(commModule);
	}

	/**
	 * @param commModule
	 * @param name
	 */
	public CoordinatedStrategy(CommunicationModule commModule, String name) {
		super(commModule, name);
	}

	/** {@ inheritDoc} */
	public synchronized void start() {
		super.start();
		ontology = new CoordinatorMessageOnt(Resource.NAMESPACE_PREFIX + "CoordinatedStrategyMessageOntology");
		OntologyManagement.getInstance().register(busModule, ontology);
		if (busModule.getContainer() != null) {
			Object o = busModule.getContainer().fetchSharedObject(busModule,
					new Object[] { SpaceManager.class.getName() }, this);
			sharedObjectAdded(o, null);
		}
	}

	protected final synchronized boolean iAmCoordinator() {
		return coordinatorPeer != null && coordinatorPeer.equals(bus.getPeerCard());
	}

	/**
	 * Check the conditions and announce this Peer as coordinator.
	 *
	 * @throws CoordinatorAlreadyExistsException
	 */
	protected final void requestBecomeACoordinator() throws CoordinatorAlreadyExistsException {
		if (coordinatorPeer != null && coordinatorPeer != bus.getPeerCard()) {
			CoordinatorAlreadyExistsException ex = new CoordinatorAlreadyExistsException();
			ex.setExistingCoordinator(coordinatorPeer);
			throw ex;
		}
		if (coordinatorPeer == null) {
			setCoordinatorPeer(bus.getPeerCard());
			sendEventToRemoteBusMember(new CoordinatorAnnounceEvent());
		}
		if (!iAmCoordinator()) {
			CoordinatorAlreadyExistsException ex = new CoordinatorAlreadyExistsException();
			ex.setExistingCoordinator(coordinatorPeer);
			throw ex;
		} else {
			synchronized (this) {
				notifyAll();
			}
		}
	}

	/**
	 * Check and announce this Peer is no longer the coordinator.
	 *
	 * @throws CoordinatorAlreadyExistsException
	 *             in case this peer is not the coordinator.
	 */
	protected final void resignFromCoordinator() throws CoordinatorAlreadyExistsException {
		if (coordinatorPeer != null && coordinatorPeer != bus.getPeerCard()) {
			CoordinatorAlreadyExistsException ex = new CoordinatorAlreadyExistsException();
			ex.setExistingCoordinator(coordinatorPeer);
			throw ex;
		}
		if (coordinatorPeer != null) {
			setCoordinatorPeer(null);
			sendEventToRemoteBusMember(new CoordinatorResignEvent());
		}
	}

	public final PeerCard getCoordinator() {
		if (coordinatorPeer != null) {
			return coordinatorPeer;
		}
		// let's see if any other instance claims to be the coordinator
		synchronized (this) {
			while (coordinatorPeer == null) {
				sendEventToRemoteBusMember(new CoordinatorRequestEvent());
				try {
					// now wait until either the reply comes and notifies me
					// or the dialog manager connects which will also lead
					// to awakening this thread
					wait(RESEND_INTERVAL);
				} catch (Exception e) {
				}
			}
		}
		return coordinatorPeer;
	}

	/** {@ inheritDoc} */
	public void spaceJoined(SpaceDescriptor spaceDescriptor) {
		/*
		 * NOTHING, wait for notification of the coordinator or for one of the
		 * busmembers to request a coordinator
		 */
	}

	/** {@ inheritDoc} */
	public void spaceLost(SpaceDescriptor spaceDescriptor) {
		/*
		 * I have left the Space. If i was the coordinator, no longer
		 */
		if (iAmCoordinator()) {
			synchronized (this) {
				setCoordinatorPeer(null);
				LogUtils.logInfo(busModule, getClass(), "SpaceLost", "Lost the space to Coordinate.");
				// TODO give a notification? so coordination may be reattempted.
			}
		}
	}

	/** {@ inheritDoc} */
	public void peerJoined(PeerCard peer) {
		/*
		 * A new Peer has joined, If i am the coordinator show him that I am the
		 * king of the place
		 */
		if (iAmCoordinator() && peer != bus.getPeerCard()) {
			// wellcome
			sendEventToRemoteBusMember(peer, new CoordinatorAnnounceEvent());
			// kneel before my presence!
			// muahahaHAHA
		}
	}

	/** {@ inheritDoc} */
	public void peerLost(PeerCard peer) {
		/*
		 * If the lost peer is the coordinator, well have to wait for a new one
		 */
		if (peer == coordinatorPeer) {
			setCoordinatorPeer(null);
			LogUtils.logInfo(busModule, getClass(), "peerLost", "Lost the Coordinator.");
		}
	}

	/** {@ inheritDoc} */
	public void spaceStatusChanged(SpaceStatus status) {
	}

	/** {@ inheritDoc} */
	public void sharedObjectAdded(Object sharedObj, Object removeHook) {
		if (sharedObj instanceof SpaceManager) {
			((SpaceManager) sharedObj).addSpaceListener(this);
		}
	}

	/** {@ inheritDoc} */
	public void sharedObjectRemoved(Object removeHook) {
	}

	/** Tearing down */
	public void close() {
		if (iAmCoordinator()) {
			sendEventToRemoteBusMember(new CoordinatorResignEvent());
		}
		OntologyManagement.getInstance().unregister(busModule, ontology);
		Object o = busModule.getContainer().fetchSharedObject(busModule,
				new Object[] { SpaceManager.class.getName() });
		if (o instanceof SpaceManager) {
			((SpaceManager) o).removeSpaceListener(this);
		}
	}

}

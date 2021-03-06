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
package org.universAAL.middleware.managers.deploy;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.swing.LayoutFocusTraversalPolicy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.universAAL.middleware.brokers.control.ControlBroker;
import org.universAAL.middleware.brokers.control.ExceptionUtils;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.container.SharedObjectListener;
import org.universAAL.middleware.container.utils.LogUtils;
import org.universAAL.middleware.interfaces.PeerCard;
import org.universAAL.middleware.interfaces.mpa.UAPPCard;
import org.universAAL.middleware.interfaces.mpa.UAPPPartStatus;
import org.universAAL.middleware.interfaces.space.SpaceDescriptor;
import org.universAAL.middleware.interfaces.space.SpaceStatus;
import org.universAAL.middleware.interfaces.utils.Util;
import org.universAAL.middleware.managers.api.SpaceEventHandler;
import org.universAAL.middleware.managers.api.SpaceListener;
import org.universAAL.middleware.managers.api.SpaceManager;
import org.universAAL.middleware.managers.api.DeployManager;
import org.universAAL.middleware.managers.api.DeployManagerEventHandler;
import org.universAAL.middleware.managers.api.InstallationResults;
import org.universAAL.middleware.managers.api.InstallationResultsDetails;
import org.universAAL.middleware.managers.api.UAPPPackage;
import org.universAAL.middleware.managers.deploy.uapp.model.AalUapp;
import org.universAAL.middleware.managers.deploy.uapp.model.ObjectFactory;
import org.universAAL.middleware.managers.deploy.uapp.model.Part;
import org.universAAL.middleware.managers.deploy.util.Consts;

/**
 * The implementation of the DeployManager
 *
 * @author <a href="mailto:michele.girolami@isti.cnr.it">Michele Girolami</a>
 * @author <a href="mailto:francesco.furfari@isti.cnr.it">Francesco Furfari</a>
 * @author <a href="mailto:stefano.lenzi@isti.cnr.it">Stefano Lenzi</a>
 * @author Carsten Stockloew
 * @version $LastChangedRevision$ ( $LastChangedDate$ )
 */
public class DeployManagerImpl
		implements DeployManager, DeployManagerEventHandler, SharedObjectListener, SpaceListener {

	private SpaceEventHandler spaceEventHandler;
	private SpaceManager spaceManager;
	private ControlBroker controlBroker;
	private ModuleContext context;
	private boolean initialized = false;

	// Configuration param configured with default value
	private String uappSuffix = ".uapp";
	private final String deployDir;

	// JAXB
	private JAXBContext jc;
	private Unmarshaller unmarshaller;
	private Marshaller marshaller;

	private boolean isDeployCoordinator = false;
	private HashMap<String, UAPPPackage> wip = new HashMap<String, UAPPPackage>();
	private HashMap<String, Long> installingParts = new HashMap<String, Long>();
	private HashMap<String, Long> uninstallingParts = new HashMap<String, Long>();
	private final Properties applicationRegistry = new Properties();
	private File applicationRegistryFile = null;

	private final long TIMEOUT;

	public DeployManagerImpl(ModuleContext context) {
		TIMEOUT = Long.parseLong(System.getProperty("uAAL.dm.timeout", "" + 5 * 60 * 1000));
		this.context = context;
		this.deployDir = context.getConfigHome().getAbsolutePath();
		LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
				new Object[] { "Deploy Manager folder: ", deployDir }, null);
		init();
		try {
			jc = JAXBContext.newInstance(ObjectFactory.class);
			unmarshaller = jc.createUnmarshaller();
			marshaller = jc.createMarshaller();
		} catch (JAXBException e) {
			LogUtils.logError(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "Error during creation of marshaller: " + e }, null);
		}

	}

	public boolean init() {
		if (!initialized) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "Initializing the DeployManager..." }, null);

			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "fetching the SpaceManager..." }, null);
			Object[] managers = context.getContainer().fetchSharedObject(context,
					new Object[] { SpaceManager.class.getName().toString() }, this);
			if (managers != null) {
				spaceManager = (SpaceManager) managers[0];
				spaceManager.addSpaceListener(this);

				// check if I'm the deploy coordinator
				if (spaceManager.getSpaceDescriptor() != null && spaceManager.getSpaceDescriptor()
						.getDeployManager().getPeerID().equals(spaceManager.getMyPeerCard().getPeerID())) {
					isDeployCoordinator = true;
				}
			} else {
				LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
						new Object[] { "No SpaceManagers found" }, null);
				initialized = false;
				return initialized;
			}

			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "fetching the SpaceEventHandler..." }, null);
			Object[] eventHandlers = context.getContainer().fetchSharedObject(context,
					new Object[] { SpaceManager.class.getName().toString() }, this);
			if (eventHandlers != null) {
				spaceEventHandler = (SpaceEventHandler) eventHandlers[0];

			} else {
				LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
						new Object[] { "No SpaceEventHandler found" }, null);
				initialized = false;
				return initialized;
			}

			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "fetching the ControlBroker..." }, null);
			Object[] cBrokers = context.getContainer().fetchSharedObject(context,
					new Object[] { ControlBroker.class.getName().toString() }, this);
			if (cBrokers != null) {
				LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
						new Object[] { "Found  ContextBrokers..." }, null);
				if (cBrokers[0] instanceof ControlBroker)
					controlBroker = (ControlBroker) cBrokers[0];
				if (spaceManager.getSpaceDescriptor() == null) {
					initialized = false;
					return initialized;
				}
			} else {
				LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
						new Object[] { "No ContextBroker found" }, null);
				initialized = false;
				return initialized;
			}

			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "DeployManager initialized" }, null);

		} else {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "DeployManager already initialized" }, null);
		}

		return initialized;
	}

	public InstallationResultsDetails requestToInstall(UAPPPackage application) {
		InstallationResultsDetails result = new InstallationResultsDetails(InstallationResults.FAILURE);
		try {
			InstallationResults global = m_requestToInstall(application, result);
			result.setGlobalResult(global);
		} catch (Exception ex) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "requestToInstall", new Object[] {
					"Failed to install the application due to execption ", ExceptionUtils.stackTraceAsString(ex) }, ex);
		}
		synchronized (wip) {
			wip.remove(application.getServiceId() + ":" + application.getId());
		}
		return result;
	}

	private InstallationResults m_requestToInstall(UAPPPackage application, InstallationResultsDetails result) {

		// checks
		// 1 - get the MPA file
		if (application == null) {
			LogUtils.logWarn(context, DeployManagerImpl.class, "requestToInstall",
					new Object[] { "The application object is null...aborting" }, null);
			return InstallationResults.UAPP_URI_INVALID;
		}

		final Map<PeerCard, List<Part>> layout = application.getDeploy();
		String appSrcDir = application.getFolder().toString();
		if (appSrcDir.endsWith("/")) {
			appSrcDir = appSrcDir + Consts.APPLICATION_CONFIGURATION_PATH;
		} else {
			appSrcDir = appSrcDir + "/" + Consts.APPLICATION_CONFIGURATION_PATH;
		}
		URI applicationConfigurationFolder = null;
		try {
			LogUtils.logInfo(context, DeployManagerImpl.class, "requestToInstall",
					new Object[] { "Trying to access to application folder identified by the URI " + appSrcDir }, null);
			applicationConfigurationFolder = new URI(appSrcDir);
		} catch (URISyntaxException e1) {
			LogUtils.logError(context, DeployManagerImpl.class, "requestToInstall",
					new Object[] { "The application configuration path is null...aborting: " + e1.toString() }, null);
			return InstallationResults.UAPP_URI_INVALID;
		}
		if (application == null || applicationConfigurationFolder == null || layout == null) {
			LogUtils.logWarn(context, DeployManagerImpl.class, "requestToInstall",
					new Object[] { "The deploy folder or layout are null...aborting" }, null);
			return InstallationResults.UAPP_URI_INVALID;
		}
		// 2 - verify If I belong to an Space
		if (spaceManager.getSpaceDescriptor() == null)
			return InstallationResults.NO_SPACE_JOINED;

		// 3 - verify if I'm the DeployCoordinator
		if (isDeployCoordinator() == false) {
			return InstallationResults.NOT_A_DEPLOYMANAGER;
		}

		File uappFile = Util.getFile(uappSuffix, applicationConfigurationFolder);
		AalUapp uapp = null;

		if (uappFile != null && uappFile.canRead()) {
			try {
				uapp = (AalUapp) unmarshaller.unmarshal(uappFile);
			} catch (JAXBException e) {
				LogUtils.logError(context, DeployManagerImpl.class, "requestToInstall",
						new Object[] { "uAAP file cannot be parsed. Aborting..." }, null);
				return InstallationResults.MPA_FILE_NOT_VALID;
			}
		} else {
			LogUtils.logError(context, DeployManagerImpl.class, "requestToInstall",
					new Object[] { "uAAP file cannot be found. Aborting..." }, null);
			return InstallationResults.FAILURE;
		}

		if (isInstalled(application.getServiceId(), application.getId())) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "requestToInstall",
					new Object[] { "Aplication already installed" }, null);
			return InstallationResults.APPLICATION_ALREADY_INSTALLED;
		}

		if (continueIfNotInstalling(application) == false) {
			LogUtils.logWarn(context, DeployManagerImpl.class, "requestToInstall",
					new Object[] { "The deploy coordinataor is already installing the uApp with idenitfied by "
							+ application.getServiceId() + ":" + application.getId() },
					null);
			return InstallationResults.FAILURE;
		}

		// 4 - send event to the Space
		spaceEventHandler.mpaInstalling(spaceManager.getSpaceDescriptor());

		// adding an entry to the registry
		// this.registry.put(card.getId(), new UAPPStatus(card));
		uapp.getApp();
		for (PeerCard peer : layout.keySet()) {
			List<Part> parts = layout.get(peer);
			for (Part part : parts) {
				UAPPCard card = new UAPPCard(application.getServiceId(), part.getPartId(), uapp.getApp());
				byte[] content = createZippedPart(application.getFolder(), part);
				InstallationResults partState = requestToInstallPart(card, peer, content);
				result.setDetailedResult(peer, part, partState);
				if (partState == InstallationResults.MISSING_PEER) {
					return InstallationResults.INVALID_DEPLOY_LAYOUT;
				} else if (partState != InstallationResults.SUCCESS) {
					return partState;
				}
			}
		}

		// 4 - send event to the Space
		spaceEventHandler.mpaInstalled(spaceManager.getSpaceDescriptor());

		storeInstallationStatus(application);

		return InstallationResults.SUCCESS;
	}

	private InstallationResults requestToInstallPart(UAPPCard card, PeerCard peer, byte[] content) {
		if (isPeerPartOfSpace(peer.getPeerID()) == false) {
			return InstallationResults.MISSING_PEER;
		}
		// send the part to the target node
		LogUtils.logInfo(context, DeployManagerImpl.class, "requestToInstall",
				new Object[] { "Sending request to install uAPP part to: " + peer.getPeerID() }, null);

		// TODO On failure of installation of a part we could retry up-to
		// n-times
		// TODO On permanent failure we should roll back the installation by
		// removing the parts from peers
		InstallationResults result = synchronousInstallPart(controlBroker, content, peer, card, TIMEOUT);
		return result;
	}

	private Properties getApplicationRegistry() {
		synchronized (applicationRegistry) {
			File reg = new File(deployDir, Consts.APP_REGISTRY);
			if (reg.exists() == false) {
				applicationRegistry.clear();
				applicationRegistryFile = null;
				return applicationRegistry;
			}
			if (reg.exists() && applicationRegistryFile == null) {
				reloadRegistry(reg);
			}
			if (reg.exists() && applicationRegistryFile != null
					&& reg.lastModified() > applicationRegistryFile.lastModified()) {
				reloadRegistry(reg);
			}
			return applicationRegistry;
		}
	}

	private void reloadRegistry(File reg) {
		synchronized (applicationRegistry) {
			if (reg.exists() == false) {
				applicationRegistry.clear();
				applicationRegistryFile = null;
				return;
			}
			try {
				applicationRegistry.load(new FileInputStream(reg));
				applicationRegistryFile = reg;
			} catch (Exception ex) {
				applicationRegistry.clear();
				LogUtils.logError(context, DeployManagerImpl.class, "getApplicationRegistry", new Object[] {
						"Failed to load application registry due to: " + ExceptionUtils.stackTraceAsString(ex) }, ex);
			}
		}
	}

	private void updateApplicationRegistry() throws IOException {
		if (applicationRegistry == null)
			return;

		OutputStream os = new FileOutputStream(new File(deployDir, Consts.APP_REGISTRY));
		applicationRegistry.store(os,
				"universAAL Deploy Manager Installation registry, the format is serviceId:applicationId=<application layout registry file>");
		os.flush();
		os.close();

	}

	private void storeInstallationStatus(UAPPPackage app) {
		try {
			Properties apps = getApplicationRegistry();
			String appKey = app.getServiceId() + ":" + app.getId();
			String partRegistry = appKey.hashCode() + "_" + System.currentTimeMillis() + ".registry";
			apps.setProperty(appKey, partRegistry);
			updateApplicationRegistry();

			Properties parts = new Properties();
			Map<PeerCard, List<Part>> layout = app.getDeploy();
			for (PeerCard peer : layout.keySet()) {
				List<Part> partList = layout.get(peer);
				int i = 0;
				for (Part part : partList) {
					final String partId = part.getPartId();
					i++;
					parts.setProperty(peer.getPeerID() + "/" + i, partId);
				}
			}

			OutputStream os = new FileOutputStream(new File(deployDir, partRegistry));
			parts.store(os, "universAAL Deploy layout details for application '" + appKey
					+ "', the format is peerId/<index>=partId");
			os.flush();
			os.close();
		} catch (Exception ex) {
			LogUtils.logError(context, DeployManagerImpl.class, "storeInstallationStatus",
					new Object[] {
							"Failed to update application registry due to: " + ExceptionUtils.stackTraceAsString(ex) },
					ex);
		}
	}

	private boolean isInstalled(String serviceId, String id) {
		final String appKey = serviceId + ":" + id;
		return getApplicationRegistry().containsKey(appKey);
	}

	private void deleteInstallationLayout(String serviceId, String id) {
		final String appKey = serviceId + ":" + id;
		String layoutFile = getApplicationRegistry().getProperty(appKey);
		File registry = new File(deployDir, layoutFile);
		if (registry.delete() == false) {
			LogUtils.logWarn(context, DeployManagerImpl.class, "deleteInstallationLayout",
					"Unable to delete layout database that is stored in " + registry.getAbsolutePath() + "\n"
							+ "We are trying to postpone the deletion when JVM will be closing");
			registry.deleteOnExit();
		}
	}

	private Properties getInstallationLayout(String serviceId, String id) {
		try {
			final String appKey = serviceId + ":" + id;
			String layoutFile = getApplicationRegistry().getProperty(appKey);

			InputStream is = new FileInputStream(new File(deployDir, layoutFile));
			Properties layout = new Properties();
			layout.load(is);
			return layout;
		} catch (Exception ex) {
			LogUtils.logError(context, DeployManagerImpl.class, "getInstallationLayout", new Object[] {
					"Failed to load application installation layout due to: " + ExceptionUtils.stackTraceAsString(ex) },
					ex);
			return null;
		}

	}

	private boolean isPeerPartOfSpace(String peerId) {
		if (spaceManager.getMyPeerCard().getPeerID().equals(peerId)) {
			return true;
		}

		if (spaceManager.getPeers().get(peerId) != null) {
			return true;
		}
		return false;
	}

	public InstallationResultsDetails requestToUninstall(String serviceId, String id) {
		final String METHOD = "requestToUninstall";
		InstallationResultsDetails result = new InstallationResultsDetails(InstallationResults.FAILURE);
		if (isInstalled(serviceId, id) == false) {
			LogUtils.logDebug(context, DeployManagerImpl.class, METHOD,
					new Object[] { "No aplication installed with uSrvId " + serviceId + " and uAppId " + id }, null);
			result.setGlobalResult(InstallationResults.APPLICATION_NOT_INSTALLED);
			return result;
		}
		Properties layout = getInstallationLayout(serviceId, id);
		Enumeration<?> installedParts = layout.propertyNames();
		while (installedParts.hasMoreElements()) {
			String peerLine = (String) installedParts.nextElement();
			String[] parts = peerLine.split("/");
			String peer = parts[0];

			if (isPeerPartOfSpace(peer) == false) {
				LogUtils.logDebug(context, DeployManagerImpl.class, METHOD,
						new Object[] { "Not all the peers used during the installation phase are available" }, null);
				result.setGlobalResult(InstallationResults.MISSING_PEER);
				return result;
			}
		}
		result.setGlobalResult(InstallationResults.SUCCESS);
		installedParts = layout.propertyNames();
		while (installedParts.hasMoreElements()) {
			String peerLine = (String) installedParts.nextElement();
			String[] parts = peerLine.split("/");
			String peer = parts[0];
			UAPPCard card = new UAPPCard(serviceId, id, layout.getProperty(peerLine), "", "");
			PeerCard target = spaceManager.getPeers().get(peer);
			if (target == null && peer.equals(spaceManager.getMyPeerCard().getPeerID())) {
				target = spaceManager.getMyPeerCard();
			}
			InstallationResults status = synchronousUninstallPart(controlBroker, target, card, TIMEOUT);
			result.setDetailedResult(peer, card.getPartId(), status);
			if (status != InstallationResults.SUCCESS) {
				result.setGlobalResult(InstallationResults.FAILURE);
			}
		}
		if (result.getGlobalResult() == InstallationResults.SUCCESS) {
			deleteInstallationLayout(serviceId, id);
			getApplicationRegistry().remove(serviceId + ":" + id);
			try {
				updateApplicationRegistry();
			} catch (IOException ex) {
				LogUtils.logError(context, DeployManagerImpl.class, "requestToUninstall",
						new Object[] { "Update the application service registry due to ",
								ExceptionUtils.stackTraceAsString(ex), "\n",
								"It is a serius issue that could result in a corrupted installation database" },
						ex);
				result.setGlobalResult(InstallationResults.FAILURE);
			}
		}
		return result;
	}

	private InstallationResults synchronousUninstallPart(ControlBroker broker, PeerCard target, UAPPCard card,
			long timeout) {
		final long fireTimeout;
		synchronized (uninstallingParts) {
			fireTimeout = System.currentTimeMillis() + timeout;
			uninstallingParts.put(card.toString(), fireTimeout);
		}
		try {
			broker.requestToUninstallPart(target, card);
			return waitForOperation(uninstallingParts, card.toString(), timeout);
		} catch (Exception ex) {
			LogUtils.logDebug(context,
					DeployManagerImpl.class, "synchronousUninstallPart", new Object[] {
							"Failed to request part UNinstallation due to", ExceptionUtils.stackTraceAsString(ex) },
					null);
			return InstallationResults.UNKNOWN;
		}
	}

	private InstallationResults waitForOperation(HashMap<String, Long> monitor, String item, long timeout) {
		synchronized (monitor) {
			while (true) {
				final Long currentTimeout = monitor.get(item);
				/*
				 * Received the installationPartNotification callback with a
				 * SUCCESS status so we can stop to wait
				 */
				if (currentTimeout == null)
					return InstallationResults.SUCCESS;

				/*
				 * Received the installationPartNotification callback with a
				 * FAILURE code so timeout has been reset to -1 which means that
				 * the installation of the part has been failed
				 */
				if (currentTimeout < 0) {
					UAPPPartStatus status = convertTimeout2UAPPPartStatus(currentTimeout.intValue());

					if (status == null) {
						return InstallationResults.FAILURE;
					} else if (status == UAPPPartStatus.PART_MISSING_NEEDED_FILES) {
						return InstallationResults.MPA_FILE_NOT_VALID;
					} else {
						return InstallationResults.FAILURE;
					}
				}
				/*
				 * The standard timeout fired
				 */
				if (System.currentTimeMillis() > currentTimeout)
					return InstallationResults.OPERATION_TIMEOUT;
			}

		}
	}

	private InstallationResults synchronousInstallPart(ControlBroker broker, byte[] content, PeerCard peer,
			UAPPCard card, long timeout) {

		final long fireTimeout;
		synchronized (installingParts) {
			fireTimeout = System.currentTimeMillis() + timeout;
			installingParts.put(card.toString(), fireTimeout);
		}
		try {
			broker.requestToInstallPart(content, peer, card);
			return waitForOperation(installingParts, card.toString(), timeout);
		} catch (Exception ex) {
			LogUtils.logDebug(context,
					DeployManagerImpl.class, "synchronousInstallPart", new Object[] {
							"Failed to request part installation due to", ExceptionUtils.stackTraceAsString(ex) },
					null);
			return InstallationResults.UNKNOWN;
		}
	}

	private UAPPPartStatus convertTimeout2UAPPPartStatus(int v) {
		if (v >= 0) {
			throw new IllegalArgumentException("Exepected only negative values");
		}
		v = Math.abs(v);
		UAPPPartStatus[] values = UAPPPartStatus.values();
		if (v >= values.length)
			return null;
		return values[v];
	}

	private boolean continueIfNotInstalling(UAPPPackage app) {
		synchronized (wip) {
			if (wip.containsKey(app.getServiceId() + ":" + app.getId())) {
				return false;
			}
			wip.put(app.getServiceId() + ":" + app.getId(), app);
		}
		return true;
	}

	public void installationPartNotification(UAPPCard card, String partID, PeerCard peer, UAPPPartStatus status) {
		LogUtils.logDebug(context, DeployManagerImpl.class, "installationPartNotification",
				new Object[] {
						"Updating notification status of the part: " + partID + " from peer " + peer.getPeerID() },
				null);
		synchronized (installingParts) {
			synchronized (uninstallingParts) {
				final String key = card.toString();
				if (installingParts.containsKey(key) == false && uninstallingParts.containsKey(key) == false) {
					LogUtils.logWarn(context, DeployManagerImpl.class, "installationPartNotification",
							new Object[] { "Received notification" + status + " for " + partID + " from peer "
									+ peer.getPeerID() + " which was not valid or that has already been TIMEOUT" },
							null);
					return;
				}
				switch (status) {
				case PART_INSTALLED:
					installingParts.remove(key);
					break;
				// TODO following aggregation has been done on purpose
				case PART_MISSING_NEEDED_FILES:
				case PART_NOT_INSTALLED:
					installingParts.put(key, (long) (status.ordinal() * -1));
					break;
				// TODO we should handle the above cases PART_NOT_UNINSTALLED,
				// PART_UNINSTALLED
				case PART_NOT_UNINSTALLED:
					installingParts.put(key, (long) (status.ordinal() * -1));
					break;
				case PART_UNINSTALLED:
					uninstallingParts.remove(key);
					break;
				case PART_PENDING:
					break;
				}
			}
		}
	}

	private void addDirToZipFile(ZipOutputStream out, File dir, String name) {
		final String METHOD = "addDirsToZipFile";
		byte[] data = new byte[1000];
		if (!dir.exists()) {
			return;
		}
		File[] partFiles = dir.listFiles();
		if (name != null && name.trim().isEmpty() == false) {
			name += File.separator;
		} else {
			name = "";
		}
		if (partFiles == null || partFiles.length == 0 && name.endsWith(File.separator)) {
			try {
				out.putNextEntry(new ZipEntry(name));
				out.closeEntry();
			} catch (IOException e) {
				LogUtils.logError(context, DeployManagerImpl.class, METHOD,
						new Object[] { "Error adding empty folder to Zip archive for folder ", name, "\n",
								"The exception was:\n", ExceptionUtils.stackTraceAsString(e) },
						e);
			}
			return;
		}
		BufferedInputStream inPartFile;
		for (File fileName : partFiles) {
			if (fileName.isDirectory()) {
				addDirToZipFile(out, fileName, name + fileName.getName());
				return;
			}
			try {
				inPartFile = new BufferedInputStream(new FileInputStream(fileName), 1000);
				out.putNextEntry(new ZipEntry(name + fileName.getName()));
				int count;
				while ((count = inPartFile.read(data, 0, 1000)) != -1) {
					out.write(data, 0, count);
				}
				out.closeEntry();
				LogUtils.logInfo(context, DeployManagerImpl.class, METHOD,
						new Object[] { "Added", fileName.getAbsolutePath(), " to Zip archive with SUCCES" }, null);
			} catch (FileNotFoundException e) {
				LogUtils.logWarn(context, DeployManagerImpl.class, METHOD,
						new Object[] { "Unable to add ", fileName.getAbsolutePath(),
								" to Zip archive due to exception, we will skip the file." + "\n",
								"The exception was:\n", ExceptionUtils.stackTraceAsString(e) },
						e);
			} catch (IOException ex) {
				LogUtils.logError(context, DeployManagerImpl.class, METHOD,
						new Object[] { "Writing entry to Zip archive file failed for entry, while adding ",
								fileName.getAbsolutePath(), "\n", "The exception was:\n",
								ExceptionUtils.stackTraceAsString(ex) },
						ex);
			}
		}
	}

	/**
	 * Creates a zip file containing the artifacts to send
	 *
	 * @param applicationFolder
	 * @param part
	 * @return
	 */
	private byte[] createZippedPart(URI applicationFolder, Part part) {
		final String METHOD = "createZippedPart";
		ZipOutputStream out = null;
		File zippedPart = null;
		byte[] buf = new byte[1024];
		try {
			// create the zip file in a tmp dir
			zippedPart = File.createTempFile("shipping-", "-part.zip", new File(deployDir));
			out = new ZipOutputStream(new FileOutputStream(zippedPart));
			// pit the part descriptor in the zip
			String name = part.getBundleId() + "_" + part.getBundleVersion() + "_" + part.getPartId();
			name = name.replaceAll("[ :!@#%&\\/]", "_").replaceAll("[^-_a-zA-Z0-9.]", "");
			File partDescription = File.createTempFile(name, "-uApp.xml", new File(deployDir));
			marshaller.marshal(part, partDescription);
			FileInputStream in = new FileInputStream(partDescription);
			out.putNextEntry(new ZipEntry(partDescription.getName()));
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			out.closeEntry();
			in.close();
		} catch (FileNotFoundException e1) {
			LogUtils.logError(context, DeployManagerImpl.class, METHOD,
					new Object[] { "Error while creating the zip file part. Aborting: " + e1 }, null);
			return null;
		} catch (JAXBException e) {
			LogUtils.logError(context, DeployManagerImpl.class, METHOD,
					new Object[] { "Error while creating the zip file part: " + e }, null);
			return null;
		} catch (IOException e) {
			LogUtils.logError(context, DeployManagerImpl.class, METHOD,
					new Object[] { "Error while creating the zip file part: " + e }, null);
			return null;
		}

		// zip the part folder
		// get the part id
		String partID = part.getPartId();
		String partFolderString = applicationFolder.getPath() + File.separatorChar + Consts.APPLICATION_BINARYPART_PATH
				+ File.separatorChar + partID + File.separatorChar;
		File partFolder = new File(partFolderString);
		File configFolder = new File(partFolder.getParentFile().getParentFile(), Consts.APPLICATION_CONFIGURATION_PATH);
		if (configFolder.exists() && configFolder.isDirectory()) {
			addDirToZipFile(out, configFolder.listFiles(new FileFilter() {

				public boolean accept(File f) {
					return f.isDirectory();
				}
			})[0], Consts.APPLICATION_CONFIGURATION_PATH);
		}
		addDirToZipFile(out, partFolder, null);

		try {
			out.flush();
			out.close();
		} catch (IOException e) {
			LogUtils.logError(context, DeployManagerImpl.class, METHOD,
					new Object[] { "Error while closing the ZIP file", ExceptionUtils.stackTraceAsString(e) }, e);
			return null;
		}
		try {
			FileInputStream inZip = new FileInputStream(zippedPart);
			byte[] fileContent = new byte[(int) zippedPart.length()];
			inZip.read(fileContent);
			LogUtils.logDebug(context, DeployManagerImpl.class, METHOD,
					new Object[] { "ZipFile created ready to sent it to the target node" }, null);
			return fileContent;
		} catch (FileNotFoundException e) {
			LogUtils.logError(context, DeployManagerImpl.class, METHOD,
					new Object[] { "Error while creating the zip file part: " + e }, null);
			return null;
		} catch (IOException e) {
			LogUtils.logError(context, DeployManagerImpl.class, METHOD,
					new Object[] { "Error while creating the zip file part: " + e }, null);
			return null;
		}

	}

	public void sharedObjectAdded(Object sharedObj, Object arg1) {
		if (sharedObj instanceof ControlBroker) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "ControlBroker service added" }, null);
			this.controlBroker = (ControlBroker) sharedObj;
		} else if (sharedObj instanceof SpaceManager) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "SpaceManager service added" }, null);
			spaceManager = (SpaceManager) sharedObj;
			spaceManager.addSpaceListener(this);
			if (spaceManager.getSpaceDescriptor() != null && spaceManager.getSpaceDescriptor()
					.getDeployManager().getPeerID().equals(spaceManager.getMyPeerCard().getPeerID())) {
				isDeployCoordinator = true;
			}

		} else if (sharedObj instanceof SpaceEventHandler) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "SpaceEventHandler service added" }, null);
			spaceEventHandler = (SpaceEventHandler) sharedObj;
			spaceManager.addSpaceListener(this);

		}

	}

	public void sharedObjectRemoved(Object sharedObj) {
		if (sharedObj instanceof ControlBroker) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "ControlBroker service removed" }, null);
			controlBroker = null;
			initialized = false;
		} else if (sharedObj instanceof SpaceManager) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "SpaceManager service removed" }, null);
			spaceManager = null;
			initialized = false;
		} else if (sharedObj instanceof SpaceEventHandler) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "SpaceEventHandler service removed" }, null);
			spaceEventHandler = null;
			initialized = false;
		}

	}

	/**
	 * Method for checking if the MPA can be installed on the Space where the
	 * MW resides.
	 *
	 * @return boolean answer
	 *
	 *         private boolean SpaceCheck(AalMpa mpa, SpaceDescriptor
	 *         spaceDescriptor){ if(mpa.getApplicationProfile().getSpace() !=
	 *         null){ Space targetSpace =
	 *         mpa.getApplicationProfile().getSpace();
	 *         if(targetSpace.getTargetProfile()== null){
	 *         LogUtils.logWarn(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {"MPA
	 *         target profile is null but it is requirred...aborting", null);
	 *         return false; //check if target profiles matches with the profile
	 *         of my Space }else if(!targetSpace.getTargetProfile(
	 *         ).getProfileId().value().equals
	 *         (spaceDescriptor.getSpaceCard().getProfile())){
	 *         LogUtils.logDebug(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] { "MPA
	 *          Space profile does not match with the current
	 *         Space...trying with the alternative MPA profiles" , null);
	 *         for(ProfileType alternativeProfile: mpa.getApplicationProfile
	 *         ().getSpace().getAlternativeProfiles().getProfile()){
	 *         if(alternativeProfile
	 *         .getProfileId().value().equals(spaceDescriptor
	 *         .getSpaceCard().getProfile())){ LogUtils.logInfo(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {"MPA
	 *         alternative profile: "
	 *         +alternativeProfile.getProfileId().value()+ " matches with the
	 *         Space profile: " +spaceDescriptor.getSpaceCard().getProfile(),
	 *         null); return true; } } LogUtils.logWarn(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {"MPA:
	 *         "+mpa.getApp().getName()+ " cannot be installed on the current
	 *         Space", null); return false; } }else{
	 *         LogUtils.logWarn(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {"MPA
	 *         "+mpa.getApp().getName()+ " target Space is null...aborting",
	 *         null); return false;
	 *
	 *         } return true;
	 *
	 *         }
	 */

	/**
	 * this method performs the checks related to the consistency of the MPA
	 *
	 * @param mpa
	 * @return
	 *
	 * 		private boolean mpaChecks(AalMpa mpa){ try{
	 *         if(mpa.getApp().getAppId() == null ||
	 *         mpa.getApp().getAppId().isEmpty()){ LogUtils.logWarn(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {"MPA
	 *         application ID is null...aborting", null); return false; } else
	 *         if(mpa.getApplicationPart().getPart().isEmpty()){
	 *         LogUtils.logWarn(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {"MPA
	 *         parts are empty...aborting", null); return false;
	 *
	 *         } //TODO: Add check for the runtime support }catch
	 *         (NullPointerException e) { LogUtils.logError(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {"Error
	 *         during application checks", null); return false; } return true; }
	 */

	/**
	 * Method to find the set of target peers according to the multipart
	 * applicatio
	 *
	 * @param mpa
	 *            the MPA
	 * @return map of PeerCard of the target peers
	 *
	 *         private Map<PeerCard, Part> makeMPALayout(AalMpa mpa){
	 *         Map<PeerCard, Part> mpaLayout = new HashMap<PeerCard, Part>();
	 *         Map<String, PeerCard> peers = new HashMap<String, PeerCard>();
	 *         peers.putAll(SpaceManager.getPeers()); for(Part part :
	 *         mpa.getApplicationPart().getPart()){ //check: deployment units
	 *         for(String key: peers.keySet()){ PeerCard peer =
	 *         SpaceManager.getPeers().get(key);
	 *         if(checkDeployementUnit(part.getDeploymentUnit(), peer)){
	 *         mpaLayout.put(peer, part); peers.remove(key); break; } } } return
	 *         mpaLayout; }
	 *
	 *         private boolean checkDeployementUnit(List<DeploymentUnit>
	 *         depoyementUnits, PeerCard peer){ for(DeploymentUnit
	 *         deployementUnit: depoyementUnits){ //check the existence of:
	 *         osUnit if(deployementUnit.getOsUnit()!= null){
	 *         if(deployementUnit.getOsUnit().value() == null ||
	 *         deployementUnit.getOsUnit().value().isEmpty()){
	 *         LogUtils.logWarn(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {
	 *         "OSunit is present but not consistent. OSUnit is null or empty",
	 *         null); return false; } }else if
	 *         (deployementUnit.getPlatformUnit() != null){
	 *         if(deployementUnit.getPlatformUnit().value() == null ||
	 *         deployementUnit.getPlatformUnit().value().isEmpty()){
	 *         LogUtils.logWarn(context,
	 *         DeployManagerImpl.class,"DeployManagerImpl", new Object[] {
	 *         "PlatformUnit is present but not consistent. Plaform is null or
	 *         empty" , null); return false;
	 *
	 *         } } } return true; }
	 */

	public void spaceJoined(SpaceDescriptor spaceDescriptor) {
		LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
				new Object[] { "Configure the ControlBroker for the reception of DeployMessage" }, null);
		// if I'm also the deploy manager, set this property
		if (spaceDescriptor.getDeployManager().getPeerID().equals(spaceManager.getMyPeerCard().getPeerID())) {
			isDeployCoordinator = true;
		}

	}

	public void spaceLost(SpaceDescriptor spaceDescriptor) {
		// TODO Auto-generated method stub

	}

	public void dispose() {
		// remove me as listener
		context.getContainer().removeSharedObjectListener(this);
		// TODO Auto-generated method stub

	}

	public void loadConfigurations(Dictionary configurations) {
		LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
				new Object[] { "Updating DeployManager properties" }, null);
		if (configurations == null) {
			LogUtils.logDebug(context, DeployManagerImpl.class, "DeployManagerImpl",
					new Object[] { "Properties are null. Aborting..." }, null);

		} else {
			// Set up all the configuration if any...
		}

	}

	public void peerJoined(PeerCard peer) {
		// TODO Auto-generated method stub
	}

	public void peerLost(PeerCard peer) {
		// TODO Auto-generated method stub
	}

	public boolean isDeployCoordinator() {
		return isDeployCoordinator;
	}

	public void spaceStatusChanged(SpaceStatus status) {
		// TODO Auto-generated method stub
	}
}

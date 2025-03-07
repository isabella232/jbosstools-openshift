/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.tools.openshift.internal.crc.server.core.adapter;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ServerDelegate;
import org.jboss.ide.eclipse.as.core.util.ServerNamingUtility;
import org.jboss.tools.openshift.common.core.utils.StringUtils;
import org.jboss.tools.openshift.internal.cdk.server.core.BinaryUtility;
import org.jboss.tools.openshift.internal.cdk.server.core.detection.MinishiftVersionLoader;
import org.jboss.tools.openshift.internal.cdk.server.core.detection.MinishiftVersionLoader.MinishiftVersions;

public class CRC100Server extends ServerDelegate {
	public static final String CRC_100_SERVER_TYPE_ID = "org.jboss.tools.openshift.crc.server.type.crc.v100";
	private static final String CRC_10_BASE_NAME = "CodeReady Containers 1.0+";
	public static final String PROPERTY_PULL_SECRET_FILE = "crc.pullsecret.file";
	public static final String PROPERTY_BINARY_FILE = "crc.binary.file";

	
	@Override
	public void setDefaults(IProgressMonitor monitor) {
		getServerWorkingCopy().setHost("localhost"); //$NON-NLS-1$
		setDefaultServerName(monitor);
	}

	/**
	 * Initializes this server with a default server name. 
	 * This method is called when a new server is created so that the server 
	 * can be initialized with a name suitable to the server type. 
	 * 
	 * This method currently overrides a *nonexistant* upstream method, which
	 * is only proposed in upstream bug. 
	 * 
	 * @param monitor a progress monitor, or <code>null</code> if progress
	 *    reporting and cancellation are not desired
	 */
	public void setDefaultServerName(IProgressMonitor monitor) {
		getServerWorkingCopy().setName(ServerNamingUtility.getDefaultServerName(getBaseName()));
	}

	public static String getServerTypeBaseName() {
		return CRC_10_BASE_NAME;
	}
	
	protected String getBaseName() {
		return CRC100Server.getServerTypeBaseName();
	}
	
	/**
	 * Subclasses may override if the structure of the minishift home folder changes in future versions
	 * @return
	 */
	public boolean isInitialized() {
		String home = getCRCHome(getServer());
		if (StringUtils.isEmpty(home)) {
			return false;
		}
		File homeF = new File(home);
		if( !homeF.exists() || !homeF.isDirectory())
			return false;
		
		// Verification of crc being initialized
		// this is very unreliable check and should be improved in future
		// https://issues.redhat.com/browse/JBIDE-27921
		File bin = new File(homeF, "bin");
		File json = new File(homeF, "crc.json");
		File log = new File(homeF, "crc.log");
		if (!json.exists() || !log.exists()) {
			return false;
		}
		
		// OC only present for pre-1.24.0 crc
		if( matchesCRC_1_24_OrGreater(getServer()))
			return true;
		
		if (!bin.exists()) {
			return false;
		}
		File oc = new File(bin, "oc");
		File ocExe = new File(bin, "oc.exe");
		if( !oc.exists() && !ocExe.exists()) 
			return false;
		return true;
	}

	public static boolean matchesCRC_1_24_OrGreater(IServer server) {
		return matchesCRC_1_x_OrGreater(server, 24);
	}

	public static boolean matchesCRC_1_21_OrGreater(IServer server) {
		return matchesCRC_1_x_OrGreater(server, 21);
	}

	public static boolean matchesCRC_1_x_OrGreater(IServer server, int minor) {
		String binLoc = getCRCBinaryLocation(server);
		MinishiftVersions minishiftVersionProps = MinishiftVersionLoader.getVersionProperties(binLoc);
		String crcVersion = minishiftVersionProps.getCRCVersion();
		if( matchesCRC_1_x_OrGreater(crcVersion, minor))
			return true;
		return false;
	}
	public static boolean matchesCRC_1_x_OrGreater(String version, int minor) {
		String prefix = version;
		if (version.contains("+")) {
			prefix = version.substring(0, version.indexOf("+"));
		}
		
		String[] segments = prefix.split("\\.");
		if ("1".equals(segments[0]) && Integer.parseInt(segments[1]) >= minor) {
			return true;
		}
		return false;
	}

	public String getCRCHome(IServer server) {
		return getDefaultCRCHome();
	}

	public String getDefaultCRCHome() {
		return new File(System.getProperty("user.home"), ".crc").getAbsolutePath();
	}

	public static String getCRCBinaryLocation(IServer server) {
		return BinaryUtility.CRC_BINARY.getLocation(server, true, false, false);
	}

	@Override
	public IStatus canModifyModules(IModule[] arg0, IModule[] arg1) {
		return Status.CANCEL_STATUS;
	}

	@Override
	public IModule[] getChildModules(IModule[] arg0) {
		return new IModule[0];
	}

	@Override
	public IModule[] getRootModules(IModule arg0) throws CoreException {
		return new IModule[0];
	}

	@Override
	public void modifyModules(IModule[] arg0, IModule[] arg1, IProgressMonitor arg2) throws CoreException {
	}
}

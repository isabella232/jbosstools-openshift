/*******************************************************************************
 * Copyright (c) 2007-2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v 1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributor:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.reddeer.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.reddeer.common.exception.RedDeerException;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.swt.condition.ShellIsAvailable;
import org.eclipse.reddeer.swt.impl.button.CheckBox;
import org.eclipse.reddeer.swt.impl.button.PushButton;
import org.eclipse.reddeer.swt.impl.button.RadioButton;
import org.eclipse.reddeer.swt.impl.button.YesButton;
import org.eclipse.reddeer.swt.impl.shell.DefaultShell;
import org.eclipse.reddeer.workbench.ui.dialogs.WorkbenchPreferenceDialog;
import org.jboss.tools.openshift.reddeer.preference.page.OpenShift3PreferencePage;
import org.jboss.tools.openshift.reddeer.requirement.OpenShiftCommandLineToolsRequirement;

public class TestUtils {

	public static void setUpOcBinary() {
		setOCBinaryPath(true);
	}
	
	public static void cleanUpOCBinary() {
		setOCBinaryPath(false);
	}
	
	private static void setOCBinaryPath(boolean setUp) {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		OpenShift3PreferencePage page = new OpenShift3PreferencePage(dialog);
		dialog.open();
		dialog.select(page);
		if (setUp) {
			page.setOCLocation(OpenShiftCommandLineToolsRequirement.getDefaultOCLocation());
		} else {
			page.clearOCLocation();
		}
		page.apply();
		dialog.ok();
	}
	
	public static void setVisualEditorToUseHTML5() {
		WorkbenchPreferenceDialog dialog = new WorkbenchPreferenceDialog();
		dialog.open();
		
		dialog.select("JBoss Tools", "Web", "Editors", "Visual Page Editor");
		
		RadioButton button = new RadioButton("HTML5 (use WebKit)");
		if (button.isEnabled() && !button.isSelected()) {
			button.click();
		}
		
		CheckBox checkBox = new CheckBox("Do not show Browser Engine dialog");
		if (checkBox.isEnabled() && !checkBox.isChecked()) {
			checkBox.click();
		}
		
		new PushButton("Apply").click();
		dialog.ok();
	}
	
	public static void cleanupGitFolder(File gitDir, String repoName) {
		boolean exists = gitDir.exists() ? true : gitDir.mkdir();

		if (exists && gitDir.isDirectory() && gitDir.listFiles().length > 0) {
			for (File file : gitDir.listFiles()) {
				if (file.getName().contains(repoName)) {
					if (file.isDirectory()) {
						closeGitRepository(file);
					}
					try {
						TestUtils.delete(file);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public static void cleanupGitFolder(File gitDir) {
		boolean exists = gitDir.exists() ? true : gitDir.mkdir();

		if (exists && gitDir.isDirectory() && gitDir.listFiles().length > 0) {
			for (File file : gitDir.listFiles()) {
				if (file.isDirectory()) {
					closeGitRepository(file);
				}
				try {
					TestUtils.delete(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public static String getOS() {
		return System.getProperty("os.name").toLowerCase();
	}
	
	public static void cleanupGitFolder(String appname) {
		File gitDir = new File(System.getProperty("user.home") + File.separatorChar + "git");
		cleanupGitFolder(gitDir, appname);
	}

	public static void closeGitRepository(File repoDir) {
		try {
			Git git = Git.open(repoDir);
			git.getRepository().close();
			git.close();
		} catch (IOException ex) {
			// DO NOTHING
		}
	}
	
	public static void delete(File file) throws IOException {
		if (file.isDirectory() && file.list().length > 0) {
			String files[] = file.list();
			for (String tmpFile : files) {
				File fileToDelete = new File(file, tmpFile);
				delete(fileToDelete);
			}
		}
		
		file.delete();
	}
	
	public static String getValueOrDefault(String value, String defaultValue) {
		if (StringUtils.isBlank(value)) {
			return defaultValue;
		}
		return value;
	}
	
	/**
	 * Provide resource absolute path in project directory
	 * @param path - resource relative path
	 * @return resource absolute path
	 */
	public static String getProjectAbsolutePath(String... path) {

		// Construct path
		StringBuilder builder = new StringBuilder();
		for (String fragment : path) {
			builder.append("/" + fragment);
		}

		String filePath = System.getProperty("user.dir");
		File file = new File(filePath + builder.toString());
		if (!file.exists()) {
			throw new RedDeerException("Resource file does not exists within project path "
					+ filePath + builder.toString());
		}

		return file.getAbsolutePath();
	}
	
	public static Properties readPropertiesFile(String filePath) {
		Properties properties = new Properties();
		File file = new File(filePath);
		try (FileInputStream fis = new FileInputStream(file)) {
			properties.load(fis);
		} catch (IOException exc) {
			exc.printStackTrace();
		}
		return properties;
	}

	/**
	 * Finds out whether a URL returns HTTP OK or not.
	 * 
	 * @param URL URL to find out whether is accessible 
	 * @return true if URL is accesible with HTTP OK exit code (200), false otherwise
	 */
	public static boolean isURLAccessible(String URL) {
		try {
            HttpURLConnection.setFollowRedirects(false);
            HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
            connection.setRequestMethod("HEAD");
            return (connection.getResponseCode() == HttpURLConnection.HTTP_OK);
        } catch (Exception e) {
            return false;
        }
	}
	
	public static void acceptSSLCertificate() {
		acceptShellDialog(OpenShiftLabel.Shell.UNTRUSTED_SSL_CERTIFICATE);
		acceptShellDialog(OpenShiftLabel.Shell.UNTRUSTED_SSL_CERTIFICATE);
	}
	
	public static void acceptShellDialog(String shell) {
		ShellIsAvailable sslDialogIsAvailable = new ShellIsAvailable(shell);
		new WaitUntil(sslDialogIsAvailable, TimePeriod.MEDIUM, false);
		if (sslDialogIsAvailable.getResult() != null) {
			new DefaultShell(shell);
			new YesButton().click();
			new WaitWhile(sslDialogIsAvailable, TimePeriod.MEDIUM, false);
		}
	}
}

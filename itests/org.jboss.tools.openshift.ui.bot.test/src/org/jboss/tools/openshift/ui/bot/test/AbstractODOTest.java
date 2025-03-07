/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.ui.bot.test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.reddeer.common.wait.TimePeriod;
import org.eclipse.reddeer.common.wait.WaitUntil;
import org.eclipse.reddeer.common.wait.WaitWhile;
import org.eclipse.reddeer.core.exception.CoreLayerException;
import org.eclipse.reddeer.eclipse.ui.navigator.resources.ProjectExplorer;
import org.eclipse.reddeer.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.eclipse.reddeer.jface.condition.WindowIsAvailable;
import org.eclipse.reddeer.jface.wizard.WizardDialog;
import org.eclipse.reddeer.requirements.openperspective.OpenPerspectiveRequirement.OpenPerspective;
import org.eclipse.reddeer.swt.condition.ControlIsEnabled;
import org.eclipse.reddeer.swt.impl.button.FinishButton;
import org.eclipse.reddeer.swt.impl.text.LabeledText;
import org.eclipse.reddeer.workbench.core.condition.JobIsRunning;
import org.eclipse.reddeer.workbench.handler.WorkbenchShellHandler;
import org.jboss.tools.common.launcher.reddeer.wizards.NewLauncherProjectWizard;
import org.jboss.tools.common.launcher.reddeer.wizards.NewLauncherProjectWizardPage;
import org.jboss.tools.common.reddeer.perspectives.JBossPerspective;
import org.jboss.tools.openshift.core.odo.URL;
import org.jboss.tools.openshift.internal.ui.odo.OdoCliFactory;
import org.jboss.tools.openshift.reddeer.utils.OpenShiftLabel;
import org.jboss.tools.openshift.reddeer.view.OpenShiftApplicationExplorerView;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOApplication;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOComponent;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODODevfile;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODOProject;
import org.jboss.tools.openshift.reddeer.view.resources.OpenShiftODODevfileRegistry;
import org.jboss.tools.openshift.reddeer.widget.terminal.TerminalHasNoChange;
import org.jboss.tools.openshift.reddeer.wizard.CreateComponentWizard;
import org.jboss.tools.openshift.reddeer.wizard.page.CreateComponentWizadPage;
import org.junit.After;

/**
 * Abstract class for ODO Tests
 * 
 * @author jkopriva@redhat.com
 */
@OpenPerspective(JBossPerspective.class)
public abstract class AbstractODOTest {
	
	protected static String eclipseProject = "myservice";
	
	@After
	public void cleanUp() {
		//delete all projects - test could fail and interfere next test
		ProjectExplorer pe = new ProjectExplorer();
		pe.open();
		pe.deleteAllProjects();
		//Cleanup notifications
		WorkbenchShellHandler.getInstance().closeAllNonWorbenchShells();
	}
	
	public static void triggerDebugSession(String eclipseProjectName, String project, String application, String component, String urlSuffix) throws IOException, InterruptedException, ExecutionException {
		String path = ResourcesPlugin.getWorkspace().getRoot().getProject(eclipseProjectName).getLocation().toOSString();
		List<URL> urls = OdoCliFactory.getInstance().getOdo().get().listURLs(project, application, path, component);
		java.net.URL url = new java.net.URL("http://" + urls.get(0).getHost() + urlSuffix);
		new Thread(new Runnable() {
			public void run() {
				try {
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.connect();
					connection.getResponseCode();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
	
  protected static void importEmptyProject(String projectName) {
    BasicNewProjectResourceWizard wizard = new BasicNewProjectResourceWizard();
    wizard.open();
    new LabeledText("Project name:").setText(projectName);
    wizard.finish();
  }

  protected static void importEmptyProject() {
    importEmptyProject(eclipseProject);
	}

	protected static void importVertxLauncherProject() {
		importLauncherProject(eclipseProject, "vert.x community");
	}
	
	public static void importLauncherProject(String projectName, String stack) {
		NewLauncherProjectWizard wizard = new NewLauncherProjectWizard();
		wizard.openWizardFromShellMenu();

		NewLauncherProjectWizardPage wizardPage = new NewLauncherProjectWizardPage(wizard);
		if (!wizardPage.getTargetMissions().isEmpty() && !wizardPage.getTargetRuntimes().isEmpty()) {
			wizardPage.setTargetMission("rest-http");
			wizardPage.setTargetRuntime(stack);
			wizardPage.setProjectName(projectName);
			wizardPage.toggleUseDefaultLocationCheckBox(true);
		}

		new WaitUntil(new ControlIsEnabled(new FinishButton()), TimePeriod.VERY_LONG, false);
		wizard.finish(TimePeriod.getCustom(2500));

	}
	
	protected static void createComponent(String projectName, String componentType, String starter, boolean devfile) {
		createComponent(eclipseProject, projectName, componentType, starter, devfile);
	}
	
  public static void createComponent(String eclipseProjectName, String projectName, String componentType, String starter, boolean devfile) {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		OpenShiftODOProject project = explorer.getOpenShiftODOConnection().getProject(projectName);
		project.openCreateComponentWizard();
		CreateComponentWizard componentWizard = new CreateComponentWizard();
		CreateComponentWizadPage componentWizardPage = new CreateComponentWizadPage(componentWizard);
		componentWizardPage.setComponentName(eclipseProjectName);
		componentWizardPage.setEclipseProject(eclipseProjectName);
		if (componentType != null) {
			componentWizardPage.selectComponentType(componentType, devfile);
		}
		if (starter != null) {
		  componentWizardPage.selectStarter(starter);
		}
		if (!devfile) {
			try {
				componentWizardPage.selectComponentVersion("latest");
			} catch (CoreLayerException exc) {
				// nothing
			}
		}
		componentWizardPage.setApplication("myapp");
		componentWizard.finish(TimePeriod.getCustom(600L)); //Maven builds may take more than 5mn
			
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		new WaitWhile(new TerminalHasNoChange(), TimePeriod.VERY_LONG);
	}
  
  protected static void createComponentFromRegistry(String registryName, String componentType, String starter) {
    createComponentFromRegistry(eclipseProject, registryName, componentType, starter);
  }

  public static void createComponentFromRegistry(String eclipseProjectName, String registryName, String componentType, String starter) {
    OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
    explorer.open();
    OpenShiftODODevfileRegistry registry = explorer.getOpenShiftODORegistries().getRegistry(registryName);
    OpenShiftODODevfile devfile = registry.getDevfile(componentType);
    devfile.openCreateComponentWizard();
    CreateComponentWizard componentWizard = new CreateComponentWizard();
    CreateComponentWizadPage componentWizardPage = new CreateComponentWizadPage(componentWizard);
    componentWizardPage.setComponentName(eclipseProjectName);
    componentWizardPage.setEclipseProject(eclipseProjectName);
    if (starter != null) {
      componentWizardPage.selectStarter(starter);
    }
    componentWizardPage.setApplication("myapp");
    componentWizard.finish(TimePeriod.getCustom(600L)); //Maven builds may take more than 5mn
      
    new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
    new WaitWhile(new TerminalHasNoChange(), TimePeriod.VERY_LONG);
  }


	public static void createURL(String projectName, String applicationName, String componentName, String urlName, int port) {
		OpenShiftApplicationExplorerView explorer = new OpenShiftApplicationExplorerView();
		explorer.open();
		OpenShiftODOProject project = explorer.getOpenShiftODOConnection().getProject(projectName);
		OpenShiftODOApplication application = project.getApplication(applicationName);
		OpenShiftODOComponent component = application.getComponent(componentName);
		component.openCreateURLWizard();
		WizardDialog dialog = new WizardDialog(OpenShiftLabel.Shell.CREATE_URL);
		new LabeledText(OpenShiftLabel.TextLabels.NAME).setText(urlName);
			new LabeledText(OpenShiftLabel.TextLabels.PORT).setText(String.valueOf(port));
		new FinishButton(dialog).click();
		 
		new WaitWhile(new WindowIsAvailable(dialog), TimePeriod.VERY_LONG);
		new WaitWhile(new JobIsRunning(), TimePeriod.VERY_LONG);
		component.select();
		component.push();
	}
}

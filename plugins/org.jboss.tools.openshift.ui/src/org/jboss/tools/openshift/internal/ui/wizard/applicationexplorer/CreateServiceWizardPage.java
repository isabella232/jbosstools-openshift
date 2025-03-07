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
package org.jboss.tools.openshift.internal.ui.wizard.applicationexplorer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.typed.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.core.odo.OperatorCRD;
import org.jboss.tools.openshift.core.odo.ServiceTemplate;
import org.jboss.tools.openshift.internal.common.ui.databinding.IsNotNullValidator;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;
import org.jboss.tools.openshift.internal.ui.OpenShiftUIActivator;
import org.jboss.tools.openshift.internal.ui.widgets.JsonSchemaWidget;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Red Hat Developers
 *
 */
public class CreateServiceWizardPage extends AbstractOpenShiftWizardPage {

	private static final String PROPERTIES = "properties";
	private static final String SPEC = "spec";
	
	private CreateServiceModel model;
	private JsonSchemaWidget schemaWidget;
	
	private static final ObjectMapper MAPPER = new ObjectMapper();

	protected CreateServiceWizardPage(IWizard wizard, CreateServiceModel model) {
		super("Create service", "Specify a name for your service and choose a template to start from.", "Create service", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		
		Label serviceNameLabel = new Label(parent, SWT.NONE);
		serviceNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(serviceNameLabel);
		Text serviceNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(serviceNameText);

		ISWTObservableValue<String> serviceNameObservable = WidgetProperties.text(SWT.Modify).observe(serviceNameText);
		Binding serviceNameBinding = ValueBindingBuilder.bind(serviceNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a name"))
				.to(BeanProperties.value(CreateServiceModel.PROPERTY_SERVICE_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(serviceNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		Label serviceTemplatesLabel = new Label(parent, SWT.NONE);
		serviceTemplatesLabel.setText("Service:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(serviceTemplatesLabel);
		Combo serviceTemplatesCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(serviceTemplatesCombo);
		ComboViewer serviceTemplatesComboViewer = new ComboViewer(serviceTemplatesCombo);
		serviceTemplatesComboViewer.setContentProvider(ArrayContentProvider.getInstance());
		serviceTemplatesComboViewer.setLabelProvider(new ServiceTemplateColumLabelProvider());
		serviceTemplatesComboViewer.setInput(model.getServiceTemplates());
		Binding serviceTemplatesBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(serviceTemplatesComboViewer))
				.validatingAfterGet(new IsNotNullValidator(
						ValidationStatus.cancel("You have to select a template.")))
				.to(BeanProperties.value(CreateServiceModel.PROPERTY_SELECTED_SERVICE_TEMPLATE, ServiceTemplate.class)
						.observe(model))
				.in(dbc);
		ControlDecorationSupport.create(serviceTemplatesBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		
		Label serviceCRDsLabel = new Label(parent, SWT.NONE);
		serviceCRDsLabel.setText("Type:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(serviceCRDsLabel);
		Combo serviceCRDsCombo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
				.applyTo(serviceCRDsCombo);
		ComboViewer serviceCRDsComboViewer = new ComboViewer(serviceCRDsCombo);
		serviceCRDsComboViewer.setContentProvider(new ObservableListContentProvider<>());
		serviceCRDsComboViewer.setLabelProvider(new ServiceTemplateCRDColumLabelProvider());
		serviceCRDsComboViewer.setInput(BeanProperties.list(CreateServiceModel.PROPERTY_SELECTED_SERVICE_TEMPLATE_CRDS).observe(model));
		Binding serviceCRDsBinding = ValueBindingBuilder
				.bind(ViewerProperties.singleSelection().observe(serviceCRDsComboViewer))
				.validatingAfterGet(new IsNotNullValidator(
						ValidationStatus.cancel("You have to select a type.")))
				.to(BeanProperties.value(CreateServiceModel.PROPERTY_SELECTED_SERVICE_TEMPLATE_CRD, OperatorCRD.class)
						.observe(model))
				.in(dbc);
		ControlDecorationSupport.create(serviceCRDsBinding, SWT.LEFT | SWT.TOP, null,
				new RequiredControlDecorationUpdater());
		
		ScrolledComposite schemaParentComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).span(3, 1)
		.applyTo(schemaParentComposite);
		schemaParentComposite.setExpandHorizontal(true);
		schemaParentComposite.setExpandVertical(true);
		schemaWidget = new JsonSchemaWidget(schemaParentComposite, ERROR, schemaParentComposite);
		schemaParentComposite.setContent(schemaWidget);
		schemaParentComposite.setMinHeight(250);
		serviceCRDsComboViewer.addSelectionChangedListener(e -> {
			initSchemaWidget();
		});
		initSchemaWidget();
		

		Label applicationLabel = new Label(parent, SWT.NONE);
		applicationLabel.setText("Application:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(applicationLabel);
		Text applicationNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(applicationNameText);

		ISWTObservableValue<String> applicationNameObservable = WidgetProperties.text(SWT.Modify).observe(applicationNameText);
		Binding applicationNameBinding = ValueBindingBuilder.bind(applicationNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify an application"))
				.to(BeanProperties.value(CreateServiceModel.PROPERTY_APPLICATION_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(applicationNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));
		if (StringUtils.isNotBlank(model.getApplicationName())) {
			applicationNameText.setEnabled(false);
		}
}

	private void initSchemaWidget() {
		Job job = new Job("Loading schema for " + model.getSelectedServiceTemplateCRD().getKind()) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				JsonNode schema = model.getSelectedServiceTemplateCRD().getSchema();
				schemaWidget.getDisplay().asyncExec(() -> {
					if (schema.has(PROPERTIES) && schema.get(PROPERTIES).has(SPEC)) {
						schemaWidget.setEnabled(true);
						schemaWidget.init((ObjectNode) schema.get(PROPERTIES).get(SPEC),
								model.getSelectedServiceTemplateCRD().getSample() != null
										&& model.getSelectedServiceTemplateCRD().getSample().has(SPEC)
												? model.getSelectedServiceTemplateCRD().getSample().get(SPEC)
												: null);
					} else {
						schemaWidget.setEnabled(false);
					}
				});
				return Status.OK_STATUS;
			}
		};
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					job.addJobChangeListener(new JobChangeAdapter() {
						@Override
						public void done(IJobChangeEvent event) {
							monitor.done();
						}
					});
					monitor.beginTask(job.getName(), IProgressMonitor.UNKNOWN);
					job.schedule();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			OpenShiftUIActivator.log(IStatus.ERROR, e.getLocalizedMessage(), e);
		}
	}
	
	/**
	 * @return
	 */
	public boolean finish() {
		try {
			ObjectNode spec = null;
			if (schemaWidget.isEnabled()) {
				spec = MAPPER.createObjectNode();
				schemaWidget.dump(spec);
			}
			model.getOdo().createService(model.getProjectName(), model.getApplicationName(), model.getSelectedServiceTemplate(),
			    model.getSelectedServiceTemplateCRD(), model.getServiceName(), spec, false);
			return true;
		} catch (IOException e) {
			setErrorMessage(e.getLocalizedMessage());
			return false;
		}
	}
}

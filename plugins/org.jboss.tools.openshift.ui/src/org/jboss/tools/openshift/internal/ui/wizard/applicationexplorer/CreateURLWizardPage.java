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

import java.text.DecimalFormat;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.BeanProperties;
import org.eclipse.core.databinding.conversion.text.NumberToStringConverter;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.swt.ISWTObservableValue;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.tools.common.ui.databinding.MandatoryStringValidator;
import org.jboss.tools.common.ui.databinding.ValueBindingBuilder;
import org.jboss.tools.openshift.internal.common.ui.databinding.RequiredControlDecorationUpdater;
import org.jboss.tools.openshift.internal.common.ui.wizard.AbstractOpenShiftWizardPage;

/**
 * @author Red Hat Developers
 *
 */
public class CreateURLWizardPage extends AbstractOpenShiftWizardPage {
  
  private static class PortValidator implements IValidator<String> {

    @Override
    public IStatus validate(String value) {
      if (value == null || value.isBlank()) {
        return ValidationStatus.cancel("Port cannot be empty");
      }
      try {
        Integer port = Integer.parseInt(value);
        if (port < 1024 || port > 65535) {
          return ValidationStatus.cancel("Port must be in the 1024-65535 range");
        }
      } catch (NumberFormatException e) {
        return ValidationStatus.cancel("Port must be a positive integer");
      }
      return ValidationStatus.ok();
    }
    
  }

	private CreateURLModel model;

	protected CreateURLWizardPage(IWizard wizard, CreateURLModel model) {
		super("Create url", "Specify a name for the url, choose a port to bind to and select a secure (https) scheme or not.", "Create url", wizard);
		this.model = model;
	}

	@Override
	protected void doCreateControls(Composite parent, DataBindingContext dbc) {
		GridLayoutFactory.fillDefaults().numColumns(3).margins(10, 10).applyTo(parent);
		
		Label urlNameLabel = new Label(parent, SWT.NONE);
		urlNameLabel.setText("Name:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(urlNameLabel);
		Text urlNameText = new Text(parent, SWT.BORDER);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(urlNameText);

		ISWTObservableValue<String> urlNameObservable = WidgetProperties.text(SWT.Modify).observe(urlNameText);
		Binding urlNameBinding = ValueBindingBuilder.bind(urlNameObservable)
				.validatingAfterGet(new MandatoryStringValidator("Please specify a name"))
				.to(BeanProperties.value(CreateURLModel.PROPERTY_URL_NAME).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(urlNameBinding, SWT.LEFT | SWT.TOP, null, new RequiredControlDecorationUpdater(true));

		Label portLabel = new Label(parent, SWT.NONE);
		portLabel.setText("Port:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(portLabel);
		
		  Text portText = new Text(parent, SWT.SINGLE);
      GridDataFactory.fillDefaults().span(2, 1).align(SWT.FILL, SWT.CENTER).grab(true, false)
      .applyTo(portText);
      Binding portBinding = ValueBindingBuilder
          .bind(WidgetProperties.text(SWT.Modify).observe(portText))
          .validatingAfterGet(new PortValidator())
          .to(BeanProperties.value(CreateURLModel.PROPERTY_PORT)
              .observe(model))
          .converting(NumberToStringConverter.fromInteger(new DecimalFormat("#"), true))
          .in(dbc);
      ControlDecorationSupport.create(portBinding, SWT.LEFT | SWT.TOP, null,
          new RequiredControlDecorationUpdater());
		
		Label secureLabel = new Label(parent, SWT.NONE);
		secureLabel.setText("Secure:");
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(secureLabel);
		Button secureButton = new Button(parent, SWT.CHECK);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).grab(true, false).span(2, 1)
				.applyTo(secureButton);

		ISWTObservableValue<Boolean> secureObservable = WidgetProperties.buttonSelection().observe(secureButton);
		Binding secureBinding = ValueBindingBuilder.bind(secureObservable)
				.to(BeanProperties.value(CreateURLModel.PROPERTY_SECURE).observe(model))
				.in(dbc);
		ControlDecorationSupport.create(secureBinding, SWT.LEFT | SWT.TOP);
		}
}

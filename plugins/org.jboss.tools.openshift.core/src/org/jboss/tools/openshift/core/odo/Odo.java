/*******************************************************************************
 * Copyright (c) 2019-2021 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.openshift.core.odo;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

public interface Odo {
    List<String> getNamespaces() throws IOException;

    String getNamespace() throws IOException;

    void describeApplication(String project, String application) throws IOException;

    void deleteApplication(String project, String application) throws IOException;

    void push(String project, String application, String context, String component) throws IOException;

    void describeComponent(String project, String application, String context, String component) throws IOException;

    void watch(String project, String application, String context, String component) throws IOException;

    void createComponentLocal(String project, String application, String componentType, String componentVersion, String registryName, String component, String source, String devfile, String starter, boolean push) throws IOException;

    void createComponentGit(String project, String application, String context, String componentType, String componentVersion, String component, String source, String reference, boolean push) throws IOException;

    void createComponentBinary(String project, String application, String context, String componentType, String componentVersion, String component, String source, boolean push) throws IOException;

    void createService(String project, String application, ServiceTemplate serviceTemplate, OperatorCRD serviceCRD, String service,
      ObjectNode spec, boolean wait) throws IOException;

    String getServiceTemplate(String project, String application, String service) throws IOException;

    void deleteService(String project, String application, Service service) throws IOException;

    List<ComponentType> getComponentTypes() throws IOException;

    List<DevfileComponentType> getComponentTypes(String registryName) throws IOException;

    ComponentTypeInfo getComponentTypeInfo(String componentType, String registryName) throws IOException;

    List<ServiceTemplate> getServiceTemplates() throws IOException;

    void describeServiceTemplate(String template) throws IOException;

    List<Integer> getServicePorts(String project, String application, String component);

    List<URL> listURLs(String project, String application, String context, String component) throws IOException;

    ComponentInfo getComponentInfo(String project, String application, String component, String path, ComponentKind kind) throws IOException;

    void createURL(String project, String application, String context, String component, String name, Integer port, boolean secure) throws IOException;

    void deleteURL(String project, String application, String context, String component, String name) throws IOException;

    void undeployComponent(String project, String application, String context, String component, ComponentKind kind) throws IOException;

    void deleteComponent(String project, String application, String context, String component, ComponentKind kind) throws IOException;

    void follow(String project, String application, String context, String component) throws IOException;

    void log(String project, String application, String context, String component) throws IOException;

    void createProject(String project) throws IOException;

    void deleteProject(String project) throws IOException;

    void login(String url, String userName, char[] password, String token) throws IOException;

    void logout() throws IOException;

    List<Application> getApplications(String project) throws IOException;

    List<Component> getComponents(String project, String application) throws IOException;

    List<Service> getServices(String project, String application) throws IOException;

    List<Storage> getStorages(String project, String application, String context, String component) throws IOException;

    void listComponents() throws IOException;

    void listServices() throws IOException;

    void about() throws IOException;

    void createStorage(String project, String application, String context, String component, String name, String mountPath, String storageSize) throws IOException;

    void deleteStorage(String project, String application, String context, String component, String storage) throws IOException;

    void link(String project, String application, String component, String context, String source) throws IOException;

    String consoleURL() throws IOException;

    void debug(String project, String application, String context, String component, Integer port) throws IOException;

    DebugInfo debugInfo(String project, String application, String context, String component) throws IOException;

    List<ComponentDescriptor> discover(String path) throws IOException;

    java.net.URL getMasterUrl();

    List<DevfileRegistry> listDevfileRegistries() throws IOException;

    void createDevfileRegistry(String name, String url, boolean secure) throws IOException;

    void deleteDevfileRegistry(String name) throws IOException;
}

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
package org.jboss.tools.openshift.core.odo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jboss.tools.openshift.core.odo.Starter.Builder;
import org.apache.commons.lang.StringUtils;
import org.jboss.tools.openshift.core.OpenShiftCoreConstants.DebugStatus;

public class JSonParser {
    private static final String ITEMS_FIELD = "items";
    private static final String METADATA_FIELD = "metadata";
    private static final String NAME_FIELD = "name";
    private static final String SPEC_FIELD = "spec";
    private static final String SOURCE_TYPE_FIELD = "sourceType";
    private static final String TYPE_FIELD = "type";
    private static final String ENV_FIELD = "env";
    private static final String VALUE_FIELD = "value";
    private static final String DEVFILE_FIELD = "Devfile";
    private static final String STARTER_PROJECTS_FIELD = "starterProjects";
    private static final String DESCRIPTION_FIELD = "description";
    private static final String DEBUG_PROCESS_ID_FIELD = "debugProcessID";
    private static final String LOCAL_PORT_FIELD = "localPort";
    private static final String REGISTRY_NAME_FIELD = "RegistryName";
    
    private static final String PATHS_FIELD = "paths";
    private static final String POST_FIELD = "post";
    private static final String PARAMETERS_FIELD = "parameters";
    private static final String BODY_VALUE = "body";
    private static final String SCHEMA_FIELD = "schema";
    private static final String DOLLAR_REF_FIELD = "$ref";

    private final JsonNode root;

    public JSonParser(JsonNode root) {
        this.root = root;
    }

    public List<URL> parseURLS() {
        List<URL> result = new ArrayList<>();
        if (root.has(ITEMS_FIELD)) {
            root.get(ITEMS_FIELD).forEach(item -> {
                //odo incorrectly reports urls created with the web ui without names
                if (item.get(METADATA_FIELD).has(NAME_FIELD)) {
                    String name = item.get(METADATA_FIELD).get(NAME_FIELD).asText();
                    String protocol = item.get(SPEC_FIELD).has("protocol") ?
                            item.get(SPEC_FIELD).get("protocol").asText() : "";
                    String host = item.get(SPEC_FIELD).has("host") ?
                            item.get(SPEC_FIELD).get("host").asText() : "";
                    String port = item.get(SPEC_FIELD).has("port") ? item.get(SPEC_FIELD).get("port").asText() : "0";
                    result.add(URL.of(name, protocol, host, port, item.get("status").get("state").asText(), item.get(SPEC_FIELD).get("secure").asBoolean()));
                }
            });
        }
        return result;
    }

    public List<Application> parseApplications() {
        List<Application> result = new ArrayList<>();
        if (root.has(ITEMS_FIELD)) {
            root.get(ITEMS_FIELD).forEach(item -> result.add(Application.of(item.get(METADATA_FIELD).get(NAME_FIELD).asText())));
        }
        return result;
    }

    public ComponentInfo parseComponentInfo(ComponentKind kind) {
        ComponentInfo.Builder builder = new ComponentInfo.Builder().withComponentKind(kind);
        if (root.has(SPEC_FIELD)) {
            String componentTypeName = root.get(SPEC_FIELD).get(TYPE_FIELD).asText();
            if (root.get(SPEC_FIELD).has(SOURCE_TYPE_FIELD)) {
                String sourceType = root.get(SPEC_FIELD).get(SOURCE_TYPE_FIELD).asText();
                builder.withSourceType(ComponentSourceType.fromAnnotation(sourceType)).withComponentTypeName(componentTypeName);
            } else {
                builder.withSourceType(ComponentSourceType.LOCAL).withComponentTypeName(componentTypeName);
            }
            if (root.get(SPEC_FIELD).has(ENV_FIELD)) {
                JsonNode env = root.get(SPEC_FIELD).get(ENV_FIELD);
                for(JsonNode elt : env) {
                    if (elt.has(NAME_FIELD) && elt.has(VALUE_FIELD)) {
                        builder.addEnv(elt.get(NAME_FIELD).asText(), elt.get(VALUE_FIELD).asText());
                    }
                }
            }
        }
        return builder.build();
    }

    /**
     * @return
     */
    public ComponentTypeInfo parseComponentTypeInfo(String registryName) {
      ComponentTypeInfo.Builder builder = new ComponentTypeInfo.Builder();
      for(JsonNode element : root) {
        if (element.has(REGISTRY_NAME_FIELD) && registryName.equals(element.get(REGISTRY_NAME_FIELD).asText())) {
          if (element.has(DEVFILE_FIELD)) {
            JsonNode data = element.get(DEVFILE_FIELD);
            if (data.has(METADATA_FIELD) && data.get(METADATA_FIELD).has(NAME_FIELD)) {
              builder.withName(data.get(METADATA_FIELD).get(NAME_FIELD).asText());
            }
            if (data.has(STARTER_PROJECTS_FIELD)) {
              for(JsonNode starter : data.get(STARTER_PROJECTS_FIELD)) {
                builder.withStarter(parseStarter(starter));
              }
            }
          }
          break;
        }
      }
      return builder.build();
    }
    
    public Starter parseStarter(JsonNode node) {
      String name = node.get(NAME_FIELD).asText();
      String description = node.has(DESCRIPTION_FIELD)?node.get(DESCRIPTION_FIELD).asText():"";
      Builder builder = new Builder().withName(name).withDescription(description);
      return builder.build();
    }

    /**
     * @return
     */
    public DebugInfo parseDebugInfo() {
      DebugStatus status = DebugStatus.UNKNOWN;
      if (root.has(SPEC_FIELD) && root.get(SPEC_FIELD).has(DEBUG_PROCESS_ID_FIELD)) {
        String processID = root.get(SPEC_FIELD).get(DEBUG_PROCESS_ID_FIELD).asText();
        if (StringUtils.isNumeric(processID)) {
          status = DebugStatus.RUNNING;
        }
      }
      DebugInfo info = new DebugInfo(status);
      if (root.has(SPEC_FIELD) && root.get(SPEC_FIELD).has(LOCAL_PORT_FIELD)) {
        info.setLocalPort(root.get(SPEC_FIELD).get(LOCAL_PORT_FIELD).asInt());
      }
      return info;
    }
    
    private JsonNode resolveRefs(JsonNode root, JsonNode node) throws IOException {
      for (Iterator<String> it = node.fieldNames(); it.hasNext();) {
        String name = it.next();
        JsonNode child = node.get(name);
        if (child.has(DOLLAR_REF_FIELD)) {
          JsonNode replaced = resolve(root, child.get(DOLLAR_REF_FIELD).asText());
          ((ObjectNode) node).set(name, replaced);
        } else {
          resolveRefs(root, child);
        }
      }
      return node;
    }

  private JsonNode resolve(JsonNode root, String ref) throws IOException {
    JsonNode node = root;
    String[] ids = ref.split("/");
    for(String id : ids) {
        if (!"#".equals(id)) {
            if (node.has(id)) {
                node = node.get(id);
            } else {
                throw new IOException("Can't resolved reference '" + ref + "' element " + id + " not found");
            }
        }
    }
    return node;
  }

  public ObjectNode findSchema(String crd) throws IOException {
    if (root.has(PATHS_FIELD)) {
      JsonNode node = root.get(PATHS_FIELD).get(crd);
      if (node != null && node.has(POST_FIELD) && node.get(POST_FIELD).has(PARAMETERS_FIELD)) {
        for (JsonNode parameter : node.get(POST_FIELD).get(PARAMETERS_FIELD)) {
          if (parameter.has(NAME_FIELD) && parameter.get(NAME_FIELD).asText().equals(BODY_VALUE)
              && parameter.has(SCHEMA_FIELD)) {
            JsonNode schema = parameter.get(SCHEMA_FIELD);
            if (schema.has(DOLLAR_REF_FIELD)) {
              return (ObjectNode) resolveRefs(root, resolve(root, schema.get(DOLLAR_REF_FIELD).asText()));
            } else {
              return (ObjectNode) resolveRefs(root, schema);
            }
          }
        }
      }
    }
    throw new IOException("Invalid data, no 'paths' field");
  }
}


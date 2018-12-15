/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fabric8.kubernetes.client;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.junit.Test;

public class DeploymentsTest {

  private static final Logger logger = LoggerFactory.getLogger(DeploymentsTest.class);
  @Test
  public void testCreatDeploymentWithLocalDir() {
    Config config = new ConfigBuilder().withMasterUrl("http://127.0.0.1:8080").withNamespace("test-ns").build();
    KubernetesClient client = new DefaultKubernetesClient(config);

    try {
      // Create a namespace for all our stuff
      Namespace ns = new NamespaceBuilder().withNewMetadata().withName("test-ns").addToLabels("app", "my-ns").endMetadata().build();
      log("Created namespace", client.namespaces().createOrReplace(ns));

      ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata().withName("test-sa").endMetadata().build();

      client.serviceAccounts().inNamespace("test-ns").createOrReplace(fabric8);

      Deployment deployment = new DeploymentBuilder()
        .withNewMetadata()
        .withName("nginx")
        .endMetadata()
        .withNewSpec()
        .withReplicas(1)
        .withNewTemplate()
        .withNewMetadata()
        .addToLabels("app", "nginx")
        .endMetadata()
        .withNewSpec()
        .addNewContainer()
        .withName("nginx")
        .withImage("nginx")
        .addNewPort()
        .withContainerPort(80)
        .endPort()
        .addNewVolumeMount()
        .withName("log-localdir")
        .withMountPath("/ycj/tempfile")
        .endVolumeMount()
        .endContainer()
        .addNewVolume()
        .withName("log-localdir")
        .withLocalDir(new LocalDirVolumeSource(new Quantity("0.1","BinarySI")))
        .endVolume()
        .endSpec()
        .endTemplate()
        .withNewSelector()
        .addToMatchLabels("app", "nginx")
        .endSelector()
        .endSpec()
        .build();

      deployment = client.apps().deployments().inNamespace("test-ns").create(deployment);
      log("Created deployment", deployment);

      System.err.println("Scaling up:" + deployment.getMetadata().getName());
      client.apps().deployments().inNamespace("test-ns").withName("nginx").scale(2, true);
      log("Created replica sets:", client.apps().replicaSets().inNamespace("test-ns").list().getItems());
      System.err.println("Deleting:" + deployment.getMetadata().getName());
//      client.resource(deployment).delete();
    }finally {
//      client.namespaces().withName("test-ns").delete();
      client.close();
    }
  }

  private static void log(String action, Object obj) {
    logger.info("{}: {}", action, obj);
  }
}


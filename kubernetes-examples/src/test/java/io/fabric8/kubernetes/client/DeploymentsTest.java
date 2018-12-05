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
  public void testAdaptToHttpClient() {
    Config config = new ConfigBuilder().withMasterUrl("http://127.0.0.1:8080").withNamespace("thisisatest").build();
    KubernetesClient client = new DefaultKubernetesClient(config);

    try {
      // Create a namespace for all our stuff
      Namespace ns = new NamespaceBuilder().withNewMetadata().withName("thisisatest").addToLabels("this", "rocks").endMetadata().build();
      log("Created namespace", client.namespaces().createOrReplace(ns));

      ServiceAccount fabric8 = new ServiceAccountBuilder().withNewMetadata().withName("fabric8").endMetadata().build();

      client.serviceAccounts().inNamespace("thisisatest").createOrReplace(fabric8);
      for (int i = 0; i < 1; i++) {
        System.err.println("Iteration:" + (i+1));
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
          .endContainer()
          .endSpec()
          .endTemplate()
          .withNewSelector()
          .addToMatchLabels("app", "nginx")
          .endSelector()
          .endSpec()
          .build();


        deployment = client.apps().deployments().inNamespace("thisisatest").create(deployment);
        log("Created deployment", deployment);

        System.err.println("Scaling up:" + deployment.getMetadata().getName());
        client.apps().deployments().inNamespace("thisisatest").withName("nginx").scale(2, true);
        log("Created replica sets:", client.apps().replicaSets().inNamespace("thisisatest").list().getItems());
        System.err.println("Deleting:" + deployment.getMetadata().getName());
      //  client.resource(deployment).delete();
      }
      log("Done.");

    }finally {
//      client.namespaces().withName("thisisatest").delete();
      client.close();
    }
  }


  private static void log(String action, Object obj) {
    logger.info("{}: {}", action, obj);
  }

  private static void log(String action) {
    logger.info(action);
  }
}


/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geode.connectors.jdbc.internal.cli;

import static org.apache.geode.connectors.jdbc.internal.cli.CreateConnectionCommand.*;
import static org.apache.geode.distributed.ConfigurationProperties.ENABLE_CLUSTER_CONFIGURATION;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Properties;

import org.apache.geode.distributed.internal.InternalLocator;
import org.apache.geode.management.internal.cli.util.CommandStringBuilder;
import org.apache.geode.test.dunit.rules.LocatorServerStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.categories.DistributedTest;
import org.apache.geode.test.junit.categories.UnitTest;
import org.apache.geode.test.junit.rules.GfshCommandRule;
import org.apache.geode.test.junit.rules.serializable.SerializableTestName;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(DistributedTest.class)
public class CreateConnectionCommandDUnitTest {

  @Rule
  public transient GfshCommandRule gfsh = new GfshCommandRule();

  @Rule
  public LocatorServerStartupRule startupRule = new LocatorServerStartupRule();

  @Rule
  public SerializableTestName testName = new SerializableTestName();

  private MemberVM locator;
  private MemberVM server;

  @Before
  public void before() throws Exception {
    locator = startupRule.startLocatorVM(0);
    server = startupRule.startServerVM(1, locator.getPort());

    gfsh.connectAndVerify(locator);
  }

  public void connect(MemberVM serverVM) throws Exception {
    gfsh.connectAndVerify(serverVM.getJmxPort(), GfshCommandRule.PortType.jmxManager);
  }

  @Test
  public void createsConnection() throws Exception {
    CommandStringBuilder csb = new CommandStringBuilder(CREATE_CONNECTION);
    csb.addOption(CREATE_CONNECTION__NAME, "name");
    csb.addOption(CREATE_CONNECTION__URL, "url");
    csb.addOption(CREATE_CONNECTION__USER, "username");
    csb.addOption(CREATE_CONNECTION__PASSWORD, "secret");
    csb.addOption(CREATE_CONNECTION__PARAMS, "param1,param2");

    gfsh.executeAndAssertThat(csb.toString())
        .statusIsSuccess();
  }
}
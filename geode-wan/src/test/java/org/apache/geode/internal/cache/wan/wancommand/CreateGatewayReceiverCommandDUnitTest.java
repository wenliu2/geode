/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.cache.wan.wancommand;

import static org.apache.geode.distributed.ConfigurationProperties.BIND_ADDRESS;
import static org.apache.geode.distributed.ConfigurationProperties.DISTRIBUTED_SYSTEM_ID;
import static org.apache.geode.distributed.ConfigurationProperties.GROUPS;
import static org.apache.geode.distributed.ConfigurationProperties.REMOTE_LOCATORS;
import static org.apache.geode.distributed.ConfigurationProperties.SERVER_BIND_ADDRESS;
import static org.apache.geode.internal.cache.wan.wancommand.WANCommandUtils.getMemberIdCallable;
import static org.apache.geode.internal.cache.wan.wancommand.WANCommandUtils.verifyGatewayReceiverProfile;
import static org.apache.geode.internal.cache.wan.wancommand.WANCommandUtils.verifyGatewayReceiverServerLocations;
import static org.apache.geode.internal.cache.wan.wancommand.WANCommandUtils.verifyReceiverCreationWithAttributes;
import static org.apache.geode.management.internal.cli.i18n.CliStrings.GROUP;
import static org.apache.geode.test.dunit.Assert.assertEquals;
import static org.apache.geode.test.dunit.Assert.assertTrue;
import static org.apache.geode.test.dunit.Assert.fail;
import static org.apache.geode.test.dunit.LogWriterUtils.getLogWriter;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.cache.wan.GatewayReceiver;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.internal.net.SocketCreator;
import org.apache.geode.management.cli.Result;
import org.apache.geode.management.internal.cli.i18n.CliStrings;
import org.apache.geode.management.internal.cli.result.CommandResult;
import org.apache.geode.management.internal.cli.result.TabularResultData;
import org.apache.geode.test.dunit.rules.LocatorServerStartupRule;
import org.apache.geode.test.dunit.rules.MemberVM;
import org.apache.geode.test.junit.categories.DistributedTest;
import org.apache.geode.test.junit.rules.GfshCommandRule;

/**
 * DUnit tests for 'create gateway-receiver' command.
 */
@Category(DistributedTest.class)
public class CreateGatewayReceiverCommandDUnitTest {

  private static final long serialVersionUID = 1L;

  @Rule
  public LocatorServerStartupRule locatorServerStartupRule = new LocatorServerStartupRule();

  @Rule
  public GfshCommandRule gfsh = new GfshCommandRule();

  private MemberVM locatorSite1;
  private MemberVM locatorSite2;
  private MemberVM server1;
  private MemberVM server2;
  private MemberVM server3;

  @Before
  public void before() throws Exception {
    Properties props = new Properties();
    props.setProperty(DISTRIBUTED_SYSTEM_ID, "" + 1);
    locatorSite1 = locatorServerStartupRule.startLocatorVM(1, props);

    props.setProperty(DISTRIBUTED_SYSTEM_ID, "" + 2);
    props.setProperty(REMOTE_LOCATORS, "localhost[" + locatorSite1.getPort() + "]");
    locatorSite2 = locatorServerStartupRule.startLocatorVM(2, props);

    // Connect Gfsh to locator.
    gfsh.connectAndVerify(locatorSite1);
  }

  /**
   * GatewayReceiver with all default attributes
   */
  @Test
  public void testCreateGatewayReceiverWithDefault() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    String command = CliStrings.CREATE_GATEWAYRECEIVER;
    executeCommandAndVerifyStatus(command, 3);

    // if neither bind-address or hostname-for-senders is set, profile
    // uses AcceptorImpl.getExternalAddress() to derive canonical hostname
    // when the Profile (and ServerLocation) are created
    String hostname = getHostName();

    server1.invoke(() -> verifyGatewayReceiverProfile(hostname));
    server2.invoke(() -> verifyGatewayReceiverProfile(hostname));
    server3.invoke(() -> verifyGatewayReceiverProfile(hostname));

    server1.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostname));
    server2.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostname));
    server3.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostname));

    server1.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  private String getHostName() throws Exception {
    return SocketCreator.getLocalHost().getCanonicalHostName();
  }

  private String getBindAddress() throws Exception {
    return InetAddress.getLocalHost().getHostAddress();
  }

  private void executeCommandAndVerifyStatus(String command, int numGatewayReceivers) {
    CommandResult cmdResult = gfsh.executeCommand(command);
    if (cmdResult != null) {
      String strCmdResult = cmdResult.toString();
      getLogWriter().info("testCreateGatewayReceiver stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());

      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Status");
      assertEquals(numGatewayReceivers, status.size());
      // verify there is no error in the status
      for (String stat : status) {
        assertTrue("GatewayReceiver creation failed with: " + stat, !stat.contains("ERROR:"));
      }
    } else {
      fail("testCreateGatewayReceiver failed as did not get CommandResult");
    }
  }

  /**
   * GatewayReceiver with given attributes
   */
  @Test
  public void testCreateGatewayReceiver() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=true" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000";
    executeCommandAndVerifyStatus(command, 3);

    // cannot verify Profile/ServerLocation when manualStart is true

    server1.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with hostnameForSenders
   */
  @Test
  public void testCreateGatewayReceiverWithHostnameForSenders() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    String hostnameForSenders = getHostName();
    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=false" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__HOSTNAMEFORSENDERS + "="
            + hostnameForSenders + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000";
    executeCommandAndVerifyStatus(command, 3);

    // verify hostname-for-senders is used when configured
    server1.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));
    server2.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));
    server3.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));

    server1.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));
    server2.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));
    server3.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));

    server1.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
  }

  /**
   * GatewayReceiver with all default attributes and bind-address in gemfire-properties
   */
  @Test
  public void testCreateGatewayReceiverWithDefaultAndBindProperty() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    String expectedBindAddress = getBindAddress();
    String receiverGroup = "receiverGroup";

    Properties props = new Properties();
    props.setProperty(BIND_ADDRESS, expectedBindAddress);
    props.setProperty(GROUPS, receiverGroup);

    server1 = locatorServerStartupRule.startServerVM(3, props, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, props, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, props, locator1Port);

    String command = CliStrings.CREATE_GATEWAYRECEIVER + " --" + GROUP + "=" + receiverGroup;
    executeCommandAndVerifyStatus(command, 3);

    // verify bind-address used when provided as a gemfire property
    server1.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));
    server2.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));
    server3.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));

    server1.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));
    server2.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));
    server3.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));

    server1.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with all default attributes and server-bind-address in the gemfire properties
   */
  @Test
  public void testCreateGatewayReceiverWithDefaultsAndServerBindAddressProperty() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    String expectedBindAddress = getBindAddress();
    String receiverGroup = "receiverGroup";

    Properties props = new Properties();
    props.setProperty(SERVER_BIND_ADDRESS, expectedBindAddress);
    props.setProperty(GROUPS, receiverGroup);

    server1 = locatorServerStartupRule.startServerVM(3, props, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, props, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, props, locator1Port);

    String command = CliStrings.CREATE_GATEWAYRECEIVER + " --" + GROUP + "=" + receiverGroup;
    executeCommandAndVerifyStatus(command, 3);

    // verify server-bind-address used if provided as a gemfire property
    server1.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));
    server2.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));
    server3.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));

    server1.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));
    server2.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));
    server3.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));

    server1.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with all default attributes and server-bind-address in the gemfire properties
   */
  @Test
  public void testCreateGatewayReceiverWithDefaultsAndMultipleBindAddressProperties()
      throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    String extraBindAddress = "localhost";
    String expectedBindAddress = getBindAddress();
    String receiverGroup = "receiverGroup";

    Properties props = new Properties();
    props.setProperty(BIND_ADDRESS, extraBindAddress);
    props.setProperty(SERVER_BIND_ADDRESS, expectedBindAddress);
    props.setProperty(GROUPS, receiverGroup);

    server1 = locatorServerStartupRule.startServerVM(3, props, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, props, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, props, locator1Port);

    String command = CliStrings.CREATE_GATEWAYRECEIVER + " --" + GROUP + "=" + receiverGroup;
    executeCommandAndVerifyStatus(command, 3);

    // verify server-bind-address used if provided as a gemfire property
    server1.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));
    server2.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));
    server3.invoke(() -> verifyGatewayReceiverProfile(expectedBindAddress));

    server1.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));
    server2.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));
    server3.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, expectedBindAddress));

    server1.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        GatewayReceiver.DEFAULT_START_PORT, GatewayReceiver.DEFAULT_END_PORT,
        GatewayReceiver.DEFAULT_BIND_ADDRESS, GatewayReceiver.DEFAULT_MAXIMUM_TIME_BETWEEN_PINGS,
        GatewayReceiver.DEFAULT_SOCKET_BUFFER_SIZE, null,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }


  /**
   * GatewayReceiver with hostnameForSenders
   */
  @Test
  public void testCreateGatewayReceiverWithHostnameForSendersAndServerBindAddressProperty()
      throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    Properties props = new Properties();
    String serverBindAddress = getBindAddress();
    String receiverGroup = "receiverGroup";
    props.setProperty(SERVER_BIND_ADDRESS, serverBindAddress);
    props.setProperty(GROUPS, receiverGroup);

    server1 = locatorServerStartupRule.startServerVM(3, props, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, props, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, props, locator1Port);

    String hostnameForSenders = getHostName();
    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=false" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__HOSTNAMEFORSENDERS + "="
            + hostnameForSenders + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --" + GROUP + "="
            + receiverGroup;
    executeCommandAndVerifyStatus(command, 3);

    // verify server-bind-address takes precedence over hostname-for-senders
    server1.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));
    server2.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));
    server3.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));

    server1.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));
    server2.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));
    server3.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));

    server1.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
  }

  /**
   * GatewayReceiver with hostnameForSenders
   */
  @Test
  public void testCreateGatewayReceiverWithHostnameForSendersAndBindAddressProperty()
      throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    Properties props = new Properties();
    String expectedBindAddress = getBindAddress();
    String receiverGroup = "receiverGroup";
    props.setProperty(BIND_ADDRESS, expectedBindAddress);
    props.setProperty(GROUPS, receiverGroup);

    server1 = locatorServerStartupRule.startServerVM(3, props, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, props, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, props, locator1Port);

    String hostnameForSenders = getHostName();
    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=false" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__HOSTNAMEFORSENDERS + "="
            + hostnameForSenders + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --" + GROUP + "="
            + receiverGroup;
    executeCommandAndVerifyStatus(command, 3);

    server1.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));
    server2.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));
    server3.invoke(() -> verifyGatewayReceiverProfile(hostnameForSenders));

    server1.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));
    server2.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));
    server3.invoke(() -> verifyGatewayReceiverServerLocations(locator1Port, hostnameForSenders));

    server1.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "", 100000,
        512000, null, hostnameForSenders));
  }

  /**
   * GatewayReceiver with given attributes and a single GatewayTransportFilter.
   */
  @Test
  public void testCreateGatewayReceiverWithGatewayTransportFilter() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=false" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__GATEWAYTRANSPORTFILTER
            + "=org.apache.geode.cache30.MyGatewayTransportFilter1";
    executeCommandAndVerifyStatus(command, 3);
    List<String> transportFilters = new ArrayList<String>();
    transportFilters.add("org.apache.geode.cache30.MyGatewayTransportFilter1");

    server1.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "localhost",
        100000, 512000, transportFilters, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "localhost",
        100000, 512000, transportFilters, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(true, 10000, 11000, "localhost",
        100000, 512000, transportFilters, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with given attributes and multiple GatewayTransportFilters.
   */
  @Test
  public void testCreateGatewayReceiverWithMultipleGatewayTransportFilters() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    String command = CliStrings.CREATE_GATEWAYRECEIVER + " --"
        + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost" + " --"
        + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
        + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
        + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
        + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --"
        + CliStrings.CREATE_GATEWAYRECEIVER__GATEWAYTRANSPORTFILTER
        + "=org.apache.geode.cache30.MyGatewayTransportFilter1,org.apache.geode.cache30.MyGatewayTransportFilter2";
    executeCommandAndVerifyStatus(command, 3);
    List<String> transportFilters = new ArrayList<String>();
    transportFilters.add("org.apache.geode.cache30.MyGatewayTransportFilter1");
    transportFilters.add("org.apache.geode.cache30.MyGatewayTransportFilter2");

    server1.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        10000, 11000, "localhost", 100000, 512000, transportFilters,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        10000, 11000, "localhost", 100000, 512000, transportFilters,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(!GatewayReceiver.DEFAULT_MANUAL_START,
        10000, 11000, "localhost", 100000, 512000, transportFilters,
        GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with given attributes. Error scenario where startPort is greater than endPort.
   */
  @Test
  public void testCreateGatewayReceiver_Error() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS
            + "=localhost" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000";
    CommandResult cmdResult = gfsh.executeCommand(command);
    if (cmdResult != null) {
      String strCmdResult = cmdResult.toString();
      getLogWriter().info("testCreateGatewayReceiver stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());

      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Status");
      assertEquals(3, status.size());

      // verify there is no error in the status
      for (String stat : status) {
        assertTrue("GatewayReceiver creation should have failed", stat.contains("ERROR:"));
      }
    } else {
      fail("testCreateGatewayReceiver failed as did not get CommandResult");
    }
  }

  /**
   * GatewayReceiver with given attributes on the given member.
   */
  @Test
  public void testCreateGatewayReceiver_onMember() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    final DistributedMember server1Member =
        (DistributedMember) server1.invoke(getMemberIdCallable());

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=true" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --"
            + CliStrings.MEMBER + "=" + server1Member.getId();
    CommandResult cmdResult = gfsh.executeCommand(command);
    if (cmdResult != null) {
      String strCmdResult = cmdResult.toString();
      getLogWriter().info("testCreateGatewayReceiver stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());

      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Status");
      assertEquals(1, status.size());
      // verify there is no error in the status
      for (String stat : status) {
        assertTrue("GatewayReceiver creation failed with: " + stat, !stat.contains("ERROR:"));
      }
    } else {
      fail("testCreateGatewayReceiver failed as did not get CommandResult");
    }

    // cannot verify Profile/ServerLocation when manualStart is true

    server1.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with given attributes on multiple members.
   */
  @Test
  public void testCreateGatewayReceiver_onMultipleMembers() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = locatorServerStartupRule.startServerVM(3, locator1Port);
    server2 = locatorServerStartupRule.startServerVM(4, locator1Port);
    server3 = locatorServerStartupRule.startServerVM(5, locator1Port);

    final DistributedMember server1Member =
        (DistributedMember) server1.invoke(getMemberIdCallable());
    final DistributedMember server2Member =
        (DistributedMember) server2.invoke(getMemberIdCallable());

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=true" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --"
            + CliStrings.MEMBER + "=" + server1Member.getId() + "," + server2Member.getId();
    CommandResult cmdResult = gfsh.executeCommand(command);
    if (cmdResult != null) {
      String strCmdResult = cmdResult.toString();
      getLogWriter().info("testCreateGatewayReceiver stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());

      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Status");
      assertEquals(2, status.size());
      // verify there is no error in the status
      for (String stat : status) {
        assertTrue("GatewayReceiver creation failed with: " + stat, !stat.contains("ERROR:"));
      }
    } else {
      fail("testCreateGatewayReceiver failed as did not get CommandResult");
    }

    // cannot verify Profile/ServerLocation when manualStart is true

    server1.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with given attributes on the given group.
   */
  @Test
  public void testCreateGatewayReceiver_onGroup() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    String groups = "receiverGroup1";
    server1 = startServerWithGroups(3, groups, locator1Port);
    server2 = startServerWithGroups(4, groups, locator1Port);
    server3 = startServerWithGroups(5, groups, locator1Port);

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=true" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --" + GROUP
            + "=receiverGroup1";
    CommandResult cmdResult = gfsh.executeCommand(command);
    if (cmdResult != null) {
      String strCmdResult = cmdResult.toString();
      getLogWriter().info("testCreateGatewayReceiver stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());

      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Status");
      assertEquals(3, status.size());//
      // verify there is no error in the status
      for (String stat : status) {
        assertTrue("GatewayReceiver creation failed with: " + stat, !stat.contains("ERROR:"));
      }
    } else {
      fail("testCreateGatewayReceiver failed as did not get CommandResult");
    }

    // cannot verify Profile/ServerLocation when manualStart is true

    server1.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with given attributes on the given group. Only 2 of 3 members are part of the
   * group.
   */
  @Test
  public void testCreateGatewayReceiver_onGroup_Scenario2() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    String group1 = "receiverGroup1";
    String group2 = "receiverGroup2";
    server1 = startServerWithGroups(3, group1, locator1Port);
    server2 = startServerWithGroups(4, group1, locator1Port);
    server3 = startServerWithGroups(5, group2, locator1Port);

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=true" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --" + GROUP
            + "=receiverGroup1";
    CommandResult cmdResult = gfsh.executeCommand(command);
    if (cmdResult != null) {
      String strCmdResult = cmdResult.toString();
      getLogWriter().info("testCreateGatewayReceiver stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());

      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Status");
      assertEquals(2, status.size());//
      // verify there is no error in the status
      for (String stat : status) {
        assertTrue("GatewayReceiver creation failed with: " + stat, !stat.contains("ERROR:"));
      }
    } else {
      fail("testCreateGatewayReceiver failed as did not get CommandResult");
    }

    // cannot verify Profile/ServerLocation when manualStart is true

    server1.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  /**
   * GatewayReceiver with given attributes on multiple groups.
   */
  @Test
  public void testCreateGatewayReceiver_onMultipleGroups() throws Exception {

    Integer locator1Port = locatorSite1.getPort();

    // setup servers in Site #1
    server1 = startServerWithGroups(3, "receiverGroup1", locator1Port);
    server2 = startServerWithGroups(4, "receiverGroup1", locator1Port);
    server3 = startServerWithGroups(5, "receiverGroup2", locator1Port);

    String command =
        CliStrings.CREATE_GATEWAYRECEIVER + " --" + CliStrings.CREATE_GATEWAYRECEIVER__MANUALSTART
            + "=true" + " --" + CliStrings.CREATE_GATEWAYRECEIVER__BINDADDRESS + "=localhost"
            + " --" + CliStrings.CREATE_GATEWAYRECEIVER__STARTPORT + "=10000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__ENDPORT + "=11000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__MAXTIMEBETWEENPINGS + "=100000" + " --"
            + CliStrings.CREATE_GATEWAYRECEIVER__SOCKETBUFFERSIZE + "=512000" + " --" + GROUP
            + "=receiverGroup1,receiverGroup2";
    CommandResult cmdResult = gfsh.executeCommand(command);
    if (cmdResult != null) {
      String strCmdResult = cmdResult.toString();
      getLogWriter().info("testCreateGatewayReceiver stringResult : " + strCmdResult + ">>>>");
      assertEquals(Result.Status.OK, cmdResult.getStatus());

      TabularResultData resultData = (TabularResultData) cmdResult.getResultData();
      List<String> status = resultData.retrieveAllValues("Status");
      assertEquals(3, status.size());//
      // verify there is no error in the status
      for (String stat : status) {
        assertTrue("GatewayReceiver creation failed with: " + stat, !stat.contains("ERROR:"));
      }
    } else {
      fail("testCreateGatewayReceiver failed as did not get CommandResult");
    }

    // cannot verify Profile/ServerLocation when manualStart is true

    server1.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server2.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
    server3.invoke(() -> verifyReceiverCreationWithAttributes(false, 10000, 11000, "localhost",
        100000, 512000, null, GatewayReceiver.DEFAULT_HOSTNAME_FOR_SENDERS));
  }

  private MemberVM startServerWithGroups(int index, String groups, int locPort) throws Exception {
    Properties props = new Properties();
    props.setProperty(GROUPS, groups);
    return locatorServerStartupRule.startServerVM(index, props, locPort);
  }
}

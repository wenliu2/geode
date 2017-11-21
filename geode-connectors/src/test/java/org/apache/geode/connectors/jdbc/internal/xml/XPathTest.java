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
package org.apache.geode.connectors.jdbc.internal.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import com.sun.org.apache.xpath.internal.NodeSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.internal.cache.InternalCache;
import org.apache.geode.internal.cache.xmlcache.CacheXmlGenerator;
import org.apache.geode.internal.cache.xmlcache.CacheXmlParser;
import org.apache.geode.management.internal.configuration.utils.XmlUtils;

public class XPathTest {

  private InternalCache cache;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

//  @Before
//  public void setup() {
//    cache = (InternalCache) new CacheFactory().create();
//  }
//
//  @After
//  public void tearDown() {
//    cache.close();
//  }

  @Test
  public void test() throws Exception {
    StringBuilder xmlBuilder = new StringBuilder();
//    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xmlBuilder.append("<cache xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://geode.apache.org/schema/cache\" xsi:schemaLocation=\"http://geode.apache.org/schema/cache http://geode.apache.org/schema/cache/cache-1.0.xsd\" version=\"1.0\">");
    xmlBuilder.append("<jdbc:connector-service xmlns:jdbc=\"http://geode.apache.org/schema/jdbc\">");

//    xmlBuilder.append("<cache xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://geode.apache.org/schema/cache\" xmlns:jdbc=\"http://geode.apache.org/schema/jdbc\" xsi:schemaLocation=\"http://geode.apache.org/schema/cache http://geode.apache.org/schema/cache/cache-1.0.xsd\" version=\"1.0\">");
//    xmlBuilder.append("<jdbc:connector-service>");

    xmlBuilder.append("<jdbc:connection name=\"name\" url=\"url\" user=\"username\" password=\"secret\"/>");
    xmlBuilder.append("</jdbc:connector-service>");
    xmlBuilder.append("</cache>");

    System.out.println("xmlBuilder: " + xmlBuilder.toString());

    File cacheXml = new File(temporaryFolder.getRoot(), "cache.xml");
    writeXml(cacheXml, xmlBuilder.toString());

    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setNamespaceAware(true);

    DocumentBuilder builder = factory.newDocumentBuilder();
    builder.setEntityResolver(new CacheXmlParser());

    Document document = builder.parse(cacheXml);
    System.out.println("document: " + document);
    System.out.println("document.getDocumentElement().getTagName(): " + document.getDocumentElement().getTagName());
    //System.out.println("document.getDocumentElement().getTagName(): " + document.getDocumentElement().getChildNodes());
    NodeList nodeList = document.getDocumentElement().getChildNodes();
    print(nodeList);

//    XmlUtils.XPathContext xpathContext = new XmlUtils.XPathContext();
//    xpathContext.addNamespace(CacheXml.PREFIX, CacheXml.GEODE_NAMESPACE);
//    xpathContext.addNamespace(PREFIX, NAMESPACE); // TODO: wrap this line with conditional

    // Create an XPathContext here
    String expression = "cache";
    XPath xpath = XPathFactory.newInstance().newXPath();
//    xpath.setNamespaceContext(xpathContext);
    Object result = xpath.evaluate(expression, document, XPathConstants.NODE);

    //Node element = XmlUtils.querySingleElement(document, "//cache/jdbc:connector-service", xpathContext);
    // Must copy to preserve namespaces.
    if (result == null) {
      System.out.println("RESULT = " + result);
    } else {
      System.out.println("RESULT = " + XmlUtils.elementToString((Element) result));
    }
  }

  @Test
  public void test2() throws Exception {
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//    xmlBuilder.append("<catalog xmlns:journal=\"http://www.w3.org/2001/XMLSchema-Instance\" >");
//    xmlBuilder.append("<journal:journal title=\"XML\"  publisher=\"IBM developerWorks\">");
    xmlBuilder.append("<catalog>");
    xmlBuilder.append("<journal:journal xmlns:journal=\"http://www.w3.org/2001/XMLSchema-Instance\" title=\"XML\"  publisher=\"IBM developerWorks\">");
    xmlBuilder.append("<article journal:level=\"Intermediate\" date=\"February-2003\">");
    xmlBuilder.append("<title>Design XML Schemas Using UML</title>");
    xmlBuilder.append("<author>Ayesha Malik</author>");
    xmlBuilder.append("</article>");
    xmlBuilder.append("</journal:journal>");
    xmlBuilder.append("<journal title=\"Java Technology\"  publisher=\"IBM developerWorks\">");
    xmlBuilder.append("<article level=\"Advanced\" date=\"January-2004\">");
    xmlBuilder.append("<title>Design service-oriented architecture frameworks with J2EE technology</title>");
    xmlBuilder.append("<author>Naveen Balani </author>");
    xmlBuilder.append("</article>");
    xmlBuilder.append("<article level=\"Advanced\" date=\"October-2003\">");
    xmlBuilder.append("<title>Advance DAO Programming</title>");
    xmlBuilder.append("<author>Sean Sullivan </author>");
    xmlBuilder.append("</article>");
    xmlBuilder.append("</journal>");
    xmlBuilder.append("</catalog>");

    System.out.println("xmlBuilder: " + xmlBuilder.toString());

    File fileXml = new File(temporaryFolder.getRoot(), "file.xml");
    writeXml(fileXml, xmlBuilder.toString());

    XPathFactory  factory=XPathFactory.newInstance();

    XPath xPath=factory.newXPath();

    XPathExpression xPathExpression= xPath.compile("/catalog/journal/article[@date='January-2004']/title");

    InputSource inputSource = new InputSource(new FileInputStream(fileXml));

//    String title = xPathExpression.evaluate(inputSource);
//
//    String publisher = xPath.evaluate("/catalog/journal/@publisher", inputSource);

    //String expression="/catalog/journal/article";

    String expression="//catalog/journal";

    NodeList nodes = (NodeList) xPath.evaluate(expression, inputSource, XPathConstants.NODESET);

    NodeList nodeList=(NodeList)nodes;
    System.out.println(nodeList.getLength());
  }

  @Test
  public void test3() throws Exception {
    StringBuilder xmlBuilder = new StringBuilder();
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    xmlBuilder.append("<cache>");// xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://geode.apache.org/schema/cache\" xsi:schemaLocation=\"http://geode.apache.org/schema/cache http://geode.apache.org/schema/cache/cache-1.0.xsd\" version=\"1.0\">");
    xmlBuilder.append("<jdbc:connector-service xmlns:jdbc=\"http://geode.apache.org/schema/jdbc\" name=\"blah\">");

//    xmlBuilder.append("<cache xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://geode.apache.org/schema/cache\" xmlns:jdbc=\"http://geode.apache.org/schema/jdbc\" xsi:schemaLocation=\"http://geode.apache.org/schema/cache http://geode.apache.org/schema/cache/cache-1.0.xsd\" version=\"1.0\">");
//    xmlBuilder.append("<jdbc:connector-service>");

    xmlBuilder.append("<jdbc:connection name=\"name\" url=\"url\" user=\"username\" password=\"secret\"/>");
    xmlBuilder.append("</jdbc:connector-service>");
    xmlBuilder.append("</cache>");

    System.out.println("xmlBuilder: " + xmlBuilder.toString());

    File fileXml = new File(temporaryFolder.getRoot(), "file.xml");
    writeXml(fileXml, xmlBuilder.toString());

    XPathFactory  factory=XPathFactory.newInstance();

    XPath xPath=factory.newXPath();

    InputSource inputSource = new InputSource(new FileInputStream(fileXml));

//    String title = xPathExpression.evaluate(inputSource);
//
//    String publisher = xPath.evaluate("/catalog/journal/@publisher", inputSource);

    //String expression="/catalog/journal/article";

    String expression="//cache/jdbc:connector-service/jdbc:connection";

    NodeList nodes = (NodeList) xPath.evaluate(expression, inputSource, XPathConstants.NODESET);

    NodeList nodeList=(NodeList)nodes;
    System.out.println(nodeList.getLength());
  }

  private void print(NodeList nodeList) {
    for (int i = 0; i < nodeList.getLength(); i++) {
      System.out.println("node[" + i + "]:" + nodeList.item(i).getNodeName());
      if (nodeList.item(i).getChildNodes().getLength() > 0) {
        print(nodeList.item(i).getChildNodes());
      }
    }
  }

  private void printXml()
      throws IOException, SAXException, ParserConfigurationException {
    File cacheXml = new File(temporaryFolder.getRoot(), "cache.xml");
    for (String line : Files.readAllLines(cacheXml.toPath())) {
      System.out.println(line);
    }
  }

  private void writeXml(File cacheXml, String xml) throws IOException {
    PrintWriter printWriter = new PrintWriter(new FileWriter(cacheXml));
    printWriter.print(xml);
    printWriter.flush();
  }

  private void generateXml() throws IOException {
    File cacheXml = new File(temporaryFolder.getRoot(), "cache.xml");
    PrintWriter printWriter = new PrintWriter(new FileWriter(cacheXml));
    CacheXmlGenerator.generate(cache, printWriter, true, false, false);
    printWriter.flush();
  }

}

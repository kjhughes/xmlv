/*
 * XMLV.java
 *   by Kenneth J Hughes (kjh@entel.com)
 *   Time-stamp: <XMLV.java 2014-05-01 19:34:43 kjh>
 *   Derived from Xerces v2.11 sample, XMLGrammarBuilder.java, by Neil Graham
 *
 * Copyright 2013 Entelechy Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.entel.xml;

import java.util.Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.File;

import org.apache.xerces.impl.Constants;
import org.apache.xerces.parsers.XIncludeAwareParserConfiguration;
import org.apache.xerces.parsers.XMLGrammarPreparser;
import org.apache.xerces.util.SymbolTable;
import org.apache.xerces.util.XMLGrammarPoolImpl;
import org.apache.xerces.util.XMLCatalogResolver;
import org.apache.xerces.xni.grammars.Grammar;
import org.apache.xerces.xni.grammars.XMLGrammarDescription;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParserConfiguration;

public class XMLV {

  /** Property identifier: symbol table. */
  public static final String SYMBOL_TABLE =
    Constants.XERCES_PROPERTY_PREFIX + Constants.SYMBOL_TABLE_PROPERTY;

  /** Property identifier: grammar pool. */
  public static final String GRAMMAR_POOL =
    Constants.XERCES_PROPERTY_PREFIX + Constants.XMLGRAMMAR_POOL_PROPERTY;

  /** Namespaces feature id (http://xml.org/sax/features/namespaces). */
  protected static final String NAMESPACES_FEATURE_ID = "http://xml.org/sax/features/namespaces";

  /** Validation feature id (http://xml.org/sax/features/validation). */
  protected static final String VALIDATION_FEATURE_ID = "http://xml.org/sax/features/validation";

  /** Schema validation feature id (http://apache.org/xml/features/validation/schema). */
  protected static final String SCHEMA_VALIDATION_FEATURE_ID = "http://apache.org/xml/features/validation/schema";

  /** Schema full checking feature id (http://apache.org/xml/features/validation/schema-full-checking). */
  protected static final String SCHEMA_FULL_CHECKING_FEATURE_ID = "http://apache.org/xml/features/validation/schema-full-checking";

  /** Honour all schema locations feature id (http://apache.org/xml/features/honour-all-schemaLocations). */
  protected static final String HONOUR_ALL_SCHEMA_LOCATIONS_ID = "http://apache.org/xml/features/honour-all-schemaLocations";

  protected static final String ENTITY_RESOLVER_PROPERTY_ID = "http://apache.org/xml/properties/internal/entity-resolver";

  // A larg(ish) prime to use for a symbol table to be shared among
  // potentially many parsers.  Start one as close to 2K (20 times
  // larger than normal) and see what happens...
  public static final int BIG_PRIME = 2039;

  /** Default Schema full checking support (false). */
  protected static final boolean DEFAULT_SCHEMA_FULL_CHECKING = false;

  /** Default honour all schema locations (false). */
  protected static final boolean DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS = false;

  /** Main program entry point. */
  public static void main(String argv[]) {

    ArrayList<String> xmlCatalogNamesList = new ArrayList<String>();

    if (argv.length < 2) {
      printUsage();
      System.exit(1);
    }

    XMLParserConfiguration parserConfiguration = null;
    String arg = null;
    int i = 0;

    arg = argv[i];
    if (arg.equals("-p")) {
      i++;
      String parserName = argv[i];

      try {
        ClassLoader cl = ObjectFactory.findClassLoader();
        parserConfiguration = (XMLParserConfiguration)ObjectFactory.newInstance(parserName, cl, true);
      } catch (Exception e) {
        parserConfiguration = null;
        System.err.println("error: Unable to instantiate parser configuration ("+parserName+")");
      }
      i++;
    }

    arg = argv[i];
    if (arg.equals("-c")) { // catalog name
      i++;
      xmlCatalogNamesList.add(argv[i]);
      // System.out.println("Will use XML catalog (" + argv[i] + ").");
      i++;
    }

    arg = argv[i];
    Vector<String> externalDTDs = null;
    if (arg.equals("-d")) {
      externalDTDs= new Vector<String>();
      i++;
      while (i < argv.length && !(arg = argv[i]).startsWith("-")) {
        externalDTDs.addElement(arg);
        i++;
      }
      // Has to be at least one dTD or schema, and there has to be
      // other parameters:
      if (externalDTDs.size() == 0) {
        printUsage();
        System.exit(1);
      }
    }

    Vector<String> schemas = null;
    boolean schemaFullChecking = DEFAULT_SCHEMA_FULL_CHECKING;
    boolean honourAllSchemaLocations = DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS;
    if (i < argv.length) {
      arg = argv[i];
      if (arg.equals("-f")) {
        schemaFullChecking = true;
        i++;
        arg = argv[i];
      } else if (arg.equals("-F")) {
        schemaFullChecking = false;
        i++;
        arg = argv[i];
      }
      if (arg.equals("-hs")) {
        honourAllSchemaLocations = true;
        i++;
        arg = argv[i];
      } else if (arg.equals("-HS")) {
        honourAllSchemaLocations = false;
        i++;
        arg = argv[i];
      }

      if (arg.equals("-a")) { // schema files
        if (externalDTDs != null) {
          printUsage();
          System.exit(1);
        }
        schemas= new Vector<String>();
        i++;
        while (i < argv.length && !(arg = argv[i]).startsWith("-")) {
          schemas.addElement(arg);
          i++;
        }

        // Has to be at least one dTD or schema, and there has to be
        // other parameters
        if (schemas.size() == 0) {
          printUsage();
          System.exit(1);
        }
      }
    }
    Vector<String> ifiles = null;
    if (i < argv.length) {
      if (!arg.equals("-i")) { // instance files
        printUsage();
        System.exit(1);
      }

      i++;
      ifiles = new Vector<String>();
      while (i < argv.length && !(arg = argv[i]).startsWith("-")) {
        ifiles.addElement(arg);
        i++;
      }

      // Has to be at least one instance file, and there has to be no
      // more parameters:
      if (ifiles.size() == 0 || i != argv.length) {
        printUsage();
        System.exit(1);
      }
    }

    // We have all our arguments.  We only need to parse the
    // DTD's/schemas, put them in a grammar pool, possibly instantiate
    // an appropriate configuration, and we're on our way.

    SymbolTable sym = new SymbolTable(BIG_PRIME);
    XMLGrammarPreparser preparser = new XMLGrammarPreparser(sym);
    XMLGrammarPoolImpl grammarPool = new XMLGrammarPoolImpl();
    boolean haveDTDs = false;
    boolean haveXSDs = false;
    if (externalDTDs != null) {
      preparser.registerPreparser(XMLGrammarDescription.XML_DTD, null);
      haveDTDs = true;
      haveXSDs = false;
    } else if (schemas != null) {
      preparser.registerPreparser(XMLGrammarDescription.XML_SCHEMA, null);
      haveDTDs = false;
      haveXSDs = true;
    }
    preparser.setProperty(GRAMMAR_POOL, grammarPool);
    preparser.setFeature(NAMESPACES_FEATURE_ID, true);
    preparser.setFeature(VALIDATION_FEATURE_ID, true);
    // note we can set schema features just in case...
    preparser.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
    preparser.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
    preparser.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, honourAllSchemaLocations);

    if (!xmlCatalogNamesList.isEmpty()) {
      try {
        XMLCatalogResolver resolver = new XMLCatalogResolver();
        resolver.setPreferPublic(true);
        Object [] xmlCatalogNamesArray = xmlCatalogNamesList.toArray();
        String [] xmlCatalogNamesStringArray
          = Arrays.copyOf(xmlCatalogNamesArray,
                          xmlCatalogNamesArray.length,
                          String[].class);
        resolver.setCatalogList(xmlCatalogNamesStringArray);
        preparser.setEntityResolver(resolver);
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    if (haveDTDs || haveXSDs) {
      try {
        if (haveDTDs) {
          for (i = 0; i < externalDTDs.size(); i++) {
            Grammar g = preparser.preparseGrammar(XMLGrammarDescription.XML_DTD, stringToXIS((String)externalDTDs.elementAt(i)));
            // we don't really care about g; grammarPool will take care of everything.
          }
        } else { // must be schemas.
          for (i = 0; i < schemas.size(); i++) {
            String xsdName = (String) schemas.elementAt(i);
            Grammar g = preparser.preparseGrammar(XMLGrammarDescription.XML_SCHEMA, stringToXIS((String) schemas.elementAt(i)));
            // we don't really care about g; grammarPool will take care of everything.
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    // We have a grammar pool and a SymbolTable; just build a
    // configuration and we're on our way!
    if (parserConfiguration == null) {
      parserConfiguration = new XIncludeAwareParserConfiguration(sym, grammarPool);
    } else {
      parserConfiguration.setProperty(SYMBOL_TABLE, sym);
      parserConfiguration.setProperty(GRAMMAR_POOL, grammarPool);
    }
    // Must reset features, unfortunately:
    try {
      parserConfiguration.setFeature(NAMESPACES_FEATURE_ID, true);
      parserConfiguration.setFeature(VALIDATION_FEATURE_ID, true);
      // We can still do schema features just in case, so long as it's
      // our configuration...
      parserConfiguration.setFeature(SCHEMA_VALIDATION_FEATURE_ID, true);
      parserConfiguration.setFeature(SCHEMA_FULL_CHECKING_FEATURE_ID, schemaFullChecking);
      parserConfiguration.setFeature(HONOUR_ALL_SCHEMA_LOCATIONS_ID, honourAllSchemaLocations);
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
    // then for each instance file, try to validate it
    if (ifiles != null) {
      try {
        for (i = 0; i < ifiles.size(); i++) {
          parserConfiguration.parse(stringToXIS((String)ifiles.elementAt(i)));
        }
      } catch (Exception e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
  }

  private static void printUsage() {
    System.err.println("usage: java -jar xmlv.jar [-p name] [-c xmlCatalog] -d uri ... [-f|-F] [-hs|-HS] -a uri ... [-i uri ...]");
    System.err.println();
    System.err.println("options:");
    System.err.println("  -p name       Select parser configuration by name to use for instance validation");
    System.err.println("  -c xmlCatalog Provide an XML Catalog file.");
    System.err.println("  -d            Grammars to preparse are DTD external subsets");
    System.err.println("  -f  | -F      Turn on/off Schema full checking (default "+
                       (DEFAULT_SCHEMA_FULL_CHECKING ? "on" : "off)"));
    System.err.println("  -hs | -HS     Turn on/off honouring of all schema locations (default "+
                       (DEFAULT_HONOUR_ALL_SCHEMA_LOCATIONS ? "on" : "off)"));
    System.err.println("  -a uri ...    Provide a list of schema documents");
    System.err.println("  -i uri ...    Provide a list of instance documents to validate");
    System.err.println();
    System.err.println("NOTE: Both -d and -a cannot be specified.");
  }

  private static XMLInputSource stringToXIS(String uri) {
    return new XMLInputSource(null, uri, null);
  }
}


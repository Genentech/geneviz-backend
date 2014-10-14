package com.gene.compbio.geneviz;

import au.com.bytecode.opencsv.CSVReader;
import org.apache.commons.io.FileUtils;
import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.helpers.collection.MapUtil;
import org.neo4j.unsafe.batchinsert.BatchInserter;
import org.neo4j.unsafe.batchinsert.BatchInserters;

import java.io.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Imports genes.csv, datasets.csv, links.csv and meta.csv TCGA data into a neo4j db
 *
 * @author masselot.alexandre@gene.com, rinaldi.jacob@gene.com
 *         Copyright 2014 Genentech, Inc.
 */
public class Neo4jBatchImporter {
    final String dirDataIn;
    final String dirNeo4j;
    final String dirDB;
    final BatchInserter batchInserter;

    Map<String, Long> geneIndex = new HashMap<String, Long>();

    /**
     * Instanciate the batch inserter and clean up the directory.
     * The actual directory where the graph.db will be created is read from the dirNeo4j/conf/neo4j-server.properties file, under property "org.neo4j.server.database.location"
     * <p/>
     * Warning: this directory will be wiped out clean!
     *
     * @param dirDataIn
     * @param dirNeo4j
     */
    public Neo4jBatchImporter(String dirDataIn, String dirNeo4j) throws IOException {
        InputStream input = new FileInputStream(dirNeo4j + "/conf/neo4j-server.properties");
        Map<String, String> config = MapUtil.load(input);

        this.dirDataIn = dirDataIn;
        this.dirNeo4j = dirNeo4j;
        String confDir = config.get("org.neo4j.server.database.location");
        dirDB = confDir.startsWith("/") ? confDir : (dirNeo4j + "/" + confDir);

        System.out.println("data from " + dirDataIn);
        System.out.println("neo4j     " + dirNeo4j);
        System.out.println("db dir    " + dirDB);


        FileUtils.deleteDirectory(new File(dirDB));

        batchInserter = BatchInserters.inserter(dirDB, config);
    }

    /**
     * create a label and set the fields on which it should indexed
     *
     * @param name
     * @param indexed
     * @return
     */
    private Label createLabel(String name, String... indexed) {
        Label lab = DynamicLabel.label(name);
        for (String ind : indexed) {
            batchInserter.createDeferredSchemaIndex(lab).on(ind).create();
        }
        return lab;
    }

    /**
     * return a CVSReader, and skip the first line
     *
     * @param file
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private Iterator<String[]> openCSV(String file) throws FileNotFoundException, IOException {
        final CSVReader reader = new CSVReader(new FileReader(dirDataIn + "/" + file));
        reader.readNext(); //skip the header line
        return new Iterator<String[]>() {
            String[] nextLine = null;

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean hasNext() {
                try {
                    nextLine = reader.readNext();
                    return nextLine != null;
                } catch (IOException e) {
                    return false;
                }
            }

            @Override
            public String[] next() {
                try {
                    String[] ret = (nextLine == null) ? reader.readNext() : nextLine;
                    nextLine = null;
                    return ret;
                } catch (IOException e) {
                    return null;
                }
            }
        };
    }

    /**
     * load genes from the genes.csv
     * update the geneIndex Map based on the symbol (well, that should be "id", but we do one step at a time)
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected Neo4jBatchImporter loadGenes() throws FileNotFoundException, IOException {
        Label geneLabel = createLabel("gene", "id", "symbol");

        Iterator<String[]> itCSV = openCSV("genes.csv");
        int iTot = 0;
        while (itCSV.hasNext()) {
            Gene gene = new Gene(itCSV.next());
            long l = batchInserter.createNode(gene.toProperties(), geneLabel);
            geneIndex.put(gene.id, l);
            iTot++;
        }
        System.out.println("inserted genes:" + iTot);
        return this;
    }

    /**
     * Loads the datasets.csv file and create orphan nodes.
     *
     * @return this
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected Neo4jBatchImporter loadDataset() throws FileNotFoundException, IOException {
        Label label = createLabel("dataset");

        Iterator<String[]> itCSV = openCSV("datasets.csv");
        int i = 0;
        while (itCSV.hasNext()) {
            Dataset dataset = new Dataset(itCSV.next());
            long l = batchInserter.createNode(dataset.toProperties(), label);
            i++;
        }
        System.out.println("inserted dataset:" + i);
        return this;
    }

    /**
     * returns the geneNode long index for the given id.
     *
     * @param geneId
     * @return this
     * @throws NullPointerException if no gnene was registered under this id
     */
    private Long getGeneNode(String geneId) throws NullPointerException {
        if (geneIndex.get(geneId) == null) {
            throw new NullPointerException("no gene for [" + geneId + "]");
        }
        return geneIndex.get(geneId);
    }

    /**
     * Load the metagenes from meta.csv.
     * Create nodes (with labels "metagene") and relationship ("HAD_META") to genes
     *
     * @return this
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected Neo4jBatchImporter loadMetagenes() throws FileNotFoundException, IOException {
        Label label = createLabel("metagene");
        RelationshipType hasMeta = DynamicRelationshipType.withName("HAS_META");

        Iterator<String[]> itCSV = openCSV("meta.csv");
        int iTot = 0;
        int iMiss = 0;
        while (itCSV.hasNext()) {
            iTot++;
            Meta meta = new Meta(itCSV.next());
            long l = batchInserter.createNode(meta.toProperties(), label);
            try {
                batchInserter.createRelationship(getGeneNode(meta.geneId), l, hasMeta, null);
            } catch (NullPointerException e) {
                iMiss++;
                System.err.println("loadMetagenes: " + e.getMessage());
            }
        }
        System.out.println("inserted metagenes: missing " + iMiss + " out of " + iTot);
        return this;
    }

    /**
     * Load the links.csv file, i.e. correlation between genes together with the dataset.
     * No nodes are crete, only "IS_CORRELATED" relationships
     *
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    protected Neo4jBatchImporter loadCorrelationLinks() throws FileNotFoundException, IOException {
        RelationshipType relType = DynamicRelationshipType.withName("CORRELATED");

        Iterator<String[]> itCSV = openCSV("links.csv");
        int iTot = 0;
        int iMiss = 0;
        while (itCSV.hasNext()) {
            LinkCorrelation link = new LinkCorrelation(itCSV.next());
            iTot++;
            try {
                batchInserter.createRelationship(getGeneNode(link.geneIdA), getGeneNode(link.geneIdB), relType, link.toProperties());
            } catch (NullPointerException e) {
                iMiss++;
                System.err.println("loadCorrelationLinks: " + e.getMessage());
            }

        }
        System.out.println("inserted correlation links: missing " + iMiss + " out of " + iTot);
        return this;
    }


    /**
     * shoutdown the batch inserter.
     * You're ready to restart your database
     *
     * @return
     */
    protected Neo4jBatchImporter close() {
        batchInserter.shutdown();
        return this;
    }


    /**
     * usage BatchImporter importDir neo4jDir.
     * <p/>
     * importDir contains the csv files datasets.csv, genes.csv, links.csv and meta.csv
     * neo4jDir is the installed path of the neo4j system
     *
     * @param argv
     */
    public static void main(String[] argv) {
        try {
            new Neo4jBatchImporter(argv[0], argv[1])
                    .loadGenes()
                    .loadDataset()
                    .loadMetagenes()
                    .loadCorrelationLinks()
                    .close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

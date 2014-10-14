#geneViz backend
The main goal of this package is to provide a tool to load csv files into the neo4j database.

##Prerequisites
### The Data Files
Four type of files, describing the network must be available in CSV format:
####dataset.csv
The smallest one, containing a list of the available dataset, with for example
 
     "","dataset"
     "1","All"
####genes.csv
With the genes description.

    "","entrezId","geneSymbol","chromosome"
    "1",1,"A1BG","chr19"
    
####meta.csv
Containing the various metagenes information. Metagenes are linked to gene and dataset, as they also contain HOW IS THE EXPRESSION COMUTED

    "","geneSymbol","metagene","expression","chromosome","dataset"
    "1","1","ZMYND10 Cluster","2.31589177729126","chr19","All"
####links.csv
Handles the correlation between the genes, with gene ids, dataset and correlation factor
 
    "","geneIdA","cor","geneIdB","dataset"
    "1","1","0.757122757862342","503538","All"

### A Local Neo4 Installation
As the data can be too large for basic insertion in the database, we opted for the [batch inserter](http://docs.neo4j.org/chunked/stable/batchinsert.html) method.
Therefore the loader progam must have access to the neo4j library, which will be **wiped out** before importing.
Our application has been tested with the [http://neo4j.com/download/](2.1.3 community edition). The installation consist mainly in downloading the archive and untar it.
Configuration can be tuned for directory path, memory, caching etc. 

##Install the Neo4j Importer
Clone this project and build it. As simple as:

    mvn clean compile assembly:single

##Load the data
You must first **stop neo4j server**, then run the loader:
    
    java -jar target/geneviz-backend-0.2-SNAPSHOT-jar-with-dependencies.jar dir/to/csv-files dir/to/neo4j
    
##Configure neo4j
To get faster queries, you can configure neo4j server to have a higher max memory than the default value. Edit <code>conf/neo4j-wrapper.conf</code> with:

    wrapper.java.maxmemory=3000
    
And start neo4j back.

##Deploy
You can either run neo4j locally or copy the database directory (by default *data/graph.db*) to neo4j cluster. 

##License
This software is covered by the BSD license.

##Authors
Alexandre Masselot, Jacob Rinaldi, Johnny Wu. Bioinformatics & Computational Biology Department, gRED, Genentech
package com.gene.compbio.geneviz;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexandre Masselot on 8/26/14.
 */
public class LinkCorrelation implements HasProperties {
    public final String geneIdA;
    public final String geneIdB;
    public final Double correlation;
    public final String dataset;
    public LinkCorrelation(String[] line){
        geneIdA=line[1];
        correlation=new Double(Double.parseDouble(line[2]));
        geneIdB=line[3];
        dataset=line[4];
    }

    @Override
    public Map<String, Object> toProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("correlation", correlation);
        properties.put("dataset", dataset);
        return properties;
    }

    @Override
    public String toString() {
        return geneIdA+"->"+geneIdB;
    }
}

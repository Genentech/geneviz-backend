package com.gene.compbio.geneviz;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexandre Masselot on 8/26/14.
 */
public class Meta implements HasProperties {
    public final String geneId;
    public final String metagene;
    public final Double expression;
    public final String chromosome;
    public final String dataset;

    public Meta(String[] line) {
        geneId = line[1];
        metagene = line[2];
        expression = new Double(Double.parseDouble(line[3]));
        chromosome=line[4];
        dataset = line[5];
    }

    @Override
    public Map<String, Object> toProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("geneId", geneId);
        properties.put("metagene", metagene);
        properties.put("expression", expression);
        properties.put("chromosome", chromosome);
        properties.put("dataset", dataset);
        return properties;
    }

    @Override
    public String toString() {
        return "metagene:" + metagene;
    }
}

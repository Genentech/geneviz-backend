package com.gene.compbio.geneviz;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexandre Masselot on 8/26/14.
 */
public class Gene implements HasProperties {
    public final String id;
    public final String symbol;
    public final String chromosome;
    public Gene(String[] line){
        id=line[1];
        symbol=line[2];
        chromosome=line[3];
    }
    @Override
    public Map<String, Object> toProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("id", id);
        properties.put("symbol", symbol);
        properties.put("chromosome", chromosome);
        return properties;
    }

    @Override
    public String toString() {
        return "id:"+id;
    }
}

package com.gene.compbio.geneviz;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Alexandre Masselot on 8/26/14.
 */
public class Dataset implements HasProperties {
    public final String name;

    public Dataset(String[] line) {
        name = line[1];
    }

    @Override
    public Map<String, Object> toProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("name", name);
        return properties;
    }

    @Override
    public String toString() {
        return name;
    }
}

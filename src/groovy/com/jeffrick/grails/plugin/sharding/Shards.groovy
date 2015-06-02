package com.jeffrick.grails.plugin.sharding

import org.codehaus.groovy.grails.commons.GrailsDomainClass;

class Shards {
    static List<ShardConfig> shards = []
    static String indexDataSourceName
    static ConfigObject indexDataSourceConfig
    static GrailsDomainClass indexDomainClass
    static list() {
        return shards
    }
    static public String getIndexDatabaseURL() {
        return indexDataSourceConfig.getProperty("url")
    }
}

package com.jeffrick.grails.plugin.sharding

class Shards {
    static List<ShardConfig> shards = []
    static String indexDataSourceName
    static ConfigObject indexDataSourceConfig
    static list() {
        return shards
    }
    static public String getIndexDatabaseURL() {
        return indexDataSourceConfig.getProperty("url")
    }
}

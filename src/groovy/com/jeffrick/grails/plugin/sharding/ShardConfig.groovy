package com.jeffrick.grails.plugin.sharding

/**
 * @author <a href='mailto:jeffrick@gmail.com'>Jeff Rick</a>
 */
class ShardConfig {
  int id
  String name
  ConfigObject dataSourceConfig

  Boolean autoCommit = true
}

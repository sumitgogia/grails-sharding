package com.jeffrick.grails.plugin.sharding

import com.jeffrick.grails.plugin.sharding.annotation.Shard

import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.AbstractPlatformTransactionManager

/**
 * This class holds the current shard we are pointing, values are stored as thread locals
 * @author <a href='mailto:jeffrick@gmail.com'>Jeff Rick</a>
 */
class CurrentShard {

    private static final ThreadLocal _shardIndex
    private static final ThreadLocal _autoCommit
    private static HashMap<String, ConfigObject> _dataSourceLookup

    static {
        _shardIndex = new ThreadLocal();
        _autoCommit = new ThreadLocal();
        _dataSourceLookup = new HashMap<String,ConfigObject>()
    }

    static ShardConfig get() {
        Integer currentId = getIndex()
        ShardConfig current = null;
        return Shards.getShards().find { it.id == currentId }
    }

    /** *
     * Gets the index of the current shard
     */
    static Integer getIndex() {
        if (_shardIndex.get() == null) {
            _shardIndex.set(1);
        }
        return ((long) _shardIndex.get())
    }

    /** *
     * Gets whether the current shard is set to Auto Commit
     */
    static boolean getAutoCommit() {
        if (_autoCommit.get() == null) {
            return true
        } else {
            return ((boolean) _autoCommit.get())
        }
    }

    /** *
     * Sets whether the current shard is set to Auto Commit
     */
    static void setAutoCommit(boolean autoCommit) {
        _autoCommit.set autoCommit;
    }

    /** *
     * Sets the current shard, requires a map that contains a index element that refers to the index
     * for the shard
     */
    static void setShard(ShardConfig shard) {
        _shardIndex.set(shard.id)
    }

    static void setDataSourceLookup(HashMap<String, ConfigObject> dataSourceLookup) {
        _dataSourceLookup = dataSourceLookup
    }

}


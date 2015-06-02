import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionDefinition;

import com.jeffrick.grails.plugin.sharding.ShardConfig;
import com.jeffrick.grails.plugin.sharding.Shards
import com.jeffrick.grails.plugins.sharding.Shard

class ShardingBootStrap {
    def init = { servletContext ->

        Shard.withTransaction([isolationLevel: TransactionDefinition.ISOLATION_READ_COMMITTED]) {
            Shards.shards.each { ShardConfig shard ->
                // Make sure the shard is in the Shard table
                if (!Shard.findByShardName(shard.name)) {
                    try {
                        new Shard(
                            shardName: shard.name,
                            shardCapacity: 1000,
                            shardUsage: 0,
                            ratio: 0.0
                        ).save()
                    } catch(DataIntegrityViolationException de) {
                        log.debug("May be another cluster instance added the Shard!", de)
                    }
               }
            }
        }

    }
}

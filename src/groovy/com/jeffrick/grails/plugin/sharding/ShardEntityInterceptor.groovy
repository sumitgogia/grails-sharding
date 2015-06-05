package com.jeffrick.grails.plugin.sharding

import org.hibernate.EmptyInterceptor
import org.hibernate.Transaction
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionDefinition

/**
 * Synchronizes the transaction in the shard database with the one in the index database.
 * @author <a href='mailto:jeffrick@gmail.com'>Jeff Rick</a>
 */
class ShardEntityInterceptor extends EmptyInterceptor implements ApplicationContextAware {
    ApplicationContext applicationContext

    void afterTransactionBegin(Transaction transaction) {
        // If a transaction on the index DB has not been started (by us) then
        // start one. If a transaction on the index DB is already started, then
        // participate in the same one. (This is ensured by PROPAGATION_REQUIRED
        // used by DefaultTransactionDefinition
        String indexDbUrl = Shards.getIndexDatabaseURL()
        if (CurrentShard.getTransactionStatus(indexDbUrl)) {
            // Get the transaction manager for the index database
            AbstractPlatformTransactionManager txManager =
                applicationContext.getBean("transactionManager_" +
                                           Shards
                                            .getIndexDataSourceName()
                                            .replaceFirst("dataSource(_)?", ""))

            txManager.setTransactionSynchronization(
                AbstractPlatformTransactionManager.SYNCHRONIZATION_NEVER)

            CurrentShard.setTransactionManager(txManager)

            // Create a new (or participate in existing) transaction and
            // store it for later use
            TransactionDefinition txDef = new DefaultTransactionDefinition()
            TransactionStatus tx = txManager.getTransaction(txDef)

            // Set the transaction status
            CurrentShard.setTransactionStatus(indexDbUrl, tx)
        }
    }

    void afterTransactionCompletion(Transaction transaction) {
        // Handle the completion side of the transaction
        // If the transaction was committed then we need to commit the
        // transaction on index Db, otherwise we rollback
        String action = transaction?.wasCommitted() ? "commit" : "rollback"
        CurrentShard.getTransactionStatus().each { TransactionStatus tx ->
            if (!tx?.completed) {
                CurrentShard.getTransactionManager()?."$action"(tx)
            }
        }

        CurrentShard.clearTransactionStatus()

        super.afterTransactionCompletion(transaction)
    }
}

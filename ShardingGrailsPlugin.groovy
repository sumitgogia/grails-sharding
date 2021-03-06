import org.codehaus.groovy.grails.commons.DefaultGrailsDomainClass
import org.codehaus.groovy.grails.commons.GrailsApplication

import com.jeffrick.grails.plugin.sharding.CurrentShard
import com.jeffrick.grails.plugin.sharding.ShardConfig
import com.jeffrick.grails.plugin.sharding.ShardingDS
import com.jeffrick.grails.plugin.sharding.Shards
import com.jeffrick.grails.plugin.sharding.annotation.Shard as ShardAnnotation
import com.jeffrick.grails.plugins.services.ShardService
import com.jeffrick.grails.plugins.sharding.Shard

class ShardingGrailsPlugin {
    def version = "1.1-cz1"
    def grailsVersion = "2.0.0 > *"
    def loadAfter = ['dataSource', 'domainClass', 'hibernate']
    def author = "Jeff Rick"
    def authorEmail = "jeffrick@gmail.com"
    def title = "Grails Shards Plugin"
    def description = 'Supports sharding of data'
    def documentation = "http://grails.org/plugin/sharding"

    def license = 'APACHE'
    def scm = [url: 'https://github.com/jrick1977/grails-sharding']
//    def issueManagement = [system: 'JIRA', url: 'http://jira.grails.org/browse/???']
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]
    def developers = [ [ name: "Jeff Rick", email: "jeffrick@gmail.com" ]]

    def doWithSpring = {

        List<ShardConfig> shards = []
        Map shardDataSources = [:]
        int shardId = 1

        application.config.each { String key, Object value ->
            if (key.startsWith('dataSource')) {
                if (value.getProperty("shard")) {
                    shardDataSources[shardId] = ref(key)
                    shards << new ShardConfig(
                        id: shardId,
                        name: key.replaceFirst("dataSource(_)?", "") ?: "DEFAULT",
                        dataSourceConfig: value
                    )
                    shardId++
                }
            }
        }

        Shards.shards = shards

        // Create the dataSource bean that has the Shard specific
        // SwitchableDataSource implementation. Give it all the
        // available/configured shards (i.e. datasources with shard = true)
        // or the default datasource if no shards are configured
        if (!shardDataSources) {
            shardDataSources[shardId] = ref('dataSource')
        }
        "dataSource_shard"(ShardingDS) {
            targetDataSources = shardDataSources
        }
    }

    def doWithDynamicMethods = { ctx ->

        // Find the domain class the owning application has defined as
        // the "Index" domain class.  This domain class is used to store
        // the list of objects and the shard they live in
        Shards.with {
            (indexDataSourceName, indexDataSourceConfig, indexDomainClass) =
                getIndexDataSourceInfo(application)
        }

        // For the index domain class add a beforeInsert event handler
        // that will assign the next shard to the object being saved.
        // In the future will need to be able to chain this event with existing
        // beforeInsert event handlers
        Shards.indexDomainClass.clazz.metaClass.beforeInsert = {->
            ShardAnnotation prop =
                ((DefaultGrailsDomainClass)Shards.indexDomainClass)
                    .clazz.getAnnotation(ShardAnnotation)

            ShardService shardService = ctx.shardService

            // Before we insert we need to figure out the shard to assign
            // ourselves to
            def shardObject = shardService.getNextShard()
            shardObject.refresh()

            // Set the shard on the object
            String fieldName = prop.fieldName()
            if (!delegate."$fieldName") {
                delegate."$fieldName" = shardObject.shardName

                // Increment the usage of the shard assigned
                Shard.withNewSession {
                    shardObject.refresh()
                    shardObject.incrementUsage()
                }

                shardObject.refresh()
            }

            return true
        }
    }

    private List getIndexDataSourceInfo(GrailsApplication grailsApplication) {
        DefaultGrailsDomainClass indexDomainClass = grailsApplication.domainClasses.find {
            it.clazz.isAnnotationPresent(ShardAnnotation)
        }
        String indexDataSourceName = indexDomainClass?.clazz
                                        ?.getAnnotation(ShardAnnotation)
                                        ?.indexDataSourceName()

        if (!indexDataSourceName) {
            throw new Exception("Error no domain class registered as a Shard lookup class!")
        }

        ConfigObject indexDataSourceConfig = grailsApplication.config[indexDataSourceName]

        if (!indexDataSourceConfig) {
            throw new Exception("Error! No datasource defined for name $indexDataSourceName")
        }

        return [indexDataSourceName, indexDataSourceConfig, indexDomainClass]
    }

}

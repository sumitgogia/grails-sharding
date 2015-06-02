grails.project.work.dir = 'target'
grails.project.source.level = 1.6

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {

  inherits 'global'
  log 'warn'

  repositories {
    grailsHome()
    grailsCentral()

    mavenLocal()
    mavenCentral()
  }

  dependencies {
    compile 'c3p0:c3p0:0.9.1', {
      export = false
    }
    // explicitly here, because of the exclusion in the hibernate4 plugin below
    compile('net.sf.ehcache:ehcache-core:2.5.2') {
        export = false
    }
  }

  plugins {
    build(':release:3.1.1') {
      export = false
    }
    runtime(":hibernate4:4.3.8.1") {
      // conflicts with ehcache-core and is the wrong version
      excludes "ehcache"
      export = false
    }
  }
}

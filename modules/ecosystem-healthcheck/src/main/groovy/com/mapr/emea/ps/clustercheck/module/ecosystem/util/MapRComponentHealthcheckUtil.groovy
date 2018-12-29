package com.mapr.emea.ps.clustercheck.module.ecosystem.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Component

@Component
class MapRComponentHealthcheckUtil {

    static final Logger log = LoggerFactory.getLogger(MapRComponentHealthcheckUtil.class)

    static final String PATH_ECO_SYS = ".clustercheck/ecosystem-healthcheck"

    static final String PATH_CLASSPATH = "/com/mapr/emea/ps/clustercheck/module/ecosystem/healthcheck"

    @Autowired
    @Qualifier("ssh")
    def ssh

    @Autowired
    @Qualifier("globalYamlConfig")
    Map<String, ?> globalYamlConfig

    @Autowired
    ResourceLoader resourceLoader

    def suStr(String ticketFile, exec) {
        return "su ${globalYamlConfig.mapr_user} -c 'export MAPR_TICKETFILE_LOCATION=${ticketFile};${exec}'"
    }

    /**
     * Retrieve nodes where MapR Packages installed
     * @param role
     * @return
     */
    def retrievePackages(role) {

        log.trace("Start : MapRComponentHealthcheckUtil : retrievePackages")

        def packages = Collections.synchronizedList([])
        ssh.runInOrder {
            settings {
                pty = true
                ignoreError = true
            }
            session(ssh.remotes.role(role)) {
                def node = [:]
                node['host'] = remote.host
                def distribution = execute("[ -f /etc/system-release ] && cat /etc/system-release || cat /etc/os-release | uniq")
                if (distribution.toLowerCase().contains("ubuntu")) {
                    node['mapr.packages'] = executeSudo('apt list --installed | grep mapr').tokenize('\n')
                } else {
                    node['mapr.packages'] = executeSudo('rpm -qa | grep mapr').tokenize('\n')
                }
                packages.add(node)
            }
        }

        log.trace("End : MapRComponentHealthcheckUtil : retrievePackages")
        return packages
    }

    /**
     * Find hosts with package installed
     * @param packages
     * @param packageName
     * @return
     */
    static List<Object> findHostsWithPackage(List packages, packageName) {
        log.trace("Start : MapRComponentHealthcheckUtil : findHostsWithPackage")

        def hostsFound = packages.findAll { it['mapr.packages'].find { it.contains(packageName) } != null }.collect { it['host'] }

        log.trace("End : MapRComponentHealthcheckUtil : findHostsWithPackage")

        return hostsFound
    }

    /**
     * Execute commands remotely, remote hosts are detected automatically by checking packageName
     * @param packages
     * @param packageName
     * @param closure
     * @return
     */
    def executeSsh(List<Object> packages, String packageName, Closure closure) {
        log.trace("Start : MapRComponentHealthcheckUtil : executeSsh")

        def appHosts = findHostsWithPackage(packages, packageName)

        log.debug("Found ${packageName} installed on nodes : ${appHosts}")

        def result = Collections.synchronizedList([])
        appHosts.each { appHost ->
            log.info(">>>>>>> ..... testing node ${appHost}")
            ssh.runInOrder {
                settings {
                    pty = true
                    ignoreError = true
                }
                session(ssh.remotes.role(appHost)) {
                    def node = [:]
                    node['host'] = remote.host
                    closure.delegate = delegate
                    node += closure()
                    result.add(node)
                }
            }
        }

        log.trace("End : MapRComponentHealthcheckUtil : executeSsh")
        result
    }

    /**
     * Upload local file to remote
     * @param fileName
     * @param delegate
     * @return
     */
    def uploadFile(String fileName, delegate) {
        log.trace("Start : MapRComponentHealthcheckUtil : uploadFile")

        def homePath = delegate.execute 'echo $HOME'
        delegate.execute "mkdir -p ${homePath}/${PATH_ECO_SYS}/"
        def fileInputStream = resourceLoader.getResource("classpath:${PATH_CLASSPATH}/${fileName}").getInputStream()
        delegate.put from: fileInputStream, into: "${homePath}/${PATH_ECO_SYS}/${fileName}"
        def path = "${homePath}/${PATH_ECO_SYS}/${fileName}"

        log.trace("End : MapRComponentHealthcheckUtil : uploadFile")

        return path
    }

    /**
     * Upload remote file to MapR-FS
     * @param ticketfile
     * @param fileName
     * @param maprfspath
     * @param delegate
     * @return
     */
    def uploadRemoteFileToMaprfs(String ticketfile, String fileName, String maprfspath, delegate){
        log.trace("Start : MapRComponentHealthcheckUtil : uploadRemoteFileToMaprfs")

        delegate.executeSudo "MAPR_TICKETFILE_LOCATION=${ticketfile} hadoop fs -put ${fileName} ${maprfspath}"

        log.trace("End : MapRComponentHealthcheckUtil : uploadRemoteFileToMaprfs")

        return maprfspath
    }

    /**
     * Remove MapR-FS file/directory if exists
     * @param ticketfile
     * @param fileName
     * @param delegate
     * @return
     */
    def removeMaprfsFileIfExist(String ticketfile, String fileName, delegate){
        log.trace("Start : MapRComponentHealthcheckUtil : removeMaprfsFileIfExist")

        log.info("Testing existence of MapR-FS directory: ${fileName} ... Error with status 1 when it doesn't exist.")

        def result = delegate.executeSudo "MAPR_TICKETFILE_LOCATION=${ticketfile} hadoop fs -ls ${fileName}"

        if(result.contains("No such file or directory")){
            log.debug("MapR-FS file/directory : ${fileName} doesn't exist.")
        } else {
            log.debug("${fileName} exists, will be removed.")
            delegate.executeSudo "MAPR_TICKETFILE_LOCATION=${ticketfile} hadoop fs -rm -r ${fileName}"
        }

        log.trace("End : MapRComponentHealthcheckUtil : removeMaprfsFileIfExist")
    }

}

package com.mapr.emea.ps.clustercheck.module.ecosystem.ecoSystemComponent

import com.mapr.emea.ps.clustercheck.module.ecosystem.util.MapRComponentHealthcheckUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class EcoSystemYarn {

    static final Logger log = LoggerFactory.getLogger(EcoSystemYarn.class)

    static final String PACKAGE_NAME_YARN_RESOURCEMANAGER = "mapr-resourcemanager"
    static final String PACKAGE_NAME_YARN_NODEMANAGER = "mapr-nodemanager"
    static final String PACKAGE_NAME_HADOOP_CORE = "mapr-hadoop-core"
    static final String PACKAGE_NAME_YARN_HISTORY_SEVER = "mapr-historyserver"

    @Autowired
    MapRComponentHealthcheckUtil mapRComponentHealthcheckUtil

    /**
     * Verify ResourceManager UI, REST Client Authentication with Pam (Pam is mandatory)
     * @param packages
     * @param credentialFileREST
     * @param port
     * @return
     */
    def verifyResourceManagerUIPam(List<Object> packages, String credentialFileREST, int port) {

        log.trace("Start : EcoSystemYarn : verifyResourceManagerUIPam")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_RESOURCEMANAGER, {
            def nodeResult = [:]

            final String query = "curl -Is -k --netrc-file ${credentialFileREST} https://${remote.host}:${port}/ws/v1/cluster/info | head -n 1"

            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['comment'] = "Only one Resourcemanger is running, the others are standby."
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyResourceManagerUIPam")

        testResult
    }

    /**
     * Verify ResourceManager UI, REST Client Authentication with SSL and Pam (Pam is mandatory)
     * @param packages
     * @param username
     * @param password
     * @param certificate
     * @param port
     * @return
     */
    def verifyResourceManagerUIPamSSL(List<Object> packages, String certificate, String credentialFileREST, int port) {

        log.trace("Start : EcoSystemYarn : verifyResourceManagerUIPamSSL")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_RESOURCEMANAGER, {
            def nodeResult = [:]

            final String query = "curl -Is --cacert ${certificate} --netrc-file ${credentialFileREST}  https://${remote.host}:${port}/ws/v1/cluster/info | head -n 1"

            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['comment'] = "Only one Resourcemanger is running, the others are standby."
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyResourceManagerUIPamSSL")

        testResult
    }

    /**
     * Verify NodeManager UI, REST Client Authentication with Pam (Pam is mandatory)
     * @param packages
     * @param credentialFileREST
     * @param port
     * @return
     */
    def verifyNodeManagerUIPam(List<Object> packages, String credentialFileREST, int port) {

        log.trace("Start : EcoSystemYarn : verifyNodeManagerUIPam")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_NODEMANAGER, {
            def nodeResult = [:]

            final String query = "curl -Is -k --netrc-file ${credentialFileREST} https://${remote.host}:${port}/ws/v1/node | head -n 1"

            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyNodeManagerUIPam")

        testResult
    }

    /**
     * Verify NodeManager UI, REST Client Authentication with SSL and Pam (Pam is mandatory)
     * @param packages
     * @param username
     * @param password
     * @param certificate
     * @param port
     * @return
     */
    def verifyNodeManagerUIPamSSL(List<Object> packages, String certificate, String credentialFileREST, int port) {

        log.trace("Start : EcoSystemYarn : verifyNodeManagerUIPamSSL")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_NODEMANAGER, {
            def nodeResult = [:]

            final String query = "curl -Is --cacert ${certificate} --netrc-file ${credentialFileREST} https://${remote.host}:${port}/ws/v1/node | head -n 1"

            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyNodeManagerUIPamSSL")

        testResult
    }

    /**
     * Verify ResourceManager UI, REST Client Authentication with Insecure mode
     * @param packages
     * @param port
     * @return
     */
    def verifyRsourceManagerUIInSecure(List<Object> packages, int port) {

        log.trace("Start : EcoSystemYarn : verifyRsourceManagerUIInSecure")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_RESOURCEMANAGER, {
            def nodeResult = [:]

            final String query = "curl -Is http://${remote.host}:${port}/ws/v1/cluster/info | head -n 1"

            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyRsourceManagerUIInSecure")

        testResult
    }

    /**
     * Verify NodeManager UI, REST Client Authentication with Insecure mode
     * @param packages
     * @param port
     * @return
     */
    def verifyNodeManagerUIInSecure(List<Object> packages, int port) {

        log.trace("Start : EcoSystemYarn : verifyNodeManagerUIInSecure")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_NODEMANAGER, {
            def nodeResult = [:]

            final String query =  "curl -Is http://${remote.host}:${port}/ws/v1/node | head -n 1"
            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyNodeManagerUIInSecure")

        testResult
    }

    /**
     * Verify Yarn, submitting a MapReduce Job, Pi example, using MapR-SASL
     * @param packages
     * @param ticketfile
     * @return
     */
    def verifyYarnCommandMapRSasl(List<Object> packages, String ticketfile) {

        log.trace("Start : EcoSystemYarn : verifyYarnCommandMapRSasl")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_HADOOP_CORE, {
            def nodeResult = [:]

            final String query = "MAPR_TICKETFILE_LOCATION=${ticketfile} yarn application -list; echo \$?"
            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].contains("Total number of applications") && nodeResult['output'].toString().reverse().take(1).equals("0")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyYarnCommandMapRSasl")

        testResult
    }

    /**
     * Verify History Sever UI, REST Client Authentication with Pam (Pam is mandatory)
     * @param packages
     * @param credentialFileREST
     * @param port
     * @return
     */
    def verifyYarnHistoryServerPam(List<Object> packages, String credentialFileREST, int port) {

        log.trace("Start : EcoSystemYarn : verifyYarnHistoryServerPam")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_HISTORY_SEVER, {
            def nodeResult = [:]

            final String query = "curl -X post -Is -k --netrc-file ${credentialFileREST} https://${remote.host}:${port}/jobhistory | head -n 1"
            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyYarnHistoryServerPam")

        testResult
    }

    /**
     * Verify History Sever UI, REST Client Authentication with SSL and Pam (Pam is mandatory)
     * @param packages
     * @param username
     * @param password
     * @param certificate
     * @param port
     * @return
     */
    def verifyYarnHistoryServerPamSSL(List<Object> packages, String certificate, String credentialFileREST, int port) {

        log.trace("Start : EcoSystemYarn : verifyYarnHistoryServerPamSSL")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_HISTORY_SEVER, {
            def nodeResult = [:]

            final String query = "curl -X post -Is --cacert ${certificate} --netrc-file ${credentialFileREST} https://${remote.host}:${port}/jobhistory | head -n 1"
            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyYarnHistoryServerPamSSL")

        testResult
    }

    /**
     * Verify History Sever UI, REST Client Authentication with Insecure mode
     * @param packages
     * @param port
     * @return
     */
    def verifyYarnHistoryServerInsecure(List<Object> packages, int port) {

        log.trace("Start : EcoSystemYarn : verifyYarnHistoryServerInsecure")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME_YARN_HISTORY_SEVER, {
            def nodeResult = [:]

            final String query = "curl -X post -Is http://${remote.host}:${port}/jobhistory | head -n 1"
            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].toString().contains("HTTP/1.1 200 OK")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : EcoSystemYarn : verifyYarnHistoryServerInsecure")

        testResult
    }

}

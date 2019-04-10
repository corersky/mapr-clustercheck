package com.mapr.emea.ps.clustercheck.module.ecosystem.coreComponent

import com.mapr.emea.ps.clustercheck.module.ecosystem.util.MapRComponentHealthcheckUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component

@Component
class CoreMapRTool {

    static final Logger log = LoggerFactory.getLogger(CoreMapRTool.class)

    static final String PACKAGE_NAME = "mapr-core"

    @Autowired
    @Qualifier("localTmpDir")
    String tmpPath

    @Autowired
    MapRComponentHealthcheckUtil mapRComponentHealthcheckUtil

    /**
     * Verify MapR-Login tool with password (in a credential file and will be deleted after the test)
     * @param packages
     * @param username
     * @param password
     * @return
     */
    def verifyMapRLoginPassword(List<Object> packages, String username, String password, String credentialFileName) {
        log.trace("Start : CoreMapRTool : verifyMapRLoginPassword")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME, {
            def nodeResult = [:]

            final String credentialFilePath = "${tmpPath}/${credentialFileName}"
            executeSudo "rm -f ${credentialFilePath}"
            executeSudo "echo ${password} >> ${credentialFilePath}"
            executeSudo "chmod 400 ${credentialFilePath}"
            executeSudo "chown ${username} ${credentialFilePath}"


            def uid = executeSudo("su - ${username} -c 'id -u ${username}'")

            def ticketFile = "/tmp/maprticket_${uid}"

            nodeResult['output'] = executeSudo("su - ${username} -c 'echo \$(cat ${credentialFilePath}) | maprlogin password'")

            nodeResult['success'] = nodeResult['output'].contains(ticketFile)

            executeSudo "rm -f ${tmpPath}/${credentialFileName}"

            nodeResult
        })

        log.trace("End : CoreMapRTool : verifyMapRLoginPassword")

        testResult
    }

    /**
     * Verify MapR CLI Api with ticket
     * @param packages
     * @param ticketfile
     * @return
     */
    def verifyMapRCliApiSasl(List<Object> packages, String ticketfile) {
        log.trace("Start : CoreMapRTool : verifyMapRCliApiSasl")

        def testResult = mapRComponentHealthcheckUtil.executeSsh(packages, PACKAGE_NAME, {
            def nodeResult = [:]

            final String query = "MAPR_TICKETFILE_LOCATION=${ticketfile} maprcli service list; echo \$?"
            nodeResult['output'] = executeSudo query
            nodeResult['success'] = nodeResult['output'].contains("logpath") && nodeResult['output'].toString().reverse().take(1).equals("0")
            nodeResult['query'] = query

            nodeResult
        })

        log.trace("End : CoreMapRTool : verifyMapRCliApiSasl")

        testResult
    }
}

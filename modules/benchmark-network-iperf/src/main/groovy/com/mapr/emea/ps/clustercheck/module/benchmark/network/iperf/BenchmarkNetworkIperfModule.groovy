package com.mapr.emea.ps.clustercheck.module.benchmark.network.iperf

import com.mapr.emea.ps.clustercheck.core.ClusterCheckModule
import com.mapr.emea.ps.clustercheck.core.ClusterCheckResult
import com.mapr.emea.ps.clustercheck.core.ExecuteModule
import com.mapr.emea.ps.clustercheck.core.ModuleValidationException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.io.ResourceLoader

/**
 * Created by chufe on 22.08.17.
 */
@ClusterCheckModule(name = "benchmark-network-iperf", version = "1.0")
class BenchmarkNetworkIperfModule implements ExecuteModule {
    static final Logger log = LoggerFactory.getLogger(BenchmarkNetworkIperfModule.class);

    def defaultTestMatrix = [
            [threads: 1, dataPerThread: "4096M"],
            [threads: 2, dataPerThread: "2048M"],
            [threads: 4, dataPerThread: "1024M"],
            [threads: 8, dataPerThread: "512M"]
    ]

    @Autowired
    @Qualifier("ssh")
    def ssh
    @Autowired
    @Qualifier("globalYamlConfig")
    Map<String, ?> globalYamlConfig

    @Autowired
    ResourceLoader resourceLoader;

    @Override
    Map<String, ?> yamlModuleProperties() {
        return [tests:defaultTestMatrix]
    }

    @Override
    void validate() throws ModuleValidationException {
    }

    @Override
    ClusterCheckResult execute() {
        def clusteraudit = globalYamlConfig.modules['benchmark-network-iperf'] as Map<String, ?>
        def role = clusteraudit.getOrDefault("role", "all")
        checkIfIperfIsRunningAndStop(role)
        copyIperfToRemoteHost(role)
        startIperfServer(role)
        def result = runIperfTests(role)
        stopIperfServer(role)
        return new ClusterCheckResult(reportJson: result, reportText: "Not yet implemented", recommendations: ["Not yet implemented"])
    }

    def runIperfTests(role) {
        def moduleConfig = globalYamlConfig.modules['benchmark-network-iperf'] as Map<String, ?>
        def testMatrix = moduleConfig.getOrDefault("tests", defaultTestMatrix)

        def result = []
        // only one command executed with runInOrder
        log.info(">>>>> Run Iperf Tests ... this can take some time.")
        ssh.runInOrder {
            session(ssh.remotes.role(role)) {
                def currentHost = remote.host
                globalYamlConfig.nodes.each { node ->
                    def iperfTests = []
                    if (node.host != currentHost) {
                        testMatrix.each { matrixItem ->
                            //    log.info(">>>>> Run Iperf Server: ${node.host} - Client: ${currentHost} - Threads: ${matrixItem.threads} - Data per thread: ${matrixItem.dataPerThread}")
                            def iperfResult = execute "\$HOME/.clustercheck/iperf -c ${node.host} -n ${matrixItem.dataPerThread} -P ${matrixItem.threads} -y C"
                            def iperfTokens = iperfResult.tokenize(',')
                            def dataCopiedInBytes = Long.valueOf(iperfTokens[-2])
                            def throughputInBitPerSecond = Long.valueOf(iperfTokens[-1])
                            iperfTests << [dataPerThread: matrixItem.dataPerThread, threads: matrixItem.threads, dataCopiedInBytes: dataCopiedInBytes, throughputInBitsPerSecond: throughputInBitPerSecond]
                            sleep(1000)
                        }
                        result << [serverHost: node.host, clientHost: currentHost, tests: iperfTests]
                    }
                }
            }
        }
        return result
    }

    def checkIfIperfIsRunningAndStop(role) {
        log.info(">>>>> Check for running iperf instances")
        ssh.run {
            session(ssh.remotes.role(role)) {
                def pid = executeSudo "pidof iperf | xargs echo" // xargs echo get rid of return code
                if (pid) {
                    execute "kill -9 ${pid}"
                }
            }
        }
        sleep(2000)
    }

    def stopIperfServer(role) {
        log.info(">>>>> Stopping iperf server on all nodes")
        ssh.run {
            session(ssh.remotes.role(role)) {
                executeSudo "pidof iperf | xargs kill -9"
            }
        }
        log.info(">>>>> Wait for 5 seconds")
        sleep(5000)
    }

    def startIperfServer(role) {
        log.info(">>>>> Starting iperf server on all nodes")
        ssh.run {
            session(ssh.remotes.role(role)) {
                execute 'nohup $HOME/.clustercheck/iperf -s > /dev/null 2>&1 &'
            }

        }
        log.info(">>>>> Wait for 5 seconds")
        sleep(5000)
    }

    def copyIperfToRemoteHost(role) {
        log.info(">>>>> Copy iperf to remote host")
        ssh.run {
            session(ssh.remotes.role(role)) {
                def homePath = execute 'echo $HOME'
                execute "mkdir -p ${homePath}/.clustercheck"
                def iperfInputStream = resourceLoader.getResource("classpath:/com/mapr/emea/ps/clustercheck/module/benchmark/network/iperf/iperf").getInputStream()
                put from: iperfInputStream, into: "${homePath}/.clustercheck/iperf"
                execute "chmod +x ${homePath}/.clustercheck/iperf"
            }
        }
    }
}

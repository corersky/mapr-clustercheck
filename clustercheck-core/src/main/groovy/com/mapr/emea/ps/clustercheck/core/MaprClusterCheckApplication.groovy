package com.mapr.emea.ps.clustercheck.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import groovy.json.JsonOutput
import org.yaml.snakeyaml.Yaml

import java.text.SimpleDateFormat

@ComponentScan("com.mapr.emea.ps.clustercheck")
@SpringBootApplication
class MaprClusterCheckApplication implements CommandLineRunner {
    static final Logger log = LoggerFactory.getLogger(MaprClusterCheckApplication.class);

    @Autowired
    @Qualifier("globalYamlConfig")
    Map<String, ?> globalYamlConfig

    static String command;
    static String configFile;

    @Autowired
    ApplicationContext ctx;

    static void main(String[] args) {
        if(args.length != 2) {
            printHelpAndExit()
        }
        command = args[0]
        configFile = args[1]
        // TODO check commands
            // TODO check path

        SpringApplication.run MaprClusterCheckApplication, args
    }

    private static void printHelpAndExit() {
        println "USAGE: "
        println "  Run checks:                      ./clusterchecksuite run /path/to/myconfig.yaml"
        println "  Validate configuration file:     ./clusterchecksuite validate /path/to/myconfig.yaml"
        println "  Create configuration template:   ./clusterchecksuite generatetemplate /path/to/myconfig.yaml"
        println ""
        System.exit(1)
    }

    @Bean("globalYamlConfig")
    static Map<String, ?> globalYamlConfig() {
        Yaml parser = new Yaml()
        return parser.load((configFile as File).text) as Map<String, ?>
    }

    @Override
    void run(String... args) throws Exception {
        // TODO implement validate
        // TODO implement run
        // TODO implement template
        def modules = ctx.getBeansWithAnnotation(ClusterCheckModule)
        log.info("Number of modules found: " + modules.size())
        int countErrors = runValidation(modules)
        if(countErrors > 0) {
            log.error("Validation occured, please fix them and re-run.")
            return
        }

        def sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
        def outputDir = new File(globalYamlConfig.outputDir + "/" + sdf.format(new Date()))
        if(outputDir.exists()) {
            log.error("Output directory already exists " + outputDir.getAbsolutePath())
            return
        }
        log.info("... Creating output directory " + outputDir.getAbsolutePath())
        outputDir.mkdir()
        def startTime = System.currentTimeMillis()
        def moduleResults = runModuleExecution(modules, outputDir)
        def durationInMs = System.currentTimeMillis() - startTime
        // TODO
        log.info("... Writing summary JSON result ")
        writeGlobalJsonOutput(outputDir, moduleResults, startTime, durationInMs)
        log.info("... Writing summary TEXT report ")
        writeGlobalReportOutput(outputDir, moduleResults, startTime, durationInMs)
        log.info("... Execution completed")
    }

    List<ModuleInternalResult> runModuleExecution(Map<String, Object> modules, File outputDir) {
        def moduleResults = []
        log.info("====== Starting execution =======")
        for (Object module : modules.values()) {
            if (module instanceof ExecuteModule) {
                ExecuteModule m = (ExecuteModule) module
                def annotation = m.getClass().getAnnotation(ClusterCheckModule)
                log.info("Executing " + annotation.name() + " - " + annotation.version())
                long moduleStart = System.currentTimeMillis()
                def result = m.execute()
                long moduleDurationInMs = System.currentTimeMillis() - moduleStart
                def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                def internalResult = new ModuleInternalResult(result: result, module: annotation, moduleDurationInMs: moduleDurationInMs, executedAt: sdf.format(new Date()))
                writeModuleJsonOutput(outputDir, internalResult)
                writeModuleReportOutput(outputDir, internalResult)
                moduleResults << internalResult
            } else {
                log.warn("Cannot execute module")
            }

        }
        log.info("====== Execution finished ======")
        return moduleResults
    }

    int runValidation(Map<String, Object> modules) {
        log.info("====== Starting validation ======")
        int countErrors = 0
        for (Object module : modules.values()) {
            if (module instanceof ExecuteModule) {
                ExecuteModule m = (ExecuteModule) module
                def annotation = m.getClass().getAnnotation(ClusterCheckModule)
                log.info("Validating " + annotation.name() + " - " + annotation.version())
                try {
                    module.validate()
                }
                catch (ModuleValidationException ex) {
                    log.error("   " + ex.getMessage())
                    countErrors++;
                }
            } else {
                log.warn("Cannot validate module")
            }

        }
        log.info("====== Validation finished ======")
        return countErrors
    }

    def writeModuleJsonOutput(File outputDir, ModuleInternalResult result) {
        def globalJson = [clusterName: globalYamlConfig.cluster_name, customerName: globalYamlConfig.customer_name, executedAt: result.executedAt, executionDurationInMs: result.moduleDurationInMs, executionHost: InetAddress.getLocalHost().getCanonicalHostName(), moduleName: result.module.name(), moduleVersion: result.module.version(), moduleResult: result.result.reportJson, moduleRecommendations: result.result.recommendations, configuration: globalYamlConfig]
        def json = JsonOutput.toJson(globalJson)
        def outputModuleDir = new File(outputDir.getAbsolutePath() + "/modules/" + result.module.name())
        outputModuleDir.mkdirs()
        def outputFile = new File(outputModuleDir.getAbsolutePath() + "/result.json")
        outputFile.text = JsonOutput.prettyPrint(json)
    }

    def writeModuleReportOutput(File outputDir, ModuleInternalResult result) {
        def executionHost = InetAddress.getLocalHost().getCanonicalHostName()
        def outputHeader = """========================= REPORT INFO ====================================
Customer: ${globalYamlConfig.customer_name}
Cluster name: ${globalYamlConfig.cluster_name}
Executed at: ${result.executedAt}
Execution duration in ms: ${result.moduleDurationInMs}
Executed on host: ${executionHost}
Module name: ${result.module.name()}
Module version: ${result.module.version()}
========================= MODULE REPORT ==================================
"""
def recommendationHeader = """

========================= MODULE RECOMMENDATIONS =========================
"""
        def outputText = outputHeader + result.result.reportText + recommendationHeader
        int i = 1
        for (String recommendation : result.result.recommendations) {
            outputText += ">>>>> Recommendation ${i} <<<<<\n"
            outputText += recommendation + "\n"
            i++
        }
        outputText += """
========================= CONFIGURATION ==================================
"""
        outputText += (configFile as File).text
        def outputModuleDir = new File(outputDir.getAbsolutePath() + "/modules/" + result.module.name())
        outputModuleDir.mkdirs()
        def outputFile = new File(outputModuleDir.getAbsolutePath() + "/report.txt")
        outputFile.text = outputText
    }

    def writeGlobalJsonOutput(File outputDir, List<ModuleInternalResult> moduleInternalResults, long startTime, long durationInMs) {
        def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        def moduleResults = moduleInternalResults.collect { [moduleName: it.module.name(), moduleVersion: it.module.version(), moduleExecutedAt: it.executedAt, moduleDurationInMs: it.moduleDurationInMs, result: it.result.reportJson, recommendations: it.result.recommendations, ]}
        def globalJson = [clusterName: globalYamlConfig.cluster_name, customerName: globalYamlConfig.customer_name, executedAt: sdf.format(new Date(startTime)), executionDurationInMs: durationInMs, executionHost: InetAddress.getLocalHost().getCanonicalHostName(), moduleResults: moduleResults, configuration: globalYamlConfig]
        def json = JsonOutput.toJson(globalJson)
        def outputFile = new File(outputDir.getAbsolutePath() + "/result.json")
        outputFile.text = JsonOutput.prettyPrint(json)
    }


    def writeGlobalReportOutput(File outputDir, List<ModuleInternalResult> moduleInternalResults, long startTime, long durationInMs) {
        def executionHost = InetAddress.getLocalHost().getCanonicalHostName()
        def sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        def executedAtFormated = sdf.format(new Date(startTime))
        def outputText = """========================= REPORT INFO ====================================
Customer: ${globalYamlConfig.customer_name}
Cluster name: ${globalYamlConfig.cluster_name}
Executed at: ${executedAtFormated}
Execution duration in ms: ${durationInMs}
Executed on host: ${executionHost}

"""
        for (ModuleInternalResult internalResult : moduleInternalResults) {
            outputText += """========================= MODULE ${internalResult.module.name()} ==================================
Module name: ${internalResult.module.name()}
Module version: ${internalResult.module.version()}
Module executed at: ${internalResult.executedAt}
Module execution duration in ms: ${internalResult.moduleDurationInMs}
------------------------- MODULE REPORT ----------------------------------------
"""
            outputText += internalResult.result.reportText
            outputText += """

------------------------- MODULE RECOMMENDATIONS -------------------------------
"""
            int countRec = 1
            for (String recommendation : internalResult.result.recommendations) {
                outputText += ">>>>> Recommendation ${countRec} <<<<<\n"
                outputText += recommendation + "\n"
                countRec++
            }
        }
        outputText += """

========================= CONFIGURATION ==================================
"""
        outputText += (configFile as File).text
        def outputFile = new File(outputDir.getAbsolutePath() + "/report.txt")
        outputFile.text = outputText
    }
}
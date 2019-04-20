TODO

# MapR Ecosystem Components HealthCheck

## PASSWORD AUTH

* Global parameters
    - username & password
        + Used in most PAM/Plain authentication, credential files will be created locally, sent to remote hosts, and will be purged (locally/remotely) after each test, so that password will not be exposed
    - MapR user ticketfile
        + The default value is /opt/mapr/conf/mapruserticket
    - SSL certificate file
        + The default value is /opt/mapr/conf/ssl_truststore.pem
        + In each REST API check, enable/disable the certificate verification can be configured.

## Components Verification

* Hive
    - hive-beeline-pam-ssl
        + This check reqires ssl truststore (/opt/mapr/conf/ssl_truststore) and hive configuration (<https://mapr.com/docs/home/Hive/HiveServer2-ConnectWithBeelineOrJDBC.html>).

## Purge

* Local tmp files (from tool execution host)
    - Remote temporary files will be kept after checks, the defualt location is /tmp/.clustercheck on tool execution host.

* Local tmp files (from target hosts)
    - Local temporary files will be kept after checks, the defualt location is /tmp/.clustercheck on each host.

* MapR-FS tmp files
    - By default temporary files(Files/Stream/DB) on MapR-FS will be purged after checks, but this is configurable for each related component, the defualt location is maprfs://tmp/.clustercheck

## Output

* Output doesn't only provide the check results but also provide the check queries/steps for users.
# Java.ps1 - Java SDK Generation Plugins

# Dot-source core if not already present (assuming it's in the same folder)
. $PSScriptRoot\Core.ps1

function Write-Pom {
    param(
        [Parameter(Mandatory=$true)][string]$OutputDir,
        [Parameter(Mandatory=$true)][string]$ArtifactId,
        [Parameter(Mandatory=$true)][string]$Description,
        [string]$GroupId = "com.am.portfolio",
        [string]$Version = "1.0.0"
    )

    $pomPath = Join-Path $OutputDir "pom.xml"
    $pomContent = @"
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>$GroupId</groupId>
    <artifactId>$ArtifactId</artifactId>
    <packaging>jar</packaging>
    <name>$ArtifactId</name>
    <version>$Version</version>
    <description>$Description</description>
    <distributionManagement>
        <repository>
            <id>github-investment</id>
            <name>GitHub Packages</name>
            <url>https://maven.pkg.github.com/AM-Portfolio/am-market</url>
        </repository>
    </distributionManagement>
    <build>
        <plugins>
            <plugin>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.10.1</version>
                <configuration>
                    <source>11</source>
                    <target>11</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <jackson-version>2.19.2</jackson-version>
        <jackson-databind-nullable-version>0.2.8</jackson-databind-nullable-version>
        <jakarta-annotation-version>1.3.5</jakarta-annotation-version>
        <junit-version>5.10.2</junit-version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-core</artifactId>
            <version>`${jackson-version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-annotations</artifactId>
            <version>`${jackson-version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>`${jackson-version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
            <version>`${jackson-version}</version>
        </dependency>
        <dependency>
            <groupId>org.openapitools</groupId>
            <artifactId>jackson-databind-nullable</artifactId>
            <version>`${jackson-databind-nullable-version}</version>
        </dependency>
        <dependency>
            <groupId>com.google.code.findbugs</groupId>
            <artifactId>jsr305</artifactId>
            <version>3.0.2</version>
        </dependency>
        <dependency>
            <groupId>jakarta.annotation</groupId>
            <artifactId>jakarta.annotation-api</artifactId>
            <version>`${jakarta-annotation-version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpclient</artifactId>
            <version>4.5.14</version>
        </dependency>
        <dependency>
            <groupId>org.apache.httpcomponents</groupId>
            <artifactId>httpmime</artifactId>
            <version>4.5.14</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>2.0.12</version>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <version>`${junit-version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
"@
    Set-Content -Path $pomPath -Value $pomContent
}

function Invoke-JavaGen {
    param(
        [Parameter(Mandatory=$true)][string]$Spec,
        [Parameter(Mandatory=$true)][string]$OutDir,
        [Parameter(Mandatory=$true)][string]$ArtifactId,
        [Parameter(Mandatory=$true)][string]$Description,
        [hashtable]$BaseConfig = @{ library = "native" },
        [string]$ApiPackage = "com.am.portfolio.client.api",
        [string]$ModelPackage = "com.am.portfolio.client.model",
        [string]$InvokerPackage = "com.am.portfolio.client.invoker",
        [string]$Label = "java-sdk"
    )

    $fullConfig = $BaseConfig.Clone()
    $fullConfig["groupId"] = "com.am.portfolio"
    $fullConfig["artifactId"] = $ArtifactId
    $fullConfig["apiPackage"] = $ApiPackage
    $fullConfig["modelPackage"] = $ModelPackage
    $fullConfig["invokerPackage"] = $InvokerPackage

    # Call core generator
    Invoke-OpenApiGen -Spec $Spec -OutDir $OutDir -Generator "java" -Config $fullConfig -Label $Label

    # Write pom.xml
    Write-Pom -OutputDir $OutDir -ArtifactId $ArtifactId -Description $Description
}

# End of Java.ps1


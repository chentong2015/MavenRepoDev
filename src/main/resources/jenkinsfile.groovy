#!/usr/bin/env groovy

library 'pipeline-utils' //invokeMaven, sonarAnalysis

pipeline {
    // 配置Agent Server
    agent {
        kubernetes {
            yamlFile '.ci/kubernetes/template-primary.yml'
            defaultContainer 'primary'
            customWorkspace '/src/new/version'
        }
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(daysToKeepStr: '7'))
    }

    stages {
        stage("Build and Publish") {
            steps {
                // 执行Maven打包指令: 基于指定项目的pom，提供执行的Goals，-gs配置文件，激活profile
                // global-settings.xml配置依赖下载的远程仓库
                invokeMaven(
                   pom        : 'component/database/datamodel-tool/pom.xml',
                   goals      : 'clean package -DskipTests',
                   settings   : 'component/global-settings.xml',
                   profiles   : 'mx-artifacts'
                )

                // 执行Maven deploy插件的deploy-file功能
                // 根据settings.xml文件中设置的Repo来部署
                invokeMaven(
                   goals      : 'deploy:deploy-file',
                   settings   : 'das/das2-settings.xml',
                   properties : [
                       'groupId=ctong',
                       'artifactId=datamodel-tool',
                       'version=v3.1.build-SNAPSHOT',
                       'packaging=zip',
                       'file=target/datamodel-tool.zip', // 指定上传的文件为压缩的zip包
                       'url=http://nexus/nexus/content/repositories/ctong-components-snapshots',
                       'repositoryId=ctong-components-snapshots',
                       'generatePom=true',
                       'deployer-das.username=${DEPLOYER_DATA_USERNAME}',
                       'deployer-das.password=${DEPLOYER_DATA_PASSWORD}',
                   ]
                )
            }
        }
    }
}
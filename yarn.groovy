@NonCPS
def createOcParams(def params) {
    String paramsStr = ''
    params.each { key, val ->
        paramsStr += "--param=${key}=\"${val}\" "
    }
    return paramsStr
}

def serviceName
def serviceVersion
def applicationImageProject

pipeline {
    agent {
        label 'yarn'
    }

    stages {
        stage('Setting Up') {
            when { expression { return jobOption == 'Build and Deploy' || jobOption == 'Build Only' }}
            steps {
                script {
                    applicationImageProject = "cgc-cicd"

                    WORKSPACE = "/tmp/${JOB_NAME}/${BUILD_NUMBER}"

                    sh '''
                    #!/bin/bash

                    set +x
                    mkdir -p ${WORKSPACE}/configurations
                    mkdir -p ${WORKSPACE}/deployconfigs
                    mkdir -p ${WORKSPACE}/deploy
                    mkdir -p ${WORKSPACE}/source

                    '''
                    
                    // sh "mkdir -p ${WORKSPACE}/configurations"
                    // sh "mkdir -p ${WORKSPACE}/deployconfigs"
                    // sh "mkdir -p ${WORKSPACE}/deploy"
                    // sh "mkdir -p ${WORKSPACE}/source"

                    if (ref.contains('refs/heads/')) {
                        ref = ref.replace('refs/heads/', '')
                    }

                    dir ("${WORKSPACE}/source") {
                        checkout scm: [$class: 'GitSCM',
                            userRemoteConfigs: [[url: "${repo}"]],
                            branches: [[name: "${ref}"]],
                            extensions: [
                                [$class: 'CloneOption', noTags: true, shallow: true]
                            ]
                        ]
                    }

                    /*dir ("${WORKSPACE}/configurations") {
                        checkout scm: [$class: 'GitSCM',
                            userRemoteConfigs: [[url: "${env.APP_CONFIG_DEV_URL}", credentialsId: 'gitlab-login']],
                            branches: [[name: "main"]],
                            extensions: [
                                [$class: 'CloneOption', noTags: true, shallow: true]
                            ]
                        ]
                    }

                    dir ("${WORKSPACE}/deployconfigs") {
                        checkout scm: [$class: 'GitSCM',
                            userRemoteConfigs: [[url: "${env.APP_CONFIG_URL}", credentialsId: 'gitlab-login']],
                            branches: [[name: "main"]],
                            extensions: [
                                [$class: 'CloneOption', noTags: true, shallow: true]
                            ]
                        ]
                    } */
                }
            }
        }



        stage('Build') {
            when { expression { return jobOption == 'Build and Deploy' || jobOption == 'Build Only' }}
            steps {
                script {
                    dir("${WORKSPACE}/source") {
                        withCredentials([usernamePassword(credentialsId: 'nexus-login', passwordVariable: 'N_PASS', usernameVariable: 'N_USER')]) {

                            println "service name --> " + serviceName

                            
                            //OLD_BUILD_NUMBER = BUILD_NUMBER.toInteger() - 1
                            //oldServiceVersion = "${serviceVersion}-${OLD_BUILD_NUMBER}"

                            //serviceVersion = "${serviceVersion}-${BUILD_NUMBER}"
                            serviceVersion = "${BUILD_NUMBER}"
                            println 'serviceVersion -->' + serviceVersion

                            currentBuild.displayName = "#${BUILD_NUMBER}"

                            sh '''
                                #!/bin/bash

                                npm config set registry http://nexus.cicd.svc.cluster.local:8081/nexus/content/groups/npm-group
                                echo "_auth=''' + auth + '''" >> ~/.npmrc
								npm config set prefix "/home/jenkins"
                                npm config set strict-ssl false
                                npm config set strict-ssl true
								
                                yarn config set npmRegistryServer http://nexus.cicd.svc.cluster.local:8081/repository/npm-group/
                                yarn config set unsafeHttpWhitelist nexus.cicd.svc.cluster.local:8081
                                yarn config set npmAlwaysAuth true
                                yarn config set npmAuthIdent $N_USER:$N_PASSS
                            '''

                             sh '''
                                #!/bin/bash
        
                                set +x
                                
                                export PATH=~/bin:$PATH
                                
                                yarn workspaces focus
                                yarn install
                            '''
                        }
                    }
                }
            }
        }
        //TODO: Publish to nexus registry (npm-release-dev hosted repo).
        //TODO: Sonarqube code coverage.
        //TODO: Deploy to webserver (via SSH)




    /*
    post {
        success {
            mail body: "${serviceName}:${serviceVersion} has been built and deployed on DEV successfully.\n\n$BUILD_URL", charset: 'UTF-8', mimeType: 'text/html',
                        subject: "[CI/CD] DEV Build - ${serviceName}:${serviceVersion}", from: "svc_cicdjenkins--do-not-reply@cgc.com.my", to: "${env.NOTIFICATION_TO_ADDRESS}"
        }

        failure {
            mail body: "Failed to build and deploy ${serviceName}:${serviceVersion} on DEV.\n\nPlease check the status from $BUILD_URL", charset: 'UTF-8', mimeType: 'text/html',
                        subject: "[CI/CD] DEV Build - ${serviceName}:${serviceVersion}", from: "svc_cicdjenkins--do-not-reply@cgc.com.my", to: "${env.NOTIFICATION_TO_ADDRESS}"
        }

        // Clean after build
        always {
            script {
                echo "Finished!!"
            }
        }
    } */
}
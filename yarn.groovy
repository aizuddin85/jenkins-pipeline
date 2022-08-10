
def serviceName
def serviceVersion
def applicationImageProject

pipeline {
    agent {
        label 'yarn'
    }

    stages {
        stage('Setting Up') {
            steps {
                script {
                    applicationImageProject = "cicd"

                    WORKSPACE = "/tmp/${JOB_NAME}/${BUILD_NUMBER}"

                    sh '''
                    #!/bin/bash

                    set +x
                    mkdir -pv ${WORKSPACE}/configurations
                    mkdir -pv ${WORKSPACE}/deployconfigs
                    mkdir -pv ${WORKSPACE}/deploy
                    mkdir -pv ${WORKSPACE}/source

                    '''

                    dir ("${WORKSPACE}/source") {
                        checkout scm: [$class: 'GitSCM',
                            userRemoteConfigs: [[url: "${repo}"]],
                            branches: [[name: "${ref}"]],
                            extensions: [
                                [$class: 'CloneOption', noTags: true, shallow: true]
                            ]
                        ]
                    }
                }
            }
        }



        stage('Build') {
            steps {
                script {
                    dir("${WORKSPACE}/source") {
                        withCredentials([usernamePassword(credentialsId: 'nexus-login', passwordVariable: 'N_PASS', usernameVariable: 'N_USER')]) {

                            println "service name --> " + serviceName
                           
                            
                             
                            //OLD_BUILD_NUMBER = BUILD_NUMBER.toInteger() - 1
                            //oldServiceVersion = "${serviceVersion}-${OLD_BUILD_NUMBER}"

                            //serviceVersion = "${serviceVersion}-${BUILD_NUMBER}"
                            serviceVersion = "${BUILD_NUMBER}"
                            auth = "YWRtaW46YWRtaW4xMjM="
                            println 'serviceVersion -->' + serviceVersion

                            currentBuild.displayName = "#${BUILD_NUMBER}"

                            sh '''
                                #!/bin/bash

                                rm -rvf yarn.lock
                                echo Read version from package.json
                                node --eval="process.stdout.write(require('./package.json').version)"
                            '''

                             sh '''
                                #!/bin/bash
        
                                set +x
                                pwd
                                ls -lart ../
                                export PATH=~/bin:$PATH
                                echo yarnrc content
                                cat /home/jenkins/.yarnrc
                                echo home npmrc content
                                cat /home/jenkins/.npmrc
                                echo usr npmrc content
                                cat /usr/etc/npmrc

                                yarn install --verbose
                                yarn publish --verbose

                            '''
                        }
                    }
                }
            }
        }
    }
}

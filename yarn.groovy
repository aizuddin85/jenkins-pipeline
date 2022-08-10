
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

                                npm config set registry http://nexus.cicd.svc.cluster.local:8081/nexus/content/groups/npm-group/
                                echo "_auth=''' + auth + '''" >> ~/.npmrc
								npm config set prefix "/home/jenkins"
                                npm config set strict-ssl false
                                npm config set strict-ssl true
								
                                yarn config set registry http://nexus.cicd.svc.cluster.local:8081/nexus/content/groups/npm-group/
                                yarn config set unsafeHttpWhitelist nexus.cicd.svc.cluster.local:8081
                                yarn config set npmAlwaysAuth true
                                yarn config set npmAuthIdent $N_USER:$N_PASS
                            '''

                             sh '''
                                #!/bin/bash
        
                                set +x
                                
                                export PATH=~/bin:$PATH
                                cat /home/jenkins/.yarnrc
                                cat /home/jenkins/.npmrc
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

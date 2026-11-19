@Library('ivolve-shared-library') _

// One pipeline for the whole project: it builds the 3 microservices one after the other.
// Jenkins only builds and pushes images, then updates the image tags in Git. ArgoCD (on EKS) does the deployment.
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

   

    environment {
        AWS_REGION      = 'us-east-1'
        REGISTRY        = '908124814208.dkr.ecr.us-east-1.amazonaws.com'
        REPOSITORY      = 'ivolve-app'
        GIT_REPO_URL    = 'https://github.com/ossmaahmed/CloudDevOpsProject.git'
        GIT_BRANCH_NAME = 'main'
        GIT_CREDENTIALS = 'github-creds'
    }

    stages {
        // Runs on a manual build, or when polling sees changes under app/.
        // The manifest commit made by this pipeline only touches kubernetes/, so it cannot start a build loop.
        stage('Pipeline') {
            when {
                anyOf {
                    changeset 'app/**'
                    not { triggeredBy 'SCMTrigger' }
                }
            }
            stages {
                stage('Build App') {
                    stages {
                        stage('Unit Test') {
                            steps {
                                script {
                                    ['auth-service', 'frontend', 'roadmap-service'].each { svc ->
                                        unitTest(dir: "app/${svc}")
                                    }
                                }
                            }
                        }
                        stage('Build App') {
                            steps {
                                script {
                                    ['auth-service', 'frontend', 'roadmap-service'].each { svc ->
                                        buildApp(dir: "app/${svc}")
                                    }
                                }
                            }
                        }
                    }
                }

                stage('Build Image') {
                    stages {
                        stage('Build Image') {
                            steps {
                                script {
                                    ['auth-service', 'frontend', 'roadmap-service'].each { svc ->
                                        buildImage(image: "${REGISTRY}/${REPOSITORY}:${svc}-${BUILD_NUMBER}", context: "app/${svc}")
                                    }
                                }
                            }
                        }
                        stage('Scan Image') {
                            steps {
                                script {
                                    ['auth-service', 'frontend', 'roadmap-service'].each { svc ->
                                        scanImage(image: "${REGISTRY}/${REPOSITORY}:${svc}-${BUILD_NUMBER}", failOnCritical: false)
                                    }
                                }
                            }
                        }
                        stage('Push Image') {
                            steps {
                                script {
                                    ['auth-service', 'frontend', 'roadmap-service'].each { svc ->
                                        retry(3) {
                                            pushImage(image: "${REGISTRY}/${REPOSITORY}:${svc}-${BUILD_NUMBER}",
                                                      registry: env.REGISTRY, region: env.AWS_REGION)
                                        }
                                    }
                                }
                            }
                        }
                        stage('Delete Image Locally') {
                            steps {
                                script {
                                    ['auth-service', 'frontend', 'roadmap-service'].each { svc ->
                                        deleteImage(image: "${REGISTRY}/${REPOSITORY}:${svc}-${BUILD_NUMBER}")
                                    }
                                }
                            }
                        }
                    }
                }

                stage('Update Manifests') {
                    stages {
                        stage('Update Image Tags') {
                            steps {
                                script {
                                    ['auth-service', 'frontend', 'roadmap-service'].each { svc ->
                                        updateManifests(file: "kubernetes/${svc}/deployment.yaml", service: svc, tag: env.BUILD_NUMBER)
                                    }
                                }
                            }
                        }
                        stage('Push Manifests') {
                            steps {
                                pushManifests(file: 'kubernetes/auth-service/deployment.yaml kubernetes/frontend/deployment.yaml kubernetes/roadmap-service/deployment.yaml',
                                              message: "ci: update images to build ${env.BUILD_NUMBER}",
                                              repoUrl: env.GIT_REPO_URL,
                                              branch: env.GIT_BRANCH_NAME,
                                              credentialsId: env.GIT_CREDENTIALS)
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            sh 'docker image prune -f || true'
        }
    }
}

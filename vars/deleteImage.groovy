// Delete Image Locally stage: free disk space on the Jenkins server.
def call(Map cfg = [:]) {
    sh "docker rmi -f ${cfg.image} || true"
    sh 'docker image prune -f || true'
}

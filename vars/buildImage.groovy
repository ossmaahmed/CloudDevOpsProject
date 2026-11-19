// Build Image stage: docker build of one microservice.
// Usage: buildImage(image: 'registry/repo:tag', context: 'app/auth-service')
def call(Map cfg = [:]) {
    echo "Building ${cfg.image} from ${cfg.context}"
    sh "docker build -t ${cfg.image} ${cfg.context}"
}

// Update Manifests stage: point the Deployment at the freshly pushed image tag.
// Usage: updateManifests(file: 'kubernetes/auth-service/deployment.yaml', service: 'auth-service', tag: env.BUILD_NUMBER)
def call(Map cfg = [:]) {
    withEnv(["MANIFEST=${cfg.file}", "SERVICE=${cfg.service}", "NEW_TAG=${cfg.tag}"]) {
        sh '''
            sed -i -E "s|(image: .*/ivolve-app:)${SERVICE}(-[0-9]+)?[[:space:]]*$|\\1${SERVICE}-${NEW_TAG}|" "$MANIFEST"
            grep -n "image:" "$MANIFEST"
            grep -q "ivolve-app:${SERVICE}-${NEW_TAG}" "$MANIFEST"
        '''
    }
}

// Scan Image stage: Trivy scan of the local image.
// The HIGH/CRITICAL report is always archived. Set failOnCritical: true to fail the build on CRITICAL findings.
def call(Map cfg = [:]) {
    sh "trivy image --no-progress --severity HIGH,CRITICAL --ignore-unfixed --output trivy-report.txt ${cfg.image}"
    sh 'cat trivy-report.txt || true'
    archiveArtifacts artifacts: 'trivy-report.txt', allowEmptyArchive: true
    if (cfg.failOnCritical) {
        sh "trivy image --no-progress --severity CRITICAL --ignore-unfixed --exit-code 1 ${cfg.image}"
    }
}

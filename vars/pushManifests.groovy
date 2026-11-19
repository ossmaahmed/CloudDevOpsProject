// Push Manifests stage: commit the updated manifest(s) and push to GitHub (ArgoCD then deploys). file may hold several space-separated paths.
// Rebases and retries because the three service pipelines push to the same branch.
def call(Map cfg = [:]) {
    withCredentials([usernamePassword(credentialsId: cfg.credentialsId,
                                      usernameVariable: 'GIT_USER',
                                      passwordVariable: 'GIT_TOKEN')]) {
        withEnv(["MANIFEST=${cfg.file}", "COMMIT_MSG=${cfg.message}", "REPO_URL=${cfg.repoUrl}", "BRANCH=${cfg.branch}"]) {
            sh '''
                git config user.name "jenkins-ci"
                git config user.email "jenkins@ivolve.local"
                git add $MANIFEST
                if git diff --cached --quiet; then
                    echo "No manifest changes to push"
                    exit 0
                fi
                git commit -m "$COMMIT_MSG"
                REMOTE="https://${GIT_USER}:${GIT_TOKEN}@${REPO_URL#https://}"
                for i in 1 2 3 4 5; do
                    git fetch "$REMOTE" "$BRANCH"
                    git rebase FETCH_HEAD
                    if git push "$REMOTE" HEAD:"$BRANCH"; then
                        exit 0
                    fi
                    sleep $((i * 3))
                done
                exit 1
            '''
        }
    }
}

// Unit Test stage: auto-detects the project type and runs the tests inside a Docker container
// (the Jenkins server only needs Docker, no Maven/Node/Python installed).
// Usage: unitTest(dir: 'app/auth-service')
def call(Map cfg = [:]) {
    withEnv(["APP_DIR=${cfg.dir}"]) {
        sh '''
            set -e
            mkdir -p "$HOME/.cache-m2"
            RUN="docker run --rm -v $WORKSPACE/$APP_DIR:/work -w /work --user $(id -u):$(id -g) -e HOME=/tmp"
            if [ -f "$APP_DIR/pom.xml" ]; then
                echo ">> Maven project: running unit tests"
                $RUN -v "$HOME/.cache-m2:/tmp/.m2" maven:3.9.9-eclipse-temurin-21 \
                     mvn -B -q -Dmaven.repo.local=/tmp/.m2 test
            elif [ -f "$APP_DIR/package.json" ]; then
                echo ">> Node project: running unit tests"
                $RUN node:22-bookworm-slim sh -c "(npm ci || npm install) && npm test --if-present"
            elif [ -f "$APP_DIR/requirements.txt" ]; then
                echo ">> Python project: running unit tests"
                $RUN python:3.12-slim sh -c "python -m pip install --user -q -r requirements.txt && \
                    if ls test_*.py *_test.py tests 2>/dev/null | grep -q .; then python -m pip install --user -q pytest && python -m pytest -q; else echo 'No tests found, skipping'; fi"
            else
                echo "Unknown project type in $APP_DIR, skipping unit tests"
            fi
        '''
    }
}

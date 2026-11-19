// Build App stage: compile/package the application (auto-detects Maven / Node / Python).
// Usage: buildApp(dir: 'app/auth-service')
def call(Map cfg = [:]) {
    withEnv(["APP_DIR=${cfg.dir}"]) {
        sh '''
            set -e
            mkdir -p "$HOME/.cache-m2"
            RUN="docker run --rm -v $WORKSPACE/$APP_DIR:/work -w /work --user $(id -u):$(id -g) -e HOME=/tmp"
            if [ -f "$APP_DIR/pom.xml" ]; then
                echo ">> Maven project: packaging"
                $RUN -v "$HOME/.cache-m2:/tmp/.m2" maven:3.9.9-eclipse-temurin-21 \
                     mvn -B -q -Dmaven.repo.local=/tmp/.m2 -DskipTests package
                ls -l "$APP_DIR"/target/*.jar
            elif [ -f "$APP_DIR/package.json" ]; then
                echo ">> Node project: install and build"
                $RUN node:22-bookworm-slim sh -c "(npm ci || npm install) && npm run build --if-present"
            elif [ -f "$APP_DIR/requirements.txt" ]; then
                echo ">> Python project: syntax check"
                $RUN python:3.12-slim python -m compileall -q .
            else
                echo "Unknown project type in $APP_DIR, nothing to build"
            fi
        '''
    }
}

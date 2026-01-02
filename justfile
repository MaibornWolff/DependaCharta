default:
    @just --list

test:
    cd analysis && ./gradlew test

test-frontend:
    cd visualization && npm run test -- --no-watch --browsers=ChromeHeadless

ktlintformat:
    cd analysis && ./gradlew ktlintFormat

build:
    cd analysis && ./gradlew fatJar

run DIR: build
    cd analysis && java -jar build/libs/dependacharta.jar -d {{absolute_path(DIR)}}

frontend:
    cd visualization && npm ci && npm run start

analyze DIR: build
    cd analysis && java -jar build/libs/dependacharta.jar -d {{absolute_path(DIR)}} -o ../visualization/public/analysis -f analyzed-project
    @echo "Analysis complete! Starting frontend..."
    @echo "Frontend will automatically load your analysis."
    @echo "You can also manually specify a file: http://localhost:4200?file=./path/to/file.cg.json"

docker-build:
    cd analysis && docker build -t dependacharta-analysis .

docker-build-multi:
    cd analysis && docker buildx build --platform linux/amd64,linux/arm64 -t dependacharta-analysis .

docker-run DIR:
    docker run --rm -v {{DIR}}:/workspace dependacharta-analysis -d /workspace

docker-analyze DIR: docker-build
    docker run --rm -v {{DIR}}:/workspace -v ./visualization/public/analysis:/output dependacharta-analysis -d /workspace -o /output -f analyzed-project
    @echo "Docker analysis complete! Starting frontend..."
    @echo "Frontend will automatically load your analysis."
    @echo "You can also manually specify a file: http://localhost:4200?file=./path/to/file.cg.json"

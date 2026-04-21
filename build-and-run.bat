@echo off
setlocal

cd /d "%~dp0"

if not exist ".m2\repository" (
    mkdir ".m2\repository"
)

if "%SPRING_DATASOURCE_URL%"=="" set "SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/vendor_rag_db"
if "%SPRING_DATASOURCE_USERNAME%"=="" set "SPRING_DATASOURCE_USERNAME=postgres"
if "%SPRING_DATASOURCE_PASSWORD%"=="" set "SPRING_DATASOURCE_PASSWORD=admin"

if "%PINECONE_INDEX_NAME%"=="" set "PINECONE_INDEX_NAME=vendor-rag-clean"
if "%PINECONE_NAMESPACE%"=="" set "PINECONE_NAMESPACE=vendor-docs"
if "%PINECONE_CLOUD%"=="" set "PINECONE_CLOUD=aws"
if "%PINECONE_REGION%"=="" set "PINECONE_REGION=us-east-1"

if "%OLLAMA_BASE_URL%"=="" set "OLLAMA_BASE_URL=http://localhost:11434"
if "%OLLAMA_GENERATION_MODEL%"=="" set "OLLAMA_GENERATION_MODEL=qwen:0.5b"
if "%OLLAMA_MODEL%"=="" set "OLLAMA_MODEL=qwen:0.5b"
if "%OLLAMA_EMBEDDING_MODEL%"=="" set "OLLAMA_EMBEDDING_MODEL=nomic-embed-text"
if "%EMBEDDING_DIM%"=="" set "EMBEDDING_DIM=768"
if "%PORT%"=="" set "PORT=8080"

echo Building runnable JAR...
call mvn -Dmaven.repo.local=.m2\repository clean package
if errorlevel 1 (
    echo.
    echo Build failed.
    pause
    exit /b 1
)

set "JAR_FILE="
for %%f in (target\*.jar) do (
    if /I not "%%~nxf"=="original-%%~nxf" (
        set "JAR_FILE=%%f"
    )
)

if "%JAR_FILE%"=="" (
    echo.
    echo Could not find built JAR in target\.
    pause
    exit /b 1
)

if "%PINECONE_API_KEY%"=="" (
    echo.
    echo Warning: PINECONE_API_KEY is not set.
    echo Set it before running if you want ingestion/query to work.
)

echo.
echo Starting application: %JAR_FILE%
echo.
java -jar "%JAR_FILE%"

endlocal

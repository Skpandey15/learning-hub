[CmdletBinding()]
param(
    [string] $Context = 'k3d-dev',
    [string] $Namespace = 'learning-hub-development',
    [string] $OpenAiSecretNamespace = 'online-interview-dev',
    [string] $OpenAiSecretName = 'platform-secrets'
)

$ErrorActionPreference = 'Stop'
if ((kubectl config current-context) -ne $Context) {
    throw "Refusing to bootstrap unexpected Kubernetes context. Expected $Context."
}

function New-RandomToken([int] $Bytes = 32) {
    $buffer = New-Object byte[] $Bytes
    $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $generator.GetBytes($buffer) } finally { $generator.Dispose() }
    [Convert]::ToBase64String($buffer)
}

kubectl create namespace $Namespace --dry-run=client -o yaml | kubectl apply -f -

if (-not (kubectl get secret learning-hub-runtime -n $Namespace --ignore-not-found -o name)) {
    $encodedOpenAiKey = kubectl get secret $OpenAiSecretName -n $OpenAiSecretNamespace -o jsonpath='{.data.OPENAI_API_KEY}'
    if (-not $encodedOpenAiKey) { throw 'The selected source secret has no OPENAI_API_KEY.' }
    $openAiKey = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($encodedOpenAiKey))
    $liteLlmKey = 'sk-' + (New-RandomToken).Replace('+', '').Replace('/', '').Replace('=', '')
    kubectl create secret generic learning-hub-runtime -n $Namespace `
        --from-literal=POSTGRES_USER=learning_hub `
        --from-literal=POSTGRES_PASSWORD=$(New-RandomToken) `
        --from-literal=KEYCLOAK_DB_PASSWORD=$(New-RandomToken) `
        --from-literal=KEYCLOAK_ADMIN_PASSWORD=$(New-RandomToken) `
        --from-literal=INTERNAL_AI_SERVICE_TOKEN=$(New-RandomToken) `
        --from-literal=LITELLM_API_KEY=$liteLlmKey `
        --from-literal=OPENAI_API_KEY=$openAiKey
}

if (-not (kubectl get secret learning-hub-dev-tls -n $Namespace --ignore-not-found -o name)) {
    $certificateDirectory = Join-Path $env:TEMP "learning-hub-cert-$([Guid]::NewGuid().ToString('N'))"
    New-Item -ItemType Directory -Path $certificateDirectory | Out-Null
    try {
        docker run --rm -v "${certificateDirectory}:/certs" alpine/openssl:3.5.4 req -x509 -nodes `
            -newkey rsa:2048 -days 365 -keyout /certs/tls.key -out /certs/tls.crt `
            -subj /CN=learning.localhost -addext 'subjectAltName=DNS:learning.localhost'
        if ($LASTEXITCODE -ne 0) { throw 'TLS certificate generation failed.' }
        $certificatePath = Join-Path $certificateDirectory 'tls.crt'
        $privateKeyPath = Join-Path $certificateDirectory 'tls.key'
        kubectl create secret tls learning-hub-dev-tls -n $Namespace `
            --cert=$certificatePath --key=$privateKeyPath
        if ($LASTEXITCODE -ne 0) { throw 'TLS secret creation failed.' }
    }
    finally {
        $resolved = [IO.Path]::GetFullPath($certificateDirectory)
        $tempRoot = [IO.Path]::GetFullPath($env:TEMP)
        if ($resolved.StartsWith($tempRoot, [StringComparison]::OrdinalIgnoreCase)) {
            Remove-Item -LiteralPath $resolved -Recurse -Force
        }
    }
}

kubectl create configmap keycloak-realm -n $Namespace `
    --from-file=learning-hub-realm.json=platform/keycloak/learning-hub-realm.json `
    --dry-run=client -o yaml | kubectl apply -f -
kubectl apply -f deploy/k3d-dev/platform.yaml

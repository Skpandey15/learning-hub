[CmdletBinding()]
param(
    [string] $Repository = 'Skpandey15/learning-hub',
    [string] $RunnerRoot = "$env:USERPROFILE\.github-runners\learning-hub-k3d"
)

$ErrorActionPreference = 'Stop'
$version = '2.336.0'
$expectedSha256 = 'd59123a43003e357b0805b5d0f611d0bd2f65ab67d51bd070dd4e7a0f685c162'
$archive = Join-Path $env:TEMP "actions-runner-win-x64-$version.zip"

if (-not (Test-Path (Join-Path $RunnerRoot 'run.cmd'))) {
    New-Item -ItemType Directory -Force -Path $RunnerRoot | Out-Null
    try {
        $uri = "https://github.com/actions/runner/releases/download/v$version/actions-runner-win-x64-$version.zip"
        Invoke-WebRequest -Uri $uri -OutFile $archive
        $actualSha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $archive).Hash.ToLowerInvariant()
        if ($actualSha256 -ne $expectedSha256) { throw 'GitHub Actions runner checksum mismatch.' }
        Expand-Archive -LiteralPath $archive -DestinationPath $RunnerRoot -Force
    }
    finally {
        if (Test-Path $archive) { Remove-Item -LiteralPath $archive -Force }
    }
}

if (-not (Test-Path (Join-Path $RunnerRoot '.runner'))) {
    $registrationToken = gh api --method POST "repos/$Repository/actions/runners/registration-token" --jq .token
    & (Join-Path $RunnerRoot 'config.cmd') --unattended `
        --url "https://github.com/$Repository" --token $registrationToken `
        --name 'learning-hub-k3d-dev' --labels 'k3d-dev' --work '_work' --replace
    if ($LASTEXITCODE -ne 0) { throw 'Runner registration failed.' }
}

$runCommand = Join-Path $RunnerRoot 'run.cmd'
$taskAction = New-ScheduledTaskAction -Execute 'cmd.exe' -Argument "/d /c `"$runCommand`"" -WorkingDirectory $RunnerRoot
$taskTrigger = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$taskSettings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
Register-ScheduledTask -TaskName 'LearningHubK3dRunner' -Action $taskAction -Trigger $taskTrigger `
    -Settings $taskSettings -Description 'Repository-scoped GitHub Actions runner for local k3d development deployments.' `
    -User $env:USERNAME -Force | Out-Null

if (-not (Get-Process Runner.Listener -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $runCommand -WorkingDirectory $RunnerRoot -WindowStyle Hidden
}

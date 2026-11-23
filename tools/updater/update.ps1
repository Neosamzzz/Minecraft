param(
    [string]$BaseUrl = "https://java-studyweb.oss-cn-beijing.aliyuncs.com/newearth",
    [string]$Instance = (Split-Path -Parent $PSScriptRoot),
    [switch]$DryRun
)

# 旧清单可能含有配置和地形文件，更新和删除均只处理 JAR。
$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"
try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch { }

# 统一成绝对路径，后面截相对路径才不会错位。
$Instance = (Resolve-Path -LiteralPath $Instance).Path

$stateDir = Join-Path $Instance ".updater"
$logFile = Join-Path $stateDir "update.log"
$manifestUrl = ($BaseUrl.TrimEnd("/") + "/manifest.json")
$modsDir = Join-Path $Instance "mods"

function Write-Log([string]$Message) {
    $line = "[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message
    Write-Host $line
    try {
        Assert-RealDirectory $stateDir
        if ([IO.File]::Exists($logFile) -and ([IO.File]::GetAttributes($logFile) -band [IO.FileAttributes]::ReparsePoint)) { return }
        if ([System.IO.File]::Exists($logFile) -and (Get-Item -LiteralPath $logFile).Length -gt 1MB) {
            Move-Item -Force -LiteralPath $logFile -Destination ($logFile + ".1")
        }
        Add-Content -LiteralPath $logFile -Value $line -Encoding UTF8
    } catch { }
}

# 复用一个 SHA256 实例和一块缓冲区，避免每个文件都重新分配。
$script:sha = [System.Security.Cryptography.SHA256]::Create()
$script:buffer = New-Object byte[] 262144

function Get-FileSha256([string]$Path) {
    $sha = $script:sha
    $sha.Initialize()
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        while (($read = $stream.Read($script:buffer, 0, $script:buffer.Length)) -gt 0) {
            [void]$sha.TransformBlock($script:buffer, 0, $read, $script:buffer, 0)
        }
        [void]$sha.TransformFinalBlock($script:buffer, 0, 0)
        return ([BitConverter]::ToString($sha.Hash) -replace "-", "").ToLower()
    } finally {
        $stream.Dispose()
    }
}

function Assert-RealDirectory([string]$Path) {
    if (-not [IO.Directory]::Exists($Path)) { return }
    if ([IO.File]::GetAttributes($Path) -band [IO.FileAttributes]::ReparsePoint) {
        throw "更新目录指向其他位置：$Path"
    }
}

function Get-ManagedPath([string]$Relative) {
    $segments = ($Relative.Replace("\", "/")).Split("/")
    if ($segments.Length -lt 2 -or $segments[0] -ine "mods") { throw "清单路径必须位于 mods/：$Relative" }
    foreach ($segment in $segments) {
        if ($segment -match '[<>:"|?*\x00-\x1f]' -or $segment -match '[. ]$' -or $segment.Length -eq 0 -or
            $segment -match '^(CON|PRN|AUX|NUL|COM[1-9¹²³]|LPT[1-9¹²³])(?:\.|$)') {
            throw "清单中的 Windows 路径无效：$Relative"
        }
    }
    return ($segments[1..($segments.Length - 1)] -join [IO.Path]::DirectorySeparatorChar)
}

function Get-ModTree([string]$Root, [string]$Directory = $Root) {
    Assert-RealDirectory $Directory
    foreach ($path in [IO.Directory]::EnumerateFileSystemEntries($Directory)) {
        $attributes = [IO.File]::GetAttributes($path)
        if ($attributes -band [IO.FileAttributes]::ReparsePoint) { throw "mods 目录含链接：$path" }
        $relative = $path.Substring($Root.Length + 1)
        $isDirectory = [bool]($attributes -band [IO.FileAttributes]::Directory)
        [pscustomobject]@{ Path = $path; Relative = $relative; IsDirectory = $isDirectory }
        if ($isDirectory) { Get-ModTree $Root $path }
    }
}

$nextMods = Join-Path $stateDir "mods-next"
$previousMods = Join-Path $stateDir "mods-previous"

function Remove-StagedMods([string]$Path) {
    if (-not [IO.Directory]::Exists($Path)) { return }
    $absolute = [IO.Path]::GetFullPath($Path)
    if ($absolute -ine $nextMods -and $absolute -ine $previousMods) {
        throw "目录不在更新暂存位置：$Path"
    }
    Assert-RealDirectory $stateDir
    foreach ($entry in (Get-ModTree $absolute)) { }
    [IO.Directory]::Delete($absolute, $true)
}

function Get-EncodedKey([string]$Key) {
    return (($Key.Split("/") | ForEach-Object { [Uri]::EscapeDataString($_) }) -join "/")
}

function Get-UpdateManifest {
    $tempManifest = Join-Path ([IO.Path]::GetTempPath()) ("newearth-manifest-" + [guid]::NewGuid().ToString("N") + ".json")
    try {
        Invoke-WebRequest -UseBasicParsing -Uri $manifestUrl -OutFile $tempManifest -TimeoutSec 20
        return Get-Content -LiteralPath $tempManifest -Raw -Encoding UTF8 | ConvertFrom-Json
    } finally {
        [IO.File]::Delete($tempManifest)
    }
}

function Invoke-Update {
    Assert-RealDirectory $modsDir
    Assert-RealDirectory $nextMods
    Assert-RealDirectory $previousMods
    if ([IO.Directory]::Exists($previousMods)) {
        if (-not [IO.Directory]::Exists($modsDir)) {
            if ($DryRun) { throw "发现需要恢复的旧 mods 目录，请运行正常更新。" }
            [IO.Directory]::Move($previousMods, $modsDir)
            Write-Log "已恢复上次切换前的 mods 目录。"
        } elseif (-not $DryRun) {
            Remove-StagedMods $previousMods
        }
    }
    if (-not $DryRun) { Remove-StagedMods $nextMods }

    $hadMods = [IO.Directory]::Exists($modsDir)
    try {
        $manifest = Get-UpdateManifest
        if ($null -eq $manifest -or $manifest.files -isnot [array]) { throw "清单缺少文件列表。" }
        $desired = @{}
        foreach ($file in $manifest.files) {
            if ($file.path -notlike "*.jar") { continue }
            $relative = Get-ManagedPath $file.path
            if ($desired.ContainsKey($relative) -or $file.sha256 -notmatch '^[0-9a-f]{64}$') {
                throw "清单文件条目无效：$($file.path)"
            }
            $contentKey = "files/sha256/" + $file.sha256.ToLowerInvariant()
            $previousKey = "files/mods/" + $relative.Replace("\", "/")
            if ($file.key -cne $contentKey -and $file.key -cne $previousKey) {
                throw "清单文件条目无效：$($file.path)"
            }
            $desired[$relative] = $file
        }
        $removed = @{}
        foreach ($path in @($manifest.removed)) {
            if ($path -notlike "*.jar") { continue }
            $relative = Get-ManagedPath $path
            if ($desired.ContainsKey($relative)) { throw "同一路径同时出现在更新和删除清单：$path" }
            $removed[$relative] = $true
        }

        $local = @{}
        $tree = @()
        if ($hadMods) { $tree = @(Get-ModTree $modsDir) }
        foreach ($entry in $tree) { $local[$entry.Relative] = $entry }
        $downloads = @{}
        foreach ($relative in $desired.Keys) {
            $file = $desired[$relative]
            $entry = $local[$relative]
            if ($null -ne $entry -and -not $entry.IsDirectory -and (Get-FileSha256 $entry.Path) -eq $file.sha256) { continue }
            $downloads[$relative] = $file
        }
        $existingRemovals = @($removed.Keys | Where-Object { $local.ContainsKey($_) -and -not $local[$_].IsDirectory })
        if ($downloads.Count -eq 0 -and $existingRemovals.Count -eq 0) {
            Write-Log "已是最新（$($manifest.version)）"
            return
        }
        Write-Log ("需要下载 {0} 个，移除 {1} 个（版本 {2}）" -f $downloads.Count, $existingRemovals.Count, $manifest.version)
        if ($DryRun) {
            foreach ($file in $downloads.Values) { Write-Log "  将下载：$($file.path)" }
            foreach ($relative in $existingRemovals) { Write-Log "  将移除：mods\$relative" }
            return
        }

        [void][IO.Directory]::CreateDirectory($nextMods)
        foreach ($entry in $tree) {
            $target = Join-Path $nextMods $entry.Relative
            if ($entry.IsDirectory) {
                [void][IO.Directory]::CreateDirectory($target)
            } elseif (-not ($downloads.ContainsKey($entry.Relative) -or $removed.ContainsKey($entry.Relative))) {
                [IO.File]::Copy($entry.Path, $target)
            }
        }
        $tempFile = Join-Path ([IO.Path]::GetTempPath()) ("newearth-mod-" + [guid]::NewGuid().ToString("N") + ".tmp")
        try {
            foreach ($relative in $downloads.Keys) {
                $file = $downloads[$relative]
                $target = Join-Path $nextMods $relative
                [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))
                $url = $BaseUrl.TrimEnd("/") + "/" + (Get-EncodedKey $file.key)
                Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $tempFile -TimeoutSec 600
                [IO.File]::Copy($tempFile, $target)
            }
        } finally {
            [IO.File]::Delete($tempFile)
        }
        foreach ($relative in $desired.Keys) {
            $target = Join-Path $nextMods $relative
            if ((Get-FileSha256 $target) -ne $desired[$relative].sha256) {
                throw "校验失败：$($desired[$relative].path)"
            }
        }
    } catch {
        if (-not $DryRun) { Remove-StagedMods $nextMods }
        if (-not $hadMods -or $DryRun) { throw }
        Write-Log "本次更新未切换 mods：$($_.Exception.Message)"
        return
    }

    if ($hadMods) { [IO.Directory]::Move($modsDir, $previousMods) }
    try {
        [IO.Directory]::Move($nextMods, $modsDir)
    } catch {
        if ($hadMods) { [IO.Directory]::Move($previousMods, $modsDir) }
        throw
    }
    try {
        Remove-StagedMods $previousMods
    } catch {
        Write-Log "新版本已切换，旧目录留待下次清理：$($_.Exception.Message)"
    }
    Write-Log "更新完成（版本 $($manifest.version)）"
}

try {
    Assert-RealDirectory $stateDir
    [void][IO.Directory]::CreateDirectory($stateDir)
    $lockPath = Join-Path $stateDir "update.lock"
    if ([IO.File]::Exists($lockPath) -and ([IO.File]::GetAttributes($lockPath) -band [IO.FileAttributes]::ReparsePoint)) {
        throw "更新锁指向其他位置。"
    }
    $lock = [IO.File]::Open($lockPath, "OpenOrCreate", "ReadWrite", "None")
    try {
        Write-Log "检查更新：$manifestUrl"
        Invoke-Update
    } finally {
        $lock.Dispose()
    }
    exit 0
} catch {
    Write-Log "更新失败：$($_.Exception.Message)"
    exit 1
}

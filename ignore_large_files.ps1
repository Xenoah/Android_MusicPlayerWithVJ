<#
.SYNOPSIS
Finds files larger than a specified size and adds them to .gitignore.

.DESCRIPTION
This script scans the current repository for any files larger than 75MB (by default).
If it finds such files, and they are not already in .gitignore, it appends their
relative paths to the .gitignore file to prevent them from being accidentally committed.
It explicitly ignores the .git folder.

.PARAMETER MaxSizeMB
The maximum allowed file size in Megabytes. Files larger than this will be ignored. Default is 75.

.EXAMPLE
.\ignore_large_files.ps1
Runs with the default 75MB limit.

.EXAMPLE
.\ignore_large_files.ps1 -MaxSizeMB 50
Runs with a 50MB limit.
#>

param (
    [int]$MaxSizeMB = 75
)

$thresholdBytes = $MaxSizeMB * 1MB
$gitignorePath = Join-Path $PWD ".gitignore"

Write-Host "Scanning for files larger than ${MaxSizeMB}MB..." -ForegroundColor Cyan

# Find large files, excluding the .git directory
$largeFiles = Get-ChildItem -File -Recurse -Force | 
    Where-Object { $_.FullName -notmatch '\\\.git\\' -and $_.Length -gt $thresholdBytes }

if ($null -eq $largeFiles -or $largeFiles.Count -eq 0) {
    Write-Host "No files found exceeding ${MaxSizeMB}MB." -ForegroundColor Green
    exit 0
}

Write-Host "Found $($largeFiles.Count) large file(s)." -ForegroundColor Yellow

# Read existing gitignore content to avoid duplicates
$existingIgnores = @()
if (Test-Path $gitignorePath) {
    $existingIgnores = Get-Content $gitignorePath | Where-Object { $_.Trim() -ne '' -and -not $_.StartsWith('#') }
}

$addedCount = 0

foreach ($file in $largeFiles) {
    # Get relative path with forward slashes for gitignore
    $relativePath = $file.FullName.Substring($PWD.Path.Length + 1).Replace('\', '/')
    
    # Check if already ignored (simple string match)
    if ($existingIgnores -notcontains $relativePath) {
        Write-Host "Adding to .gitignore: $relativePath ($([math]::Round($file.Length / 1MB, 2)) MB)"
        # Append to gitignore
        Add-Content -Path $gitignorePath -Value $relativePath
        $addedCount++
    } else {
        Write-Host "Already ignored: $relativePath" -ForegroundColor DarkGray
    }
}

if ($addedCount -gt 0) {
    Write-Host "Successfully added $addedCount new file(s) to .gitignore." -ForegroundColor Green
} else {
    Write-Host "No new files needed to be added to .gitignore." -ForegroundColor Green
}

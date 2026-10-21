Add-Type -AssemblyName System.Drawing

$srcPath = Resolve-Path "icone/Morabus.png"
$resDir = Resolve-Path "android/app/src/main/res"

$densities = @(
    @{ Name = "mipmap-mdpi"; LauncherSize = 48; ForegroundSize = 108 },
    @{ Name = "mipmap-hdpi"; LauncherSize = 72; ForegroundSize = 162 },
    @{ Name = "mipmap-xhdpi"; LauncherSize = 96; ForegroundSize = 216 },
    @{ Name = "mipmap-xxhdpi"; LauncherSize = 144; ForegroundSize = 324 },
    @{ Name = "mipmap-xxxhdpi"; LauncherSize = 192; ForegroundSize = 432 }
)

$srcImg = [System.Drawing.Image]::FromFile($srcPath)

function Resize-Image($image, $width, $height) {
    $bmp = New-Object System.Drawing.Bitmap($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($image, 0, 0, $width, $height)
    $g.Dispose()
    return $bmp
}

function Create-Circular-Image($image, $size) {
    $bmp = New-Object System.Drawing.Bitmap($size, $size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $path.AddEllipse(0, 0, $size, $size)
    $g.SetClip($path)
    $g.DrawImage($image, 0, 0, $size, $size)
    $g.Dispose()
    $path.Dispose()
    return $bmp
}

function Create-Adaptive-Foreground($image, $canvasSize) {
    $bmp = New-Object System.Drawing.Bitmap($canvasSize, $canvasSize, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    # Safe zone é ~68% do canvas (ex.: 72dp no centro de 108dp)
    $innerSize = [int]($canvasSize * 0.70)
    $offset = [int](($canvasSize - $innerSize) / 2)

    $g.DrawImage($image, $offset, $offset, $innerSize, $innerSize)
    $g.Dispose()
    return $bmp
}

foreach ($d in $densities) {
    $targetDir = Join-Path $resDir $d.Name
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Path $targetDir | Out-Null
    }

    # 1. ic_launcher.png
    $launcherBmp = Resize-Image $srcImg $d.LauncherSize $d.LauncherSize
    $launcherPath = Join-Path $targetDir "ic_launcher.png"
    $launcherBmp.Save($launcherPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $launcherBmp.Dispose()

    # 2. ic_launcher_round.png
    $roundBmp = Create-Circular-Image $srcImg $d.LauncherSize
    $roundPath = Join-Path $targetDir "ic_launcher_round.png"
    $roundBmp.Save($roundPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $roundBmp.Dispose()

    # 3. ic_launcher_foreground.png
    $fgBmp = Create-Adaptive-Foreground $srcImg $d.ForegroundSize
    $fgPath = Join-Path $targetDir "ic_launcher_foreground.png"
    $fgBmp.Save($fgPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $fgBmp.Dispose()

    Write-Host "Gerados icones para $($d.Name): $($d.LauncherSize)x$($d.LauncherSize) / FG: $($d.ForegroundSize)x$($d.ForegroundSize)"
}

$srcImg.Dispose()
Write-Host "Todos os icones do Morabus foram gerados com sucesso!"

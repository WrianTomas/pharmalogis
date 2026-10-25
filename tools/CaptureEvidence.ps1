param([string]$BaseUrl='http://localhost:8080',[switch]$MySql)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.Net.Http
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$out=Join-Path $root 'docs/evidencias'
if($MySql) { $out=Join-Path $out 'mysql' }
[IO.Directory]::CreateDirectory($out) | Out-Null
$profile=Join-Path $root '.local/chrome-evidence'
[IO.Directory]::CreateDirectory($profile) | Out-Null
$browser=Start-Process -FilePath 'C:\Program Files\Google\Chrome\Application\chrome.exe' -ArgumentList @('--headless=new','--disable-gpu','--no-first-run','--no-default-browser-check','--remote-debugging-port=9223',('--user-data-dir="'+$profile+'"'),'about:blank') -WindowStyle Hidden -PassThru
$socket=[Net.WebSockets.ClientWebSocket]::new()
$script:seq=0
function Cdp($method,$params=@{}) {
    $script:seq++
    $id=$script:seq
    $json=@{id=$id;method=$method;params=$params}|ConvertTo-Json -Depth 20 -Compress
    $bytes=[Text.Encoding]::UTF8.GetBytes($json)
    $cancel=[Threading.CancellationTokenSource]::new(20000)
    try {
        $socket.SendAsync([ArraySegment[byte]]::new($bytes),[Net.WebSockets.WebSocketMessageType]::Text,$true,$cancel.Token).GetAwaiter().GetResult() | Out-Null
        while($true) {
            $stream=[IO.MemoryStream]::new()
            do {
                $buffer=New-Object byte[] 65536
                $received=$socket.ReceiveAsync([ArraySegment[byte]]::new($buffer),$cancel.Token).GetAwaiter().GetResult()
                $stream.Write($buffer,0,$received.Count)
            } while(!$received.EndOfMessage)
            $message=[Text.Encoding]::UTF8.GetString($stream.ToArray())|ConvertFrom-Json
            $stream.Dispose()
            if($message.id -eq $id) {
                if($message.error) { throw ($message.error|ConvertTo-Json -Compress) }
                return $message.result
            }
        }
    } finally { $cancel.Dispose() }
}
function Eval($expression) {
    $r=Cdp 'Runtime.evaluate' @{expression=$expression;returnByValue=$true;awaitPromise=$true}
    if($r.exceptionDetails) { throw ($r.exceptionDetails|ConvertTo-Json -Depth 8) }
    return $r.result.value
}
function Wait-Page($path) {
    for($i=0;$i -lt 60;$i++) {
        Start-Sleep -Milliseconds 150
        try {
            $ready=Eval ('document.readyState === "complete" && location.pathname === '+($path|ConvertTo-Json -Compress))
            if($ready) { return }
        } catch { if($i -eq 59) { throw } }
    }
    throw "La página no terminó de cargar: $path"
}
function Navigate($path) { Cdp 'Page.navigate' @{url=($BaseUrl+$path)}|Out-Null; Wait-Page (($path -split '\?')[0]) }
function Capture($name) {
    Start-Sleep -Milliseconds 200
    $r=Cdp 'Page.captureScreenshot' @{format='png';captureBeyondViewport=$true}
    [IO.File]::WriteAllBytes((Join-Path $out $name),[Convert]::FromBase64String($r.data))
    Write-Output "Captura guardada: $name"
}
try {
    $target=$null
    for($i=0;$i -lt 8;$i++) {
        try { $targets=Invoke-RestMethod 'http://127.0.0.1:9223/json/list' -UseBasicParsing -TimeoutSec 8; $target=$null; foreach($candidate in $targets) { if($candidate.type -eq 'page') { $target=$candidate; break } }; if($target){break} } catch {}
        Start-Sleep -Milliseconds 250
    }
    if(!$target) { throw 'Chrome no inició el puerto de depuración.' }
    Write-Output 'Navegador de pruebas listo.'
    $socket.ConnectAsync([Uri]$target.webSocketDebuggerUrl,[Threading.CancellationToken]::None).GetAwaiter().GetResult()|Out-Null
    Cdp 'Page.enable'|Out-Null
    Cdp 'Emulation.setDeviceMetricsOverride' @{width=1440;height=1000;deviceScaleFactor=1;mobile=$false}|Out-Null
    Navigate '/login'; Capture '01-login.png'
    if($MySql) {
        $admin=Import-Clixml -LiteralPath (Join-Path $root '.local/admin-credential.xml')
        $emailJson=$admin.UserName|ConvertTo-Json -Compress
        $passwordJson=$admin.GetNetworkCredential().Password|ConvertTo-Json -Compress
        Eval ('document.querySelector("#email").value='+$emailJson+';document.querySelector("#password").value='+$passwordJson+';document.querySelector("form").submit();true')|Out-Null
        $passwordJson=$null
    } else {
        Eval 'document.querySelector("#email").value="admin@pharmalogis.pe";document.querySelector("#password").value="DemoPharma2026!";document.querySelector("form").submit();true'|Out-Null
    }
    Wait-Page '/attendance'
    Navigate '/departments'; Capture '02-departamentos.png'
    Navigate '/shifts'; Capture '03-turnos.png'
    Navigate '/employees'
    Eval 'document.querySelector("#fullName").value="Empleado de prueba";document.querySelector("#document").value="88888888";document.querySelector("#email").value="evidencia@pharmalogis.pe";document.querySelector("#departmentId").selectedIndex=1;document.querySelector("#shiftId").selectedIndex=1;document.querySelector(".form-panel form").submit();true'|Out-Null
    Wait-Page '/employees'; Capture '04-registro-listado.png'
    $editPath=Eval 'Array.from(document.querySelectorAll("tbody tr")).find(r=>r.textContent.includes("88888888")).querySelector("a").getAttribute("href")'
    Navigate $editPath; Capture '05-edicion.png'
    Eval 'document.querySelector("#fullName").value="Empleado actualizado";document.querySelector(".form-panel form").submit();true'|Out-Null
    Wait-Page '/employees'
    Navigate '/employees'
    Eval 'document.querySelector("#document").value="12";document.querySelector("#email").value="invalido";document.querySelector(".form-panel form").submit();true'|Out-Null
    Wait-Page '/employees'; Capture '06-validaciones.png'
    Navigate '/employees'
    Eval 'Array.from(document.querySelectorAll("tbody tr")).find(r=>r.textContent.includes("88888888")).querySelector("form").submit();true'|Out-Null
    Wait-Page '/employees'; Capture '07-eliminacion.png'
    if(Eval 'document.body.textContent.includes("88888888")') { throw 'El empleado de evidencia no se eliminó.' }
    Navigate '/attendance'
    Eval 'document.querySelector("form[action*=import]").submit();true'|Out-Null
    Wait-Page '/attendance'
    Eval 'document.querySelector("form[action*=calculate]").submit();true'|Out-Null
    Wait-Page '/attendance'; Capture '08-asistencias.png'
    if(!(Eval 'document.querySelector("tbody").textContent.includes("TARDANZA")')) { throw 'No se visualizó la asistencia calculada.' }
    Cdp 'Emulation.setDeviceMetricsOverride' @{width=390;height=844;deviceScaleFactor=1;mobile=$true}|Out-Null
    Capture '09-movil.png'
    Cdp 'Browser.close'|Out-Null
} finally {
    $socket.Dispose()
    if(!$browser.HasExited) { Stop-Process -Id $browser.Id -ErrorAction SilentlyContinue }
}

$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$db=Import-Clixml -LiteralPath (Join-Path $projectRoot '.local/mysql-credential.xml')
$admin=Import-Clixml -LiteralPath (Join-Path $projectRoot '.local/admin-credential.xml')
$env:DB_USERNAME=$db.UserName;$env:DB_PASSWORD=$db.GetNetworkCredential().Password
$env:DB_URL='jdbc:mysql://localhost:3306/pharmalogis_iam?serverTimezone=America/Lima'
$jar=Join-Path $projectRoot 'system/target/system-0.0.1-SNAPSHOT.jar'
$evidence=[Collections.Generic.List[string]]::new()
$evidence.Add('Prueba de persistencia MySQL tras reiniciar una instancia independiente de Spring Boot.')
$evidence.Add('Fecha local: '+(Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
$evidence.Add('Puerto de verificación: 8081. La instancia del usuario en 8080 permanece en ejecución.')
$process=$null
try {
    for($round=1;$round -le 2;$round++) {
        $process=Start-Process java -ArgumentList @('-jar',('"'+$jar+'"'),'--server.port=8081','--debug=false','--logging.level.root=WARN') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $projectRoot ('.local/restart-'+$round+'.out.log')) -RedirectStandardError (Join-Path $projectRoot ('.local/restart-'+$round+'.err.log'))
        $ready=$false
        for($i=0;$i -lt 45;$i++) {
            try { $page=Invoke-WebRequest 'http://127.0.0.1:8081/login' -UseBasicParsing -SessionVariable verifySession -TimeoutSec 2; if($page.StatusCode -eq 200){$ready=$true;break} } catch {}
            if($process.HasExited){throw 'La instancia de verificación terminó. Revisar .local/restart-*.log.'}
            Start-Sleep -Milliseconds 500
        }
        if(!$ready){throw 'La instancia de verificación no respondió.'}
        $csrf=[regex]::Match($page.Content,'name="_csrf"[^>]*value="([^"]+)"').Groups[1].Value
        if(!$csrf){throw 'No se encontró el token CSRF del login.'}
        Invoke-WebRequest 'http://127.0.0.1:8081/login' -Method Post -WebSession $verifySession -Body @{email=$admin.UserName;password=$admin.GetNetworkCredential().Password;_csrf=$csrf} -UseBasicParsing -TimeoutSec 15|Out-Null
        $attendance=Invoke-WebRequest 'http://127.0.0.1:8081/attendance' -WebSession $verifySession -UseBasicParsing -TimeoutSec 10
        foreach($expected in @('Ana Torres','Luis Mendoza','Carla Rojas','Diego Flores','TARDANZA','INCOMPLETA','FALTA')) {
            if(!$attendance.Content.Contains($expected)){throw ('No se recuperó el resultado persistido: '+$expected)}
        }
        $evidence.Add('Arranque '+$round+': esquema validado; login correcto; cuatro empleados y estados de asistencia recuperados desde MySQL.')
        Stop-Process -Id $process.Id
        $process.WaitForExit();$process.Dispose();$process=$null
        Start-Sleep -Milliseconds 500
    }
    $evidence.Add('Resultado: APROBADO. Los resultados se conservaron al cerrar y reiniciar el proceso Java de verificación.')
    $path=Join-Path $projectRoot 'docs/evidencias/mysql-reinicio.txt'
    [IO.File]::WriteAllLines($path,$evidence,[Text.UTF8Encoding]::new($false))
    Write-Output 'Persistencia tras reinicio: APROBADA.'
} finally {
    if($process -and !$process.HasExited){Stop-Process -Id $process.Id -ErrorAction SilentlyContinue}
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
}

param([switch]$EvidenceOnly)
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$credentialFile=Join-Path $projectRoot '.local/mysql-credential.xml'
if(!(Test-Path -LiteralPath $credentialFile)) { throw 'Primero ejecute tools/Configure-MySQL.ps1 desde su terminal.' }
$credential=Import-Clixml -LiteralPath $credentialFile
$env:DB_USERNAME=$credential.UserName
$env:DB_PASSWORD=$credential.GetNetworkCredential().Password
$env:DB_URL='jdbc:mysql://localhost:3306/pharmalogis_iam?serverTimezone=America/Lima'
if($EvidenceOnly) {
    $info=[Diagnostics.ProcessStartInfo]::new('D:\MySQL\bin\mysql.exe','--host=127.0.0.1 --port=3306 --default-character-set=utf8mb4 --table --user='+$credential.UserName)
    $info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    $info.StandardOutputEncoding=[Text.UTF8Encoding]::new($false)
    $info.StandardErrorEncoding=[Text.UTF8Encoding]::new($false)
    $info.EnvironmentVariables['MYSQL_PWD']=$env:DB_PASSWORD
    $p=[Diagnostics.Process]::Start($info)
    $outTask=$p.StandardOutput.ReadToEndAsync();$errTask=$p.StandardError.ReadToEndAsync()
    $sqlBytes=[Text.Encoding]::UTF8.GetBytes([IO.File]::ReadAllText((Join-Path $projectRoot 'database/03-evidence-queries.sql'))+[Environment]::NewLine)
    $p.StandardInput.BaseStream.Write($sqlBytes,0,$sqlBytes.Length);$p.StandardInput.Close();$p.WaitForExit()
    $result=$outTask.GetAwaiter().GetResult();$failure=$errTask.GetAwaiter().GetResult()
    if($p.ExitCode -ne 0) { throw $failure }
    $evidence=Join-Path $projectRoot 'docs/evidencias/mysql-resultados.txt'
    [IO.File]::WriteAllText($evidence,$result,[Text.UTF8Encoding]::new($false));$p.Dispose()
    Write-Host "Resultados guardados en $evidence"
    exit
}
$env:BOOTSTRAP_ADMIN_EMAIL='admin@pharmalogis.pe'
$adminCredentialFile=Join-Path $projectRoot '.local/admin-credential.xml'
if(Test-Path -LiteralPath $adminCredentialFile) { $admin=Import-Clixml -LiteralPath $adminCredentialFile }
else {
    $adminPassword=Read-Host 'Elija una contraseña para admin@pharmalogis.pe (mínimo 12 caracteres)' -AsSecureString
    $admin=[PSCredential]::new($env:BOOTSTRAP_ADMIN_EMAIL,$adminPassword)
    if($admin.GetNetworkCredential().Password.Length -lt 12) { throw 'La contraseña debe tener al menos 12 caracteres.' }
    $admin|Export-Clixml -LiteralPath $adminCredentialFile
}
$env:BOOTSTRAP_ADMIN_PASSWORD=$admin.GetNetworkCredential().Password
$jwtFile=Join-Path $projectRoot '.local/jwt-secret.xml'
if(Test-Path -LiteralPath $jwtFile) { $jwt=Import-Clixml -LiteralPath $jwtFile }
else {
    $keyBytes=New-Object byte[] 48;$rng=[Security.Cryptography.RandomNumberGenerator]::Create();$rng.GetBytes($keyBytes);$rng.Dispose()
    $jwt=[PSCredential]::new('jwt',(ConvertTo-SecureString ([Convert]::ToBase64String($keyBytes)) -AsPlainText -Force));$jwt|Export-Clixml -LiteralPath $jwtFile
}
$env:JWT_SECRET=$jwt.GetNetworkCredential().Password
$jar=Join-Path $projectRoot 'system/target/system-0.0.1-SNAPSHOT.jar'
if(!(Test-Path -LiteralPath $jar)) { throw 'Genere el ejecutable con system/mvnw.cmd package.' }
Write-Host 'Iniciando PharmaLogis con MySQL en http://localhost:8080. Ctrl+C para detener.'
try { & java -jar $jar --debug=false }
finally { Remove-Item Env:DB_PASSWORD,Env:BOOTSTRAP_ADMIN_PASSWORD,Env:JWT_SECRET -ErrorAction SilentlyContinue }

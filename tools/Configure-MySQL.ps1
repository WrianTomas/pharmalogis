# Ejecutar en la terminal del usuario; nunca solicita contraseñas por el chat.
param([string]$MySqlExe='D:\MySQL\bin\mysql.exe')
$ErrorActionPreference='Stop'
$projectRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$localPath=Join-Path $projectRoot '.local'
[IO.Directory]::CreateDirectory($localPath)|Out-Null
$credentialFile=Join-Path $localPath 'mysql-credential.xml'
if(!(Test-Path -LiteralPath $MySqlExe)) { throw "No se encontró mysql.exe en $MySqlExe. Especifique -MySqlExe con la ruta correcta." }
$rootSecret=Read-Host 'Contraseña de root de MySQL (solo se usa para preparar la base)' -AsSecureString
$rootPassword=[Net.NetworkCredential]::new('root',$rootSecret).Password
function Invoke-MySql([string]$sql,[string]$user,[string]$password) {
    $info=[Diagnostics.ProcessStartInfo]::new()
    $info.FileName=$MySqlExe
    $info.Arguments='--host=127.0.0.1 --port=3306 --default-character-set=utf8mb4 --batch --skip-column-names --user='+$user
    $info.UseShellExecute=$false
    $info.CreateNoWindow=$true
    $info.RedirectStandardInput=$true
    $info.RedirectStandardOutput=$true
    $info.RedirectStandardError=$true
    $info.StandardOutputEncoding=[Text.UTF8Encoding]::new($false)
    $info.EnvironmentVariables['MYSQL_PWD']=$password
    $process=[Diagnostics.Process]::new();$process.StartInfo=$info
    $process.Start()|Out-Null
    $outputTask=$process.StandardOutput.ReadToEndAsync()
    $errorTask=$process.StandardError.ReadToEndAsync()
    $sqlBytes=[Text.Encoding]::UTF8.GetBytes($sql+[Environment]::NewLine)
    $process.StandardInput.BaseStream.Write($sqlBytes,0,$sqlBytes.Length)
    $process.StandardInput.Close()
    $process.WaitForExit()
    $output=$outputTask.GetAwaiter().GetResult();$failure=$errorTask.GetAwaiter().GetResult()
    if($process.ExitCode -ne 0) { throw "MySQL rechazó la operación: $failure" }
    $process.Dispose()
    return $output
}
try {
    $version=Invoke-MySql 'SELECT VERSION();' 'root' $rootPassword
    Write-Host ('Conectado a MySQL '+$version.Trim())
    Invoke-MySql ([IO.File]::ReadAllText((Join-Path $projectRoot 'database/01-schema.sql'))) 'root' $rootPassword|Out-Null
    if(Test-Path -LiteralPath $credentialFile) {
        $appCredential=Import-Clixml -LiteralPath $credentialFile
    } else {
        $existing=Invoke-MySql "SELECT COUNT(*) FROM mysql.user WHERE User='pharmalogis_project' AND Host='localhost';" 'root' $rootPassword
        if($existing.Trim() -ne '0') { throw 'El usuario pharmalogis_project ya existe. No se cambió su contraseña. Configure una cuenta propia o recupere el archivo de credencial original.' }
        $randomBytes=New-Object byte[] 36
        $random=[Security.Cryptography.RandomNumberGenerator]::Create();$random.GetBytes($randomBytes);$random.Dispose()
        $appPassword=[Convert]::ToBase64String($randomBytes)
        Invoke-MySql ("CREATE USER 'pharmalogis_project'@'localhost' IDENTIFIED BY '"+$appPassword+"'; GRANT SELECT,INSERT,UPDATE,DELETE ON pharmalogis_iam.* TO 'pharmalogis_project'@'localhost';") 'root' $rootPassword|Out-Null
        $appCredential=[PSCredential]::new('pharmalogis_project',(ConvertTo-SecureString $appPassword -AsPlainText -Force))
        $appCredential|Export-Clixml -LiteralPath $credentialFile
    }
    Invoke-MySql ([IO.File]::ReadAllText((Join-Path $projectRoot 'database/02-demo-data.sql')).Replace("SET @demo_date = '2026-10-23';",("SET @demo_date = '"+(Get-Date -Format 'yyyy-MM-dd')+"';"))) 'root' $rootPassword|Out-Null
    $check=Invoke-MySql 'USE pharmalogis_iam; SELECT COUNT(*) FROM employees;' $appCredential.UserName $appCredential.GetNetworkCredential().Password
    Write-Host ('Base y usuario de aplicación configurados. Empleados: '+$check.Trim())
    Write-Host 'La contraseña de aplicación queda cifrada con DPAPI para tu usuario de Windows en .local/mysql-credential.xml.'
    Write-Host 'Siguiente: powershell -NoProfile -ExecutionPolicy Bypass -File tools/Start-MySQL.ps1'
} finally { $rootPassword=$null;$appPassword=$null }

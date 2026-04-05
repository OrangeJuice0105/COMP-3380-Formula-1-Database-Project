Param(
    [string]$UMNetId, ## Your UMNetID name: for instance mine is thaidc
    [string]$PwdPath ## The path you want your file to go to on Aviary. Mine will be /home/student/thaidc/COMP-3380-Project
)

[string]$JARFILE = "mssql-jdbc-13.4.0.jre11.jar"

$Path = "$UMNetId@aviary.cs.umanitoba.ca:$PwdPath"

Set-Location ..
Set-Location lib

scp $JARFILE $Path

Set-Location ..
Set-Location src

scp *.java $Path

Set-Location ..

scp Makefile $Path
scp -r sql $Path

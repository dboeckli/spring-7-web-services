cd target/helm/repo

$APPLICATION_NAME = Get-ChildItem -Directory | Where-Object { $_.LastWriteTime -ge $file.LastWriteTime } | Select-Object -ExpandProperty Name
Write-Host "test application: $APPLICATION_NAME"
helm test $APPLICATION_NAME --namespace spring-7-project-template --logs

kubectl delete pod -n spring-7-project-template --field-selector=status.phase==Succeeded
kubectl delete pod -n spring-7-project-template --field-selector=status.phase==Failed


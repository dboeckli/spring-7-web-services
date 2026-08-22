cd target/helm/repo

$file = Get-ChildItem -Filter spring-7-web-services-chart-*.tgz | Select-Object -First 1
$APPLICATION_NAME = Get-ChildItem -Directory | Where-Object { $_.LastWriteTime -ge $file.LastWriteTime } | Select-Object -ExpandProperty Name
Write-Host "test application: $APPLICATION_NAME"
helm test $APPLICATION_NAME --namespace spring-7-web-services --logs

kubectl delete pod -n spring-7-web-services --field-selector=status.phase==Succeeded
kubectl delete pod -n spring-7-web-services --field-selector=status.phase==Failed


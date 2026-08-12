
```
docker compose up -d --build app
```
```
docker compose up -d --build --force-recreate app
```
```
docker compose build app
```
```
cd /Users/tigerpro/Documents/AI/vmware-ai/vetautet-app/vetautet
```
```
docker compose up -d --build app
```
```
docker compose up -d grafana prometheus postgres-exporter redis-exporter
```
```
docker compose up -d --force-recreate node-exporter prometheus
```
```
docker compose up -d --force-recreate alertmanager prometheus
```
Nếu bạn vừa thêm image/service lần đầu:
```
docker compose up -d --build --force-recreate alertmanager prometheus
```
```
docker compose up -d --force-recreate prometheus
```
curl http://localhost:9090/api/v1/alertmanagers

![img.png](docs/img.png)
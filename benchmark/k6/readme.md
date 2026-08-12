```
docker run --rm --network host \
-v "$PWD:/work" -w /work \
grafana/k6 run benchmark/k6/login.js
```

```
docker run --rm --network host \
-v "$PWD:/work" -w /work \
grafana/k6 run benchmark/k6/register.js
```
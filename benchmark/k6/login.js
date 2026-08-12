import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const LOGIN_PATH = "/api/v1/auth/login";
const EMAIL = __ENV.LOGIN_EMAIL || "abc@fpt.com";
const PASSWORD = __ENV.LOGIN_PASSWORD || "string";

const loginFailedRate = new Rate("login_failed_rate");
const loginDuration = new Trend("login_duration", true);

export const options = {
  scenarios: {
    concurrent_login_500_users: {
      executor: "constant-vus",
      vus: Number(__ENV.VUS || 500),
      duration: __ENV.DURATION || "1m",
      gracefulStop: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<2000", "p(99)<5000"],
    login_failed_rate: ["rate<0.05"],
  },
};

const payload = JSON.stringify({
  email: EMAIL,
  password: PASSWORD,
});

const params = {
  headers: {
    accept: "*/*",
    "Content-Type": "application/json",
  },
  timeout: "10s",
};

export default function () {
  const response = http.post(`${BASE_URL}${LOGIN_PATH}`, payload, params);
  loginDuration.add(response.timings.duration);

  const ok = check(response, {
    "login status is 2xx": (r) => r.status >= 200 && r.status < 300,
    "login response has body": (r) => Boolean(r.body && r.body.length > 0),
  });

  loginFailedRate.add(!ok);
  sleep(Number(__ENV.SLEEP_SECONDS || 1));
}

export function handleSummary(data) {
  return {
    stdout: [
      "Login load test summary",
      `Base URL: ${BASE_URL}`,
      `Virtual users: ${options.scenarios.concurrent_login_500_users.vus}`,
      `Duration: ${options.scenarios.concurrent_login_500_users.duration}`,
      `Requests: ${data.metrics.http_reqs?.values?.count || 0}`,
      `Failures: ${data.metrics.http_req_failed?.values?.rate || 0}`,
      "",
    ].join("\n"),
    "benchmark/k6/login-summary.json": JSON.stringify(data, null, 2),
  };
}

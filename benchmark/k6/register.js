import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const REGISTER_PATH = "/api/v1/auth/register";
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || "fpt.com";
const PASSWORD = __ENV.REGISTER_PASSWORD || "string123";
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;

const registerFailedRate = new Rate("register_failed_rate");
const registerDuration = new Trend("register_duration", true);

export const options = {
  scenarios: {
    concurrent_register_500_users: {
      executor: "constant-vus",
      vus: Number(__ENV.VUS || 10),
      duration: __ENV.DURATION || "1m",
      gracefulStop: "30s",
    },
  },
  thresholds: {
    http_req_failed: ["rate<0.05"],
    http_req_duration: ["p(95)<3000", "p(99)<7000"],
    register_failed_rate: ["rate<0.05"],
  },
};

const params = {
  headers: {
    accept: "*/*",
    "Content-Type": "application/json",
  },
  timeout: "15s",
};

function buildPayload() {
  const userNo = `${RUN_ID}${__VU}${__ITER}`;

  return JSON.stringify({
    username: `user${userNo}`,
    firstName: "Load",
    lastName: `User ${__VU}`,
    email: `loadtest${userNo}@${EMAIL_DOMAIN}`,
    password: PASSWORD,
    mobileCountryCode: "+84",
    mobileNumber: `900${String(__VU).padStart(4, "0")}${String(__ITER).padStart(4, "0")}`.slice(0, 20),
    avatarUrl: null,
  });
}

export default function () {
  const response = http.post(`${BASE_URL}${REGISTER_PATH}`, buildPayload(), params);
  registerDuration.add(response.timings.duration);

  const ok = check(response, {
    "register status is 201": (r) => r.status === 201,
    "register response has body": (r) => Boolean(r.body && r.body.length > 0),
  });

  registerFailedRate.add(!ok);
  sleep(Number(__ENV.SLEEP_SECONDS || 1));
}

export function handleSummary(data) {
  return {
    stdout: [
      "Register load test summary",
      `Base URL: ${BASE_URL}`,
      `Virtual users: ${options.scenarios.concurrent_register_500_users.vus}`,
      `Duration: ${options.scenarios.concurrent_register_500_users.duration}`,
      `Run ID: ${RUN_ID}`,
      `Requests: ${data.metrics.http_reqs?.values?.count || 0}`,
      `Failures: ${data.metrics.http_req_failed?.values?.rate || 0}`,
      "",
    ].join("\n"),
    "benchmark/k6/register-summary.json": JSON.stringify(data, null, 2),
  };
}

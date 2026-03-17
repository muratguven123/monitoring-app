"""
Mock New Relic REST API v2 Server
==================================
Uygulamayı gerçek bir New Relic hesabı olmadan test etmek için kullanın.

Desteklenen endpoint'ler:
  GET /v2/applications.json
  GET /v2/applications/{id}.json
  GET /v2/applications/{id}/metrics.json
  GET /v2/applications/{id}/metrics/data.json
  GET /v2/alerts_violations.json

Çalıştırmak için:
  python server.py
  ya da Docker ile: docker compose up
"""

import json
import random
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.parse import urlparse, parse_qs

# ── Sahte Uygulama Verileri ────────────────────────────────────────────────────

APPLICATIONS = [
    {
        "id": 1001,
        "name": "payment-service",
        "language": "java",
        "health_status": "red",
        "reporting": True,
        "last_reported_at": "2026-03-17T10:00:00+00:00",
        "application_summary": {
            "response_time": 1450.5,
            "throughput": 23.4,
            "error_rate": 8.3,
            "apdex_target": 0.5,
            "apdex_score": 0.42,
            "host_count": 3,
            "instance_count": 12,
            "concurrent_instance_count": 8
        },
        "end_user_summary": {
            "response_time": 2100.0,
            "throughput": 18.2,
            "apdex_target": 1.0,
            "apdex_score": 0.38
        }
    },
    {
        "id": 1002,
        "name": "auth-service",
        "language": "go",
        "health_status": "green",
        "reporting": True,
        "last_reported_at": "2026-03-17T10:00:00+00:00",
        "application_summary": {
            "response_time": 23.1,
            "throughput": 450.2,
            "error_rate": 0.08,
            "apdex_target": 0.5,
            "apdex_score": 0.98,
            "host_count": 5,
            "instance_count": 20,
            "concurrent_instance_count": 15
        },
        "end_user_summary": None
    },
    {
        "id": 1003,
        "name": "notification-worker",
        "language": "java",
        "health_status": "green",
        "reporting": True,
        "last_reported_at": "2026-03-17T10:00:00+00:00",
        "application_summary": {
            "response_time": 67.3,
            "throughput": 120.0,
            "error_rate": 0.0,
            "apdex_target": 0.5,
            "apdex_score": 0.96,
            "host_count": 2,
            "instance_count": 4,
            "concurrent_instance_count": 2
        },
        "end_user_summary": None
    },
    {
        "id": 1004,
        "name": "legacy-monolith",
        "language": "ruby",
        "health_status": "orange",
        "reporting": True,
        "last_reported_at": "2026-03-17T09:45:00+00:00",
        "application_summary": {
            "response_time": 890.0,
            "throughput": 88.5,
            "error_rate": 5.1,
            "apdex_target": 1.0,
            "apdex_score": 0.71,
            "host_count": 2,
            "instance_count": 6,
            "concurrent_instance_count": 4
        },
        "end_user_summary": {
            "response_time": 1200.0,
            "throughput": 75.0,
            "apdex_target": 2.0,
            "apdex_score": 0.65
        }
    },
    {
        "id": 1005,
        "name": "inventory-api",
        "language": "python",
        "health_status": "green",
        "reporting": True,
        "last_reported_at": "2026-03-17T10:00:00+00:00",
        "application_summary": {
            "response_time": 145.0,
            "throughput": 310.0,
            "error_rate": 0.5,
            "apdex_target": 0.5,
            "apdex_score": 0.91,
            "host_count": 3,
            "instance_count": 9,
            "concurrent_instance_count": 6
        },
        "end_user_summary": None
    }
]

# ── Açık Alert Violation'lar ───────────────────────────────────────────────────

VIOLATIONS = [
    {
        "id": 9001,
        "label": "payment-service / High Error Rate",
        "duration": 1800,
        "policy_name": "payment-alerts",
        "condition_name": "HighErrorRate",
        "priority": "critical",
        "opened_at": int(time.time()) - 1800,
        "closed_at": None,
        "entity": {"product": "APM", "type": "Application", "group_id": 1001, "name": "payment-service"},
        "links": {"policy_id": 501, "condition_id": 601}
    },
    {
        "id": 9002,
        "label": "payment-service / Slow Response Time",
        "duration": 3600,
        "policy_name": "sla-policy",
        "condition_name": "SlowResponseTime",
        "priority": "warning",
        "opened_at": int(time.time()) - 3600,
        "closed_at": None,
        "entity": {"product": "APM", "type": "Application", "group_id": 1001, "name": "payment-service"},
        "links": {"policy_id": 502, "condition_id": 602}
    },
    {
        "id": 9003,
        "label": "legacy-monolith / Elevated Error Rate",
        "duration": 900,
        "policy_name": "default-policy",
        "condition_name": "ElevatedErrorRate",
        "priority": "warning",
        "opened_at": int(time.time()) - 900,
        "closed_at": None,
        "entity": {"product": "APM", "type": "Application", "group_id": 1004, "name": "legacy-monolith"},
        "links": {"policy_id": 503, "condition_id": 603}
    }
]

# ── HTTP Handler ───────────────────────────────────────────────────────────────

class MockNewRelicHandler(BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        print(f"[NewRelic Mock] {self.address_string()} - {format % args}")

    def send_json(self, data, status=200):
        body = json.dumps(data, indent=2).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", len(body))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self):
        parsed = urlparse(self.path)
        path   = parsed.path
        params = parse_qs(parsed.query)

        # ── GET /v2/applications.json ──────────────────────────────────────
        if path == "/v2/applications.json":
            filter_name = params.get("filter[name]", [None])[0]
            apps = APPLICATIONS
            if filter_name:
                apps = [a for a in apps if filter_name.lower() in a["name"].lower()]
            self.send_json({"applications": apps})

        # ── GET /v2/applications/{id}.json ─────────────────────────────────
        elif path.startswith("/v2/applications/") and path.endswith(".json") and "/metrics" not in path:
            app_id = int(path.split("/")[3].replace(".json", ""))
            app = next((a for a in APPLICATIONS if a["id"] == app_id), None)
            if app:
                self.send_json({"application": app})
            else:
                self.send_json({"error": "Application not found"}, 404)

        # ── GET /v2/applications/{id}/metrics.json ─────────────────────────
        elif "/metrics.json" in path and "/metrics/data" not in path:
            self.send_json({"metrics": [
                {"name": "HttpDispatcher", "values": ["average_response_time", "calls_per_minute", "error_rate"]},
                {"name": "Apdex",          "values": ["score", "threshold"]},
                {"name": "Errors/all",     "values": ["error_count", "errors_per_minute"]},
                {"name": "EndUser/Apdex",  "values": ["score", "threshold"]},
                {"name": "Memory/Physical","values": ["used_mb", "total_mb"]},
            ]})

        # ── GET /v2/applications/{id}/metrics/data.json ────────────────────
        elif "/metrics/data.json" in path:
            self.send_json({"metric_data": {
                "from": "2026-03-17T09:00:00+00:00",
                "to":   "2026-03-17T10:00:00+00:00",
                "metrics": [
                    {"name": "HttpDispatcher", "timeslices": [
                        {"from": "2026-03-17T09:00:00+00:00", "to": "2026-03-17T10:00:00+00:00",
                         "values": {"average_response_time": round(random.uniform(100, 1500), 1),
                                    "calls_per_minute": round(random.uniform(10, 500), 1),
                                    "error_rate": round(random.uniform(0, 10), 2)}}
                    ]},
                    {"name": "Apdex", "timeslices": [
                        {"from": "2026-03-17T09:00:00+00:00", "to": "2026-03-17T10:00:00+00:00",
                         "values": {"score": round(random.uniform(0.4, 1.0), 2),
                                    "threshold": 0.5}}
                    ]}
                ]
            }})

        # ── GET /v2/alerts_violations.json ─────────────────────────────────
        elif path == "/v2/alerts_violations.json":
            only_open = params.get("only_open", ["false"])[0].lower() == "true"
            violations = [v for v in VIOLATIONS if only_open is False or v["closed_at"] is None]
            self.send_json({"violations": violations})

        else:
            self.send_json({"error": f"Unknown endpoint: {path}"}, 404)

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "X-Api-Key, Content-Type")
        self.end_headers()


if __name__ == "__main__":
    PORT = 5000
    server = HTTPServer(("0.0.0.0", PORT), MockNewRelicHandler)
    print(f"""
╔══════════════════════════════════════════════════════╗
║       Mock New Relic API Server — port {PORT}          ║
╠══════════════════════════════════════════════════════╣
║  5 uygulama  |  3 açık violation  |  gerçekçi metrik ║
║                                                      ║
║  Android uygulamasında kullanılacak değerler:        ║
║  API Key  : MOCK-TEST-API-KEY-12345                  ║
║  Account  : 9999999                                  ║
╚══════════════════════════════════════════════════════╝
    """)
    server.serve_forever()

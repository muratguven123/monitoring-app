#!/usr/bin/env bash
# ════════════════════════════════════════════════════════════════════════════
#  setup.sh — Test ortamını başlatır ve Grafana API key'i oluşturur
#  Kullanım: ./setup.sh
# ════════════════════════════════════════════════════════════════════════════
set -e

GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="admin"
GRAFANA_PASS="monitoring123"

echo ""
echo "🚀 Docker servisleri başlatılıyor..."
docker compose up -d --build

# ── Grafana hazır olana kadar bekle ─────────────────────────────────────────
echo ""
echo "⏳ Grafana'nın hazır olması bekleniyor..."
for i in $(seq 1 30); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        "$GRAFANA_URL/api/health" 2>/dev/null || echo "000")
    if [ "$STATUS" = "200" ]; then
        echo "   ✅ Grafana hazır"
        break
    fi
    if [ "$i" = "30" ]; then
        echo "   ❌ Grafana 30 saniyede hazır olmadı. Logları kontrol edin:"
        echo "      docker compose logs grafana"
        exit 1
    fi
    printf "   Deneme %d/30...\r" "$i"
    sleep 1
done

# ── Mock New Relic hazır olana kadera bekle ──────────────────────────────────
echo "⏳ Mock New Relic API'nin hazır olması bekleniyor..."
for i in $(seq 1 15); do
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
        "http://localhost:5000/v2/applications.json" 2>/dev/null || echo "000")
    if [ "$STATUS" = "200" ]; then
        echo "   ✅ Mock New Relic hazır"
        break
    fi
    sleep 1
done

# ── Grafana Service Account Token oluştur ────────────────────────────────────
echo ""
echo "🔑 Grafana API token oluşturuluyor..."

# Service account oluştur
SA_RESPONSE=$(curl -s -X POST "$GRAFANA_URL/api/serviceaccounts" \
    -H "Content-Type: application/json" \
    -u "$GRAFANA_USER:$GRAFANA_PASS" \
    -d '{"name":"monitoring-app","role":"Viewer"}')

SA_ID=$(echo "$SA_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "")

if [ -z "$SA_ID" ]; then
    # Service account zaten var, id'yi bul
    SA_ID=$(curl -s "$GRAFANA_URL/api/serviceaccounts/search?query=monitoring-app" \
        -u "$GRAFANA_USER:$GRAFANA_PASS" | \
        python3 -c "import sys,json; d=json.load(sys.stdin); print(d['serviceAccounts'][0]['id'])" 2>/dev/null || echo "1")
fi

# Token oluştur
TOKEN_RESPONSE=$(curl -s -X POST "$GRAFANA_URL/api/serviceaccounts/$SA_ID/tokens" \
    -H "Content-Type: application/json" \
    -u "$GRAFANA_USER:$GRAFANA_PASS" \
    -d '{"name":"monitoring-app-token"}')

GRAFANA_TOKEN=$(echo "$TOKEN_RESPONSE" | \
    python3 -c "import sys,json; print(json.load(sys.stdin)['key'])" 2>/dev/null || echo "")

if [ -z "$GRAFANA_TOKEN" ]; then
    echo "   ⚠️  Token oluşturulamadı (zaten var olabilir)."
    echo "   Grafana UI'dan manuel oluşturun:"
    echo "   $GRAFANA_URL → Administration → Service Accounts → monitoring-app → Add token"
    GRAFANA_TOKEN="<MANUEL_OLUŞTURUN>"
fi

# ── Bilgisayarın LAN IP'sini bul ─────────────────────────────────────────────
LAN_IP=$(ipconfig getifaddr en0 2>/dev/null || \
         ip route get 1.2.3.4 2>/dev/null | awk '{print $7; exit}' || \
         hostname -I 2>/dev/null | awk '{print $1}' || \
         echo "YOUR_IP")

# ── Özet ─────────────────────────────────────────────────────────────────────
echo ""
echo "════════════════════════════════════════════════════════════════"
echo "  ✅  Test ortamı hazır!"
echo "════════════════════════════════════════════════════════════════"
echo ""
echo "  🟠  GRAFANA"
echo "      Tarayıcı URL  : $GRAFANA_URL"
echo "      Kullanıcı     : $GRAFANA_USER"
echo "      Şifre         : $GRAFANA_PASS"
echo ""
echo "  📱  Android Uygulaması — Grafana Ayarları:"
echo "      Base URL      : http://$LAN_IP:3000"
echo "      API Key       : $GRAFANA_TOKEN"
echo ""
echo "  🟢  NEW RELIC (MOCK)"
echo "  📱  Android Uygulaması — New Relic Ayarları:"
echo "      API Key       : MOCK-TEST-API-KEY-12345"
echo "      Account ID    : 9999999"
echo ""
echo "  ⚠️  ÖNEMLİ: Uygulamada Base URL olarak localhost DEĞİL"
echo "      $LAN_IP adresini kullanın."
echo ""
echo "  🛑  Durdurmak için: docker compose down"
echo "════════════════════════════════════════════════════════════════"
echo ""

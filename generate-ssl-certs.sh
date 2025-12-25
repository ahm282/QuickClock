#!/bin/sh
set -eu

CERTS_DIR="${CERTS_DIR:-./certs}"
KEY_PATH="$CERTS_DIR/localhost.key"
CERT_PATH="$CERTS_DIR/localhost.crt"
KEYSTORE_PATH="$CERTS_DIR/keystore.p12"

PASSWORD="${SSL_KEYSTORE_PASSWORD:-changeit}"
# Strip accidental quotes
PASSWORD="${PASSWORD%\"}"
PASSWORD="${PASSWORD#\"}"

mkdir -p "$CERTS_DIR"

if [ ! -f "$KEY_PATH" ] || [ ! -f "$CERT_PATH" ]; then
  echo "[tls] Generating self-signed certificate for localhost..."
  openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
    -keyout "$KEY_PATH" \
    -out "$CERT_PATH" \
    -subj "/C=BE/ST=Dev/L=Local/O=QuickClock/OU=Dev/CN=localhost" \
    -addext "subjectAltName=DNS:localhost,DNS:*.localhost,DNS:backend,DNS:frontend,IP:127.0.0.1"
fi

echo "[tls] Writing PKCS12 keystore for Spring Boot (overwriting)..."
rm -f "$KEYSTORE_PATH"
openssl pkcs12 -export \
  -in "$CERT_PATH" \
  -inkey "$KEY_PATH" \
  -out "$KEYSTORE_PATH" \
  -name springboot \
  -passout "pass:$PASSWORD"

echo "[tls] Done."

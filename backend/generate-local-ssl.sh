#!/bin/bash
# QuickClock - Generate Local Development SSL Certificate
# Run this from the backend directory

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYSTORE_DIR="$SCRIPT_DIR/src/main/resources"
ENV_FILE="$SCRIPT_DIR/.env"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}QuickClock Local SSL Setup${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if .env exists
if [ ! -f "$ENV_FILE" ]; then
    echo -e "${RED}Error: .env file not found!${NC}"
    echo -e "${YELLOW}Please copy .env.example to .env first:${NC}"
    echo -e "  cp .env.example .env"
    echo -e "  nano .env  # Edit with your values"
    exit 1
fi

# Load SSL_KEYSTORE_PASSWORD from .env
if [ -f "$ENV_FILE" ]; then
    export $(grep -v '^#' "$ENV_FILE" | grep SSL_KEYSTORE_PASSWORD | xargs)
fi

# Check if password is set
if [ -z "$SSL_KEYSTORE_PASSWORD" ]; then
    echo -e "${YELLOW}SSL_KEYSTORE_PASSWORD not found in .env${NC}"
    read -s -p "Enter keystore password: " SSL_KEYSTORE_PASSWORD
    echo ""
    if [ -z "$SSL_KEYSTORE_PASSWORD" ]; then
        echo -e "${RED}Password cannot be empty!${NC}"
        exit 1
    fi
fi

# Create resources directory if it doesn't exist
mkdir -p "$KEYSTORE_DIR"

# Check if keystore already exists
if [ -f "$KEYSTORE_DIR/keystore.p12" ]; then
    echo -e "${YELLOW}Keystore already exists at: $KEYSTORE_DIR/keystore.p12${NC}"
    read -p "Do you want to regenerate it? (y/N): " confirm
    if [[ ! $confirm =~ ^[Yy]$ ]]; then
        echo "Keeping existing keystore."
        exit 0
    fi
    rm "$KEYSTORE_DIR/keystore.p12"
fi

echo -e "${YELLOW}Generating keystore.p12...${NC}"

# Generate the keystore
keytool -genkeypair \
    -alias springboot \
    -keyalg RSA \
    -keysize 2048 \
    -storetype PKCS12 \
    -keystore "$KEYSTORE_DIR/keystore.p12" \
    -validity 365 \
    -storepass "$SSL_KEYSTORE_PASSWORD" \
    -keypass "$SSL_KEYSTORE_PASSWORD" \
    -dname "CN=localhost, OU=QuickClock, O=Development, L=Local, ST=Dev, C=US" \
    -ext "SAN=DNS:localhost,DNS:backend,IP:127.0.0.1"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Keystore generated successfully!${NC}"
    echo -e "${GREEN}  Location: $KEYSTORE_DIR/keystore.p12${NC}"
    echo ""
    echo -e "${YELLOW}Next steps:${NC}"
    echo -e "  1. Open the backend folder in IntelliJ IDEA"
    echo -e "  2. Configure run configuration with .env file"
    echo -e "  3. Run the application"
    echo ""
    echo -e "  See SETUP.md for detailed instructions"
else
    echo -e "${RED}✗ Failed to generate keystore${NC}"
    exit 1
fi

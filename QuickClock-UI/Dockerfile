# --- Stage 1: Build ---
FROM node:20-alpine AS builder
WORKDIR /app

# Copy package files and install deps
COPY package*.json ./
RUN npm ci

# Copy source and build
COPY . .
RUN npm run build
RUN ls -R /app/dist

# --- Stage 2: Serve ---
FROM nginx:alpine
COPY --from=builder /app/dist/quick-clock/browser /usr/share/nginx/html

# Copy a custom nginx config to handle Angular routing (SPA)
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
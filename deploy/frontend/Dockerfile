FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY . .
RUN npx ng build --configuration=production

FROM nginx:alpine AS runtime
RUN apk add --no-cache gettext
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY public/runtime-config.js /usr/share/nginx/html/runtime-config.js
EXPOSE 80
CMD ["/bin/sh", "-c", "export API_URL=\"${API_URL:-}\" && export BACKEND_HOST=\"${BACKEND_HOST:-backend}\" && mkdir -p /tmp/nginx && cp /etc/nginx/nginx.conf /tmp/nginx/nginx.conf && cp -r /etc/nginx/conf.d /tmp/nginx/ && sed -i 's|include /etc/nginx/conf.d/\\*\\.conf;|include /tmp/nginx/conf.d/*.conf;|' /tmp/nginx/nginx.conf && if [ -f /tmp/nginx/conf.d/gateway.conf ]; then envsubst '${BACKEND_HOST}' < /tmp/nginx/conf.d/gateway.conf > /tmp/nginx/conf.d/gateway.conf.tmp && mv /tmp/nginx/conf.d/gateway.conf.tmp /tmp/nginx/conf.d/gateway.conf; fi && envsubst < /usr/share/nginx/html/runtime-config.js > /tmp/runtime-config.js && cp /tmp/runtime-config.js /usr/share/nginx/html/runtime-config.js && nginx -c /tmp/nginx/nginx.conf -g 'daemon off;'"]

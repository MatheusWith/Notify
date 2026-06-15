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
CMD ["/bin/sh", "-c", "envsubst < /usr/share/nginx/html/runtime-config.js > /tmp/runtime-config.js && cp /tmp/runtime-config.js /usr/share/nginx/html/runtime-config.js && nginx -g 'daemon off;'"]

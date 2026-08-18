FROM --platform=$BUILDPLATFORM node:26.7.0-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci
COPY . .
RUN npm run build

FROM nginx:1.31.3-alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
RUN nginx -t && chown -R nginx:nginx /usr/share/nginx/html /var/cache/nginx /var/run
USER 101:101
EXPOSE 8080

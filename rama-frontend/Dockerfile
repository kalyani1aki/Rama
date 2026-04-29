# Stage 1: Build
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve with nginx
# nginx.conf uses ${BACKEND_URL} which is substituted at startup via envsubst.
# The official nginx image processes *.template files in /etc/nginx/templates/
# using only variables that are present in the container environment, so nginx
# variables like $host and $remote_addr are left untouched.
FROM nginx:alpine
COPY --from=build /app/dist/rama-frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80

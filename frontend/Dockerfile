# GAIN-Z 프론트 이미지 (#176)
# 1단계: Vite로 정적 파일을 만든다
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# 백엔드 주소. 비워 두면(기본) 같은 도메인으로 API를 부르고, 아래 nginx가 /api 를 백엔드로 넘긴다
ARG VITE_API_BASE_URL=
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# 2단계: nginx가 정적 파일을 서빙하고 API만 백엔드로 프록시한다
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80

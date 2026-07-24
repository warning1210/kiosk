# 빌드 스테이지 - vite build 결과(dist)만 다음 스테이지로 넘긴다.
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 실행 스테이지 - 정적 파일을 nginx로 서빙. /api, /uploads는 백엔드 EC2로 리버스 프록시
# (로컬 개발에서 vite.config.js가 하던 프록시 역할을 배포 환경에서는 nginx가 대신한다).
# templates/*.template에 넣으면 nginx 공식 이미지의 entrypoint가 컨테이너 시작 시 자동으로
# envsubst를 돌려 BACKEND_HOST를 실제 값으로 치환한 뒤 conf.d/에 써준다 (별도 스크립트 불필요).
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80

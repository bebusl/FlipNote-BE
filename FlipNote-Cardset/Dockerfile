# 1단계: 빌드 스테이지
FROM node:22-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .
RUN npm run build

# 2단계: 런타임 스테이지
FROM node:22-alpine

WORKDIR /app

COPY package*.json ./
RUN npm install --only=production

COPY --from=builder /app/dist ./dist

EXPOSE 8085
EXPOSE 9095

CMD ["node", "dist/main.js"]

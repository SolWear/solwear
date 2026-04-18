FROM node:20-alpine AS deps
RUN apk add --no-cache python3 make g++ libc6-compat
WORKDIR /app
COPY package.json package-lock.json* ./
RUN npm install --no-audit --no-fund

FROM node:20-alpine AS builder
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

FROM node:20-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production
ENV SOLWEAR_DATA_DIR=/data
ENV PORT=3000
RUN addgroup -S solwear && adduser -S solwear -G solwear \
    && mkdir -p /data && chown -R solwear:solwear /data
COPY --from=builder --chown=solwear:solwear /app/public ./public
COPY --from=builder --chown=solwear:solwear /app/.next ./.next
COPY --from=builder --chown=solwear:solwear /app/node_modules ./node_modules
COPY --from=builder --chown=solwear:solwear /app/package.json ./package.json
USER solwear
EXPOSE 3000
VOLUME ["/data"]
CMD ["npm", "run", "start"]

FROM node:24-bookworm-slim@sha256:01d566e8d94d8c619591d78652babfccd3c6992a3dffa9ed5ab39e11cf6c70e9 AS base
ENV NEXT_TELEMETRY_DISABLED=1
WORKDIR /app

FROM base AS pnpm
RUN npm install --global pnpm@12.11.2
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

FROM pnpm AS build
RUN pnpm install --frozen-lockfile
COPY . .
RUN pnpm build

FROM pnpm AS production-deps
RUN pnpm install --frozen-lockfile --prod

FROM base
ENV NODE_ENV=production
COPY --from=production-deps /app/node_modules ./node_modules
COPY --from=build /app/.next ./.next
COPY --from=build /app/next.config.ts /app/package.json ./
USER node
EXPOSE 3000
CMD ["node_modules/.bin/next", "start"]

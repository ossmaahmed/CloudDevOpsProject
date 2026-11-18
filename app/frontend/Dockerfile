FROM node:22.11.0-bookworm-slim

WORKDIR /app

RUN apt-get update \
    && apt-get upgrade -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

COPY package*.json ./
RUN npm install --omit=dev --no-fund --no-audit

COPY . .

EXPOSE 3000

CMD ["npm", "start"]

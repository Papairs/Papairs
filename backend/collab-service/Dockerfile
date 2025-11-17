FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --production

# Copy application files
COPY . .

# Expose ports for WebSocket and HTTP API
EXPOSE 3004 3005

# Start the service
CMD ["node", "src/index.js"]

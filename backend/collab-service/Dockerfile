FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install --production

# Copy application files
COPY . .

# Expose port for WebSocket
EXPOSE 3004

# Start the service
CMD ["node", "index.js"]

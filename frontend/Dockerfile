# Development stage for Vue.js frontend with Vite
FROM node:18-alpine

WORKDIR /app

# Copy package files
COPY package*.json ./

# Install dependencies
RUN npm install

# Copy application source
COPY . .

# Expose port 3000
EXPOSE 3000

# Start Vite development server using npx to avoid path issues
CMD ["npx", "vite", "--host", "0.0.0.0", "--port", "3000"]

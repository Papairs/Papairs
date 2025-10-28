# AI Service

AI-powered autocomplete service for Papairs using OpenAI GPT-4o.

## Setup

1. Install dependencies:
   ```bash
   npm install
   ```

2. Create `.env` file:
   ```bash
   cp .env.example .env
   ```

3. Add your OpenAI API key to `.env`:
   ```
   OPENAI_API_KEY=sk-your-actual-key-here
   ```

4. Start the service:
   ```bash
   npm start
   ```
   
   Or for development with auto-reload:
   ```bash
   npm run dev
   ```

## Usage

The service runs on `http://localhost:3001` by default.

**POST /autocomplete**
```json
{
  "userInput": "The quick brown"
}
```

Response:
```json
{
  "suggestion": "fox jumps over the lazy dog"
}
```

## Configuration

- Edit `prompt.txt` to customize the system prompt for completions
- Modify `PORT` in `.env` to change the server port
- Adjust `max_tokens` and `temperature` in `index.js` for different completion styles

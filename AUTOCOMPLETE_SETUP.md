# Autocomplete Feature Setup

## What's Been Created

### Frontend (Vue.js)
- **View**: `/frontend/src/views/AutocompleteView.vue`
  - Clean, simple design matching your app's style
  - Real-time autocomplete with 1-second debounce
  - Tab key to accept suggestions
  - Ghost text overlay showing suggestions

### Backend (Node.js AI Service)
- **Location**: `/backend/ai-service/`
- **Features**:
  - Express.js server on port 3001
  - OpenAI GPT-4o integration
  - Customizable system prompt via `prompt.txt`
  - CORS enabled for frontend communication

## Setup Instructions

### 1. Set up AI Service

```bash
cd backend/ai-service
npm install
```

### 2. Configure OpenAI API Key

Create `.env` file:
```bash
cp .env.example .env
```

Edit `.env` and add your OpenAI API key:
```
OPENAI_API_KEY=sk-your-actual-openai-key-here
PORT=3001
```

### 3. Run the Services

**Option A: Run AI service standalone (for testing)**
```bash
cd backend/ai-service
npm start
```

**Option B: Run all services with Docker Compose**
```bash
# From project root
docker-compose up ai-service frontend
```

### 4. Access the Autocomplete Feature

Open your browser and navigate to:
```
http://localhost:3000/autocomplete
```

## How It Works

1. User types in the textarea
2. After 1 second of inactivity, sends request to AI service
3. AI service uses OpenAI to generate completion
4. Suggestion appears as ghost text
5. Press Tab to accept the suggestion

## Customization

### Change the AI Behavior
Edit `/backend/ai-service/prompt.txt` to modify how completions are generated.

### Adjust Timing
In `AutocompleteView.vue`, change the timeout value (currently 1000ms):
```javascript
timer = setTimeout(() => {
  fetchSuggestion(newValue)
}, 1000)  // <- Change this value
```

### Style Adjustments
The component uses Tailwind CSS classes matching your app's design system. Modify classes in the template section as needed.

## Testing

1. Start the AI service
2. Navigate to `/autocomplete`
3. Start typing (e.g., "The quick brown")
4. Wait 1 second for suggestion
5. Press Tab to accept

## Troubleshooting

**No suggestions appearing?**
- Check that AI service is running on port 3001
- Verify OpenAI API key is set correctly in `.env`
- Check browser console for errors

**CORS errors?**
- Ensure AI service has CORS enabled (already configured)
- Check that frontend is accessing `http://localhost:3001`

**Docker issues?**
- Make sure `.env` file exists in `/backend/ai-service/`
- Verify OpenAI API key is in the `.env` file

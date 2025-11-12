require('dotenv').config();
const express = require('express');
const cors = require('cors');
const OpenAI = require('openai');
const fs = require('fs').promises;
const path = require('path');

const app = express();
const port = process.env.PORT || 3001;

app.use(cors());
app.use(express.json());

// Cache for system prompt to avoid file I/O on every request
let systemPromptCache = null;
const PROMPT_CACHE_TTL = 5 * 60 * 1000; // 5 minutes
let promptCacheTime = 0;

// Simple response cache to avoid duplicate OpenAI calls
const responseCache = new Map();
const RESPONSE_CACHE_TTL = 2 * 60 * 1000; // 2 minutes
const MAX_CACHE_SIZE = 100;

// Configure OpenAI with timeout
const openai = new OpenAI({ 
  apiKey: process.env.OPENAI_API_KEY,
  timeout: 10000, // 10 second timeout
  maxRetries: 2
});

// Load and cache system prompt
async function getSystemPrompt() {
  const now = Date.now();
  
  // Return cached prompt if still valid
  if (systemPromptCache && (now - promptCacheTime) < PROMPT_CACHE_TTL) {
    return systemPromptCache;
  }

  try {
    const promptPath = path.join(process.cwd(), 'prompt.txt');
    systemPromptCache = (await fs.readFile(promptPath, 'utf8')).trim();
    
    if (!systemPromptCache) {
      systemPromptCache = 'You are a helpful assistant that completes the user input concisely.';
    }
    
    promptCacheTime = now;
    console.log('System prompt loaded and cached');
    return systemPromptCache;
  } catch (readErr) {
    console.error('Unable to read prompt.txt, using fallback system prompt:', readErr);
    systemPromptCache = 'You are a helpful assistant that completes the user input concisely.';
    promptCacheTime = now;
    return systemPromptCache;
  }
}

// Generate cache key for request
function getCacheKey(userInput) {
  return userInput.trim().toLowerCase();
}

// Clean old cache entries
function cleanCache() {
  const now = Date.now();
  for (const [key, value] of responseCache.entries()) {
    if (now - value.timestamp > RESPONSE_CACHE_TTL) {
      responseCache.delete(key);
    }
  }
  
  // Limit cache size
  if (responseCache.size > MAX_CACHE_SIZE) {
    const entries = Array.from(responseCache.entries());
    entries.sort((a, b) => a[1].timestamp - b[1].timestamp);
    const toDelete = entries.slice(0, responseCache.size - MAX_CACHE_SIZE + 10);
    toDelete.forEach(([key]) => responseCache.delete(key));
  }
}

app.post('/autocomplete', async (req, res) => {
  const { userInput } = req.body;

  if (!userInput || typeof userInput !== 'string') {
    return res.status(400).json({ error: 'Invalid userInput provided' });
  }

  const startTime = Date.now();

  try {
    // Check cache first
    const cacheKey = getCacheKey(userInput);
    const cached = responseCache.get(cacheKey);
    const now = Date.now();
    
    if (cached && (now - cached.timestamp) < RESPONSE_CACHE_TTL) {
      console.log(`Cache hit for "${userInput}" (${now - startTime}ms)`);
      return res.json({ suggestion: cached.suggestion });
    }

    // Get system prompt (cached)
    const systemPrompt = await getSystemPrompt();

    // Call OpenAI API
    const completion = await openai.chat.completions.create({
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userInput }
      ],
      model: 'gpt-4o-mini', // Use faster model
      max_tokens: 50, // Limit response length for faster processing
      temperature: 0.7
    });
    
    let suggestionText = completion.choices[0].message.content.trim();
    
    // Remove surrounding quotes if present
    if ((suggestionText.startsWith('"') && suggestionText.endsWith('"')) ||
        (suggestionText.startsWith("'") && suggestionText.endsWith("'"))) {
      suggestionText = suggestionText.slice(1, -1).trim();
    }

    // Cache the response
    responseCache.set(cacheKey, {
      suggestion: suggestionText,
      timestamp: now
    });

    // Clean cache periodically
    if (Math.random() < 0.1) { // 10% chance to clean cache
      cleanCache();
    }

    const duration = Date.now() - startTime;
    console.log(`AI completion for "${userInput}" took ${duration}ms`);
    
    res.json({ suggestion: suggestionText });
  } catch (error) {
    const duration = Date.now() - startTime;
    console.error(`Error fetching autocompletion after ${duration}ms:`, error.message);
    
    if (error.message.includes('timeout')) {
      res.status(504).json({ error: 'AI service timeout - please try again' });
    } else {
      res.status(500).json({ error: 'Failed to fetch autocompletion' });
    }
  }
});

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'healthy', 
    service: 'ai-service',
    cache: {
      responseCache: responseCache.size,
      promptCached: !!systemPromptCache
    }
  });
});

const server = app.listen(port, () => {
  console.log(`AI Service is running on http://localhost:${port}`);
});

// Graceful shutdown handler
function gracefulShutdown(signal) {
  console.log(`\n${signal} received. Starting graceful shutdown...`);
  
  server.close(() => {
    console.log('HTTP server closed');
    process.exit(0);
  });

  // Force shutdown after 5 seconds if graceful shutdown fails
  setTimeout(() => {
    console.error('Forced shutdown after timeout');
    process.exit(1);
  }, 5000);
}

// Handle shutdown signals
process.on('SIGTERM', () => gracefulShutdown('SIGTERM'));
process.on('SIGINT', () => gracefulShutdown('SIGINT'));

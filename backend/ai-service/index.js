const express = require('express');
const cors = require('cors');
const { OpenAI } = require('openai');
const fs = require('fs');
const path = require('path');
const fastMode = require('./services/fastMode');
const advancedMode = require('./services/advancedMode');

const app = express();
const PORT = process.env.PORT || 3001;
const DOCS_SERVICE_URL = 'http://docs-service:8082';
const TIMEOUT = 30000;

app.use(cors({
  origin: ['http://localhost:3000', 'http://localhost:8080'],
  credentials: true
}));
app.use(express.json());

const openai = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

function loadSystemPrompt() {
  const defaultPrompt = 'You are a helpful AI assistant providing code autocompletion suggestions. When files are provided, analyze their content to give context-aware suggestions.';
  try {
    const promptPath = path.join(__dirname, 'prompt.txt');
    if (fs.existsSync(promptPath)) {
      const prompt = fs.readFileSync(promptPath, 'utf-8');
      console.log('Loaded custom system prompt from prompt.txt');
      return prompt;
    }
  } catch (error) {
    console.error('Failed to load prompt.txt, using default:', error.message);
  }
  return defaultPrompt;
}

const systemPrompt = loadSystemPrompt();

app.get('/health', (req, res) => {
  res.json({ status: 'healthy', service: 'AI Service' });
});

app.post('/autocomplete', async (req, res) => {
  try {
    const { prompt, userInput, mode = 'fast' } = req.body;
    const userId = req.headers['X-User-Id'];
    const inputText = prompt || userInput;

    if (!inputText || typeof inputText !== 'string' || inputText.trim() === '') {
      return res.status(400).json({ error: 'Prompt is required and must be a non-empty string' });
    }

    const startTime = Date.now();
    console.log(`[${mode.toUpperCase()}] Request: ${inputText.substring(0, 50)}...`);

    let suggestion;
    
    if (mode === 'advanced') {
      suggestion = await Promise.race([
        advancedMode.handleRequest(openai, systemPrompt, DOCS_SERVICE_URL, req, userId),
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error('Advanced mode timeout')), 120000)
        )
      ]);
    } else {
      suggestion = await Promise.race([
        fastMode.handleRequest(openai, systemPrompt, DOCS_SERVICE_URL, req, userId),
        new Promise((_, reject) => 
          setTimeout(() => reject(new Error('Fast mode timeout')), TIMEOUT)
        )
      ]);
    }

    const elapsed = Date.now() - startTime;
    console.log(`[${mode.toUpperCase()}] Response in ${elapsed}ms`);

    res.json({ suggestion });
  } catch (error) {
    console.error('Error:', error.message);
    res.status(500).json({ error: error.message });
  }
});

// Generate flashcards from document content
app.post('/generate-flashcards', async (req, res) => {
  const { content, numberOfCards = 5 } = req.body;

  if (!content || typeof content !== 'string' || content.trim().length < 50) {
    return res.status(400).json({ error: 'Content must be at least 50 characters' });
  }

  const startTime = Date.now();

  try {
    const systemPrompt = `You are an expert at creating educational flashcards. Generate ${numberOfCards} high-quality flashcards from the provided content. Each flashcard should have a clear question and a concise answer. Return ONLY a JSON array with this exact format:
[
  {"question": "Question text here?", "answer": "Answer text here"},
  {"question": "Another question?", "answer": "Another answer"}
]
Do not include any other text, explanations, or markdown formatting.`;

    const completion = await openai.chat.completions.create({
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: content }
      ],
      model: 'gpt-4o-mini',
      max_tokens: 1000,
      temperature: 0.7
    });
    
    let responseText = completion.choices[0].message.content.trim();
    
    // Remove markdown code blocks if present
    responseText = responseText.replace(/```json\s*/g, '').replace(/```\s*/g, '');
    
    // Parse the JSON response
    const flashcards = JSON.parse(responseText);
    
    if (!Array.isArray(flashcards)) {
      throw new Error('Response is not an array');
    }

    const duration = Date.now() - startTime;
    console.log(`Generated ${flashcards.length} flashcards in ${duration}ms`);
    
    res.json({ flashcards });
  } catch (error) {
    const duration = Date.now() - startTime;
    console.error(`Error generating flashcards after ${duration}ms:`, error.message);
    
    if (error.message.includes('timeout')) {
      res.status(504).json({ error: 'AI service timeout - please try again' });
    } else {
      res.status(500).json({ error: 'Failed to generate flashcards', details: error.message });
    }
  }
});

app.listen(PORT, () => {
  console.log(`AI Service is running on http://localhost:${PORT}`);
});

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

const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

app.post('/autocomplete', async (req, res) => {
  const { userInput } = req.body;

  try {
    const promptPath = path.join(process.cwd(), 'prompt.txt');
    let systemPrompt;
    
    try {
      systemPrompt = (await fs.readFile(promptPath, 'utf8')).trim();
      if (!systemPrompt) {
        systemPrompt = 'You are a helpful assistant that completes the user input concisely.';
      }
    } catch (readErr) {
      console.error('Unable to read prompt.txt, using fallback system prompt:', readErr);
      systemPrompt = 'You are a helpful assistant that completes the user input concisely.';
    }

    const completion = await openai.chat.completions.create({
      messages: [
        { role: 'system', content: systemPrompt },
        { role: 'user', content: userInput }
      ],
      model: 'gpt-4o',
      max_tokens: 30,
      temperature: 0.3,
    });
    
    let suggestionText = completion.choices[0].message.content.trim();
    
    // Remove surrounding quotes if present
    if ((suggestionText.startsWith('"') && suggestionText.endsWith('"')) ||
        (suggestionText.startsWith("'") && suggestionText.endsWith("'"))) {
      suggestionText = suggestionText.slice(1, -1).trim();
    }
    
    res.json({ suggestion: suggestionText });
  } catch (error) {
    console.error('Error fetching autocompletion:', error);
    res.status(500).json({ error: 'Failed to fetch autocompletion' });
  }
});

app.listen(port, () => {
  console.log(`AI Service is running on http://localhost:${port}`);
});

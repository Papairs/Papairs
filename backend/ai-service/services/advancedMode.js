/**
 * Advanced Mode - Assistants API with File Search
 * Slower but supports all file types, semantic search, and better accuracy
 * OpenAI handles all file parsing automatically
 */

const axios = require('axios');

const DOWNLOAD_TIMEOUT = 30000;
const POLL_INTERVAL = 1000;

// Caches
const userAssistants = new Map();
const uploadedFiles = new Map();
const userVectorStores = new Map();
const userThreads = new Map();

async function getOrCreateAssistant(openai, userId, systemPrompt) {
  if (userAssistants.has(userId)) {
    return userAssistants.get(userId);
  }

  const assistant = await openai.beta.assistants.create({
    name: `Papairs Assistant for ${userId}`,
    instructions: systemPrompt,
    model: 'gpt-4o',
    tools: [{ type: 'file_search' }],
  });

  userAssistants.set(userId, assistant.id);
  console.log(`Created assistant ${assistant.id} for user ${userId}`);
  return assistant.id;
}

async function getOrCreateVectorStore(openai, userId) {
  if (userVectorStores.has(userId)) {
    return userVectorStores.get(userId);
  }

  const vectorStore = await openai.vectorStores.create({
    name: `${userId}_files`
  });

  userVectorStores.set(userId, vectorStore.id);
  console.log(`Created vector store ${vectorStore.id} for user ${userId}`);
  return vectorStore.id;
}

async function getOrCreateThread(openai, userId) {
  if (userThreads.has(userId)) {
    return userThreads.get(userId);
  }

  const thread = await openai.beta.threads.create();
  userThreads.set(userId, thread.id);
  console.log(`Created thread ${thread.id} for user ${userId}`);
  return thread.id;
}

async function uploadFileToOpenAI(openai, fileId, userId, fileName, docsServiceUrl) {
  if (uploadedFiles.has(fileId)) {
    return uploadedFiles.get(fileId);
  }

  const downloadUrl = `${docsServiceUrl}/api/files/download/${fileId}`;
  const response = await axios.get(downloadUrl, {
    responseType: 'arraybuffer',
    timeout: DOWNLOAD_TIMEOUT,
    headers: { 'X-User-Id': userId }
  });

  const buffer = Buffer.from(response.data);
  const { toFile } = require('openai');
  
  const file = await openai.files.create({
    file: await toFile(buffer, fileName),
    purpose: 'assistants'
  });

  uploadedFiles.set(fileId, file.id);
  console.log(`  Uploaded ${fileName} to OpenAI: ${file.id}`);
  return file.id;
}

async function waitForRunCompletion(openai, threadId, runId, timeout = 120000) {
  let run = await openai.beta.threads.runs.retrieve(threadId, runId);
  const startTime = Date.now();

  while (run.status === 'queued' || run.status === 'in_progress') {
    if (Date.now() - startTime > timeout) {
      throw new Error('Assistant run timeout');
    }
    await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL));
    run = await openai.beta.threads.runs.retrieve(threadId, runId);
  }

  if (run.status === 'failed') {
    throw new Error(`Run failed: ${run.last_error?.message || 'Unknown error'}`);
  }

  return run;
}

async function handleRequest(openai, systemPrompt, docsServiceUrl, req, userId) {
  const { prompt, userInput, selectedFiles = [] } = req.body;
  const inputText = prompt || userInput;

  if (!userId) {
    throw new Error('X-User-Id header is required for advanced mode');
  }

  const assistantId = await getOrCreateAssistant(openai, userId, systemPrompt);

  // Upload files if provided
  let vectorStoreId = null;
  if (selectedFiles.length > 0) {
    console.log(`Processing ${selectedFiles.length} files`);
    vectorStoreId = await getOrCreateVectorStore(openai, userId);
    
    const openAIFileIds = [];
    for (const file of selectedFiles) {
      const fileName = file.filename || file.name || file.fileName;
      const fileId = file.fileId;
      
      try {
        const openAIFileId = await uploadFileToOpenAI(openai, fileId, userId, fileName, docsServiceUrl);
        openAIFileIds.push(openAIFileId);
      } catch (error) {
        console.error(`Failed to upload ${fileName}:`, error.message);
      }
    }
    
    if (openAIFileIds.length > 0) {
      await openai.vectorStores.fileBatches.createAndPoll(vectorStoreId, {
        file_ids: openAIFileIds
      });
      console.log(`  Added ${openAIFileIds.length} files to vector store`);
    }
  }

  const threadId = await getOrCreateThread(openai, userId);
  
  await openai.beta.threads.messages.create(threadId, {
    role: 'user',
    content: inputText
  });

  const runParams = {
    assistant_id: assistantId,
    max_prompt_tokens: 20000,
    max_completion_tokens: 500,
    temperature: 0.3,
  };

  if (vectorStoreId) {
    runParams.tools = [{ type: 'file_search' }];
    runParams.tool_resources = {
      file_search: {
        vector_store_ids: [vectorStoreId]
      }
    };
  }

  const run = await openai.beta.threads.runs.create(threadId, runParams);
  await waitForRunCompletion(openai, threadId, run.id);

  const messages = await openai.beta.threads.messages.list(threadId);
  const assistantMessage = messages.data.find(m => m.role === 'assistant');
  
  if (!assistantMessage || !assistantMessage.content[0]) {
    throw new Error('No response from assistant');
  }

  return assistantMessage.content[0].text.value;
}

module.exports = { handleRequest };

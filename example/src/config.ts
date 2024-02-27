const config = {
  GEMINI_API_KEY: 'YOUR_API_KEY',
  DEFAULT_MODEL_NAME: 'gemini-pro',
  generativeConfig: {
    temperature: 0.9,
    topP: 0.1,
    topK: 16,
    maxOutputTokens: 2000,
    stopSequences: ['red'],
  },
};

export default config;

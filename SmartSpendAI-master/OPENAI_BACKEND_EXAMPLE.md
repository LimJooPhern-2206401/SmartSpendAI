# OpenAI Backend Setup

This Android app is now prepared to send the user's budgeting question and monthly budget summary to your own backend.

Do not put your OpenAI API key inside the Android app.

## 1. Add your backend URL to the Android app

Open:

`app/build.gradle`

Replace this line:

`buildConfigField "String", "OPENAI_PROXY_URL", "\"\""`

with your backend URL, for example:

`buildConfigField "String", "OPENAI_PROXY_URL", "\"https://your-domain.com/api/ai-advice\""`

For local Android emulator testing, you can use:

`http://10.0.2.2:3000/api/ai-advice`

## 2. Create a small Node.js backend

Create a folder like `openai-backend` outside the Android app and add these files.

### package.json

```json
{
  "name": "smartspend-openai-backend",
  "version": "1.0.0",
  "type": "module",
  "main": "server.js",
  "scripts": {
    "start": "node server.js"
  },
  "dependencies": {
    "express": "^4.21.2",
    "openai": "^4.100.0"
  }
}
```

### server.js

```js
import express from "express";
import OpenAI from "openai";

const app = express();
app.use(express.json());

const client = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY
});

app.post("/api/ai-advice", async (req, res) => {
  try {
    const { question, month, year, income, totalAllocated, totalSpent, unallocated, categories } = req.body;

    const response = await client.responses.create({
      model: "gpt-5",
      input: [
        {
          role: "system",
          content: "You are a helpful budgeting assistant for students. Give practical, concise, supportive financial guidance based on the provided monthly budget data."
        },
        {
          role: "user",
          content:
            `Question: ${question}\n` +
            `Month: ${month}/${year}\n` +
            `Income: ${income}\n` +
            `Allocated: ${totalAllocated}\n` +
            `Spent: ${totalSpent}\n` +
            `Unallocated: ${unallocated}\n` +
            `Categories: ${JSON.stringify(categories)}`
        }
      ]
    });

    res.json({
      answer: response.output_text
    });
  } catch (error) {
    res.status(500).json({
      error: error?.message || "Failed to get AI response."
    });
  }
});

app.listen(3000, () => {
  console.log("SmartSpend OpenAI backend running on port 3000");
});
```

## 3. Where to insert your API key

Set your key as an environment variable on the backend machine:

### Windows PowerShell

```powershell
$env:OPENAI_API_KEY="your_openai_api_key_here"
node server.js
```

### macOS / Linux

```bash
export OPENAI_API_KEY="your_openai_api_key_here"
node server.js
```

## 4. Important notes

- Keep the API key on the backend only.
- The Android app should only call your backend URL.
- If you deploy the backend publicly, add authentication and rate limiting.

# Direct OpenAI Setup For Demo Use

This version calls OpenAI directly from the Android app.

## Where to get your OpenAI API key

OpenAI API keys are created in the OpenAI developer platform:

- https://platform.openai.com/api-keys
- Help article: https://help.openai.com/en/articles/4936850-how-to-create-and-use-an-api-key
- Responses API docs: https://platform.openai.com/docs/api-reference/responses

## Where to paste the key

Open `app/build.gradle` and replace:

`buildConfigField "String", "OPENAI_API_KEY", "\"PASTE_YOUR_OPENAI_API_KEY_HERE\""`

with your real key.

You can also change the model here:

`buildConfigField "String", "OPENAI_MODEL", "\"gpt-5\""`

## Important

This is okay for a demo or learning project, but it is not secure for a production app because the key can be extracted from the APK.

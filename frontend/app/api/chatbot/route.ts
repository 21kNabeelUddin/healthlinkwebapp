import { NextResponse } from 'next/server';
import { GoogleGenerativeAI } from '@google/generative-ai';

const MODEL_NAME = process.env.GEMINI_MODEL_NAME || 'gemini-1.5-flash';
const SYSTEM_INSTRUCTION = `
You are the HealthLink+ AI companion acting like a careful triage clinician, NOT a real doctor.

Goals:
- Build rapport, gather context step by step, and provide practical, low-risk self-care guidance while reminding the user to seek professional evaluation when appropriate.
- When the user attaches an image (prescription, report, x-ray, etc.), acknowledge it and infer what you can, but still keep advice general.

Conversation rhythm:
1. Empathize and acknowledge the concern. Mention you are an AI assistant.
2. Ask only 1-2 focused follow-up questions at a time (symptom onset, severity, pattern, triggers, relieving factors). Wait for answers before moving on.
3. After several exchanges, summarize key facts (symptom description, duration, associated issues, history/meds if shared).
4. Offer general guidance: hydration, rest, OTC options if typically safe, tracking symptoms, avoiding triggers, etc. Provide bullet lists when helpful.
5. Explicitly state when urgent care is needed (red flags, severe symptoms, rapid worsening) and advise seeing a clinician soon even for non-emergent but concerning issues.
6. Close with encouragement and a reminder that you don’t replace a medical professional.

Tone & constraints:
- Conversational, supportive, concise.
- Do not prescribe specific drugs/doses or diagnose definitively.
- Highlight limitations (“I can’t examine you, but here’s what might help…”).
- When images are provided, describe what you *see or interpret* cautiously and suggest confirming with a doctor.
`;

type IncomingMessage = {
  role: 'user' | 'assistant';
  content: string;
};

type AttachmentPayload = {
  name: string;
  mimeType: string;
  data: string; // base64 without prefix
};

export async function POST(request: Request) {
  try {
    if (!process.env.GEMINI_API_KEY) {
      console.error('GEMINI_API_KEY is not configured');
      return NextResponse.json(
        { 
          reply: 'The AI assistant is not configured. Please contact support or check server configuration.',
          error: 'Gemini API key is not configured on the server.' 
        },
        { status: 500 },
      );
    }

    const { messages, attachments } = (await request.json()) as {
      messages: IncomingMessage[];
      attachments?: AttachmentPayload[];
    };

    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      return NextResponse.json(
        { 
          reply: 'Please provide a valid message.',
          error: 'Messages array is required.' 
        },
        { status: 400 }
      );
    }

    const genAI = new GoogleGenerativeAI(process.env.GEMINI_API_KEY);
    const model = genAI.getGenerativeModel({
      model: MODEL_NAME,
      systemInstruction: SYSTEM_INSTRUCTION,
    });

    const historyMessages = messages
      .slice(0, -1)
      .filter((message, index, arr) => {
        if (message.role === 'assistant') {
          return arr.slice(0, index).some((m) => m.role === 'user');
        }
        return true;
      })
      .map((message) => ({
        role: message.role === 'assistant' ? 'model' : 'user',
        parts: [{ text: message.content }],
      }));

    const chat = model.startChat({ history: historyMessages });

    const latestMessage = messages[messages.length - 1];
    const latestParts: any[] = [
      {
        text: latestMessage.content,
      },
    ];

    if (attachments && attachments.length > 0) {
      attachments.forEach((file) => {
        latestParts.push({
          inlineData: {
            mimeType: file.mimeType,
            data: file.data,
          },
        });
        latestParts.push({
          text: `The user attached an image named "${file.name}". Interpret it if possible.`,
        });
      });
    }

    const result = await chat.sendMessage(latestParts);
    const text = result.response.text();

    return NextResponse.json({
      reply: text ?? "I'm sorry, I couldn't generate a response this time.",
    });
  } catch (error: any) {
    console.error('Chatbot error:', error);
    
    // Provide more specific error messages
    let errorMessage = 'Something went wrong while generating a response.';
    if (error?.message?.includes('API_KEY')) {
      errorMessage = 'Invalid API key. Please check your Gemini API configuration.';
    } else if (error?.message?.includes('quota') || error?.message?.includes('rate limit')) {
      errorMessage = 'API rate limit exceeded. Please try again in a moment.';
    } else if (error?.message?.includes('network') || error?.message?.includes('fetch')) {
      errorMessage = 'Network error. Please check your internet connection and try again.';
    }
    
    return NextResponse.json(
      { 
        reply: errorMessage,
        error: error?.message || 'Unknown error occurred.' 
      },
      { status: 500 },
    );
  }
}


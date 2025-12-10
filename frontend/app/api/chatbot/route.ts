import { NextResponse } from 'next/server';

const GROQ_API_URL = 'https://api.groq.com/openai/v1/chat/completions';
const MODEL_NAME = process.env.GROQ_MODEL_NAME || 'llama-3.1-8b-instant';
const BACKEND_API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

type ConversationState = 'INITIAL' | 'ACTIVE' | 'RESOLVING' | 'CLOSING' | 'ENDED';

type IncomingMessage = {
  role: 'user' | 'assistant';
  content: string;
};

type AttachmentPayload = {
  name: string;
  mimeType: string;
  data: string; // base64 without prefix
};

type ConsentState = {
  prescriptions: boolean;
  history: boolean;
};

// Base system prompt - core instructions
const BASE_SYSTEM_PROMPT = `You are the HealthLink+ AI companion, acting as a careful triage assistant.

[ROLE DEFINITION]
You are NOT a doctor. You cannot diagnose or prescribe. You provide general guidance only. Always recommend professional evaluation when appropriate.

[CONVERSATION STYLE]
- Empathetic and supportive
- Ask 1-2 questions at a time (don't overwhelm)
- Wait for answers before proceeding
- Use bullet points for clarity
- Be concise but thorough

[CONVERSATION MANAGEMENT]
- Track conversation state: INITIAL → ACTIVE → RESOLVING → CLOSING
- When you have enough information (after 3-5 exchanges), move to RESOLVING state
- In RESOLVING: Provide summary + guidance + next steps
- In CLOSING: Offer to help with something else
- End conversations gracefully when:
  * User says goodbye/thanks/that's all
  * Topic is resolved
  * User needs urgent care (provide clear escalation path)

[RED FLAGS - ESCALATE IMMEDIATELY]
- Severe pain (8+/10)
- Difficulty breathing
- Chest pain
- Loss of consciousness
- Severe allergic reactions
- Any life-threatening symptoms

When red flags detected:
1. Acknowledge urgency
2. Provide immediate action steps
3. Strongly recommend emergency care
4. End conversation with clear next steps

[CONTEXT AWARENESS]
- You have access to user's medical history (if consented)
- You have access to prescriptions (if consented)
- You have access to upcoming appointments
- Use this context to provide personalized responses
- Never share sensitive info unless user explicitly asks`;

// Build enhanced system prompt with context
function buildSystemPrompt(
  consent: ConsentState,
  context: {
    prescriptions?: any[];
    medicalHistory?: any[];
    appointments?: any[];
    patientName?: string;
  },
  conversationState: ConversationState
): string {
  let prompt = BASE_SYSTEM_PROMPT;

  // Add patient name if available
  if (context.patientName) {
    prompt += `\n\n[PATIENT NAME]\n${context.patientName}`;
  }

  // Add upcoming appointments (always available, non-sensitive)
  if (context.appointments && context.appointments.length > 0) {
    prompt += `\n\n[UPCOMING APPOINTMENTS]`;
    context.appointments.slice(0, 3).forEach((apt: any) => {
      const date = apt.appointmentDateTime ? new Date(apt.appointmentDateTime).toLocaleDateString() : 'N/A';
      const time = apt.appointmentDateTime ? new Date(apt.appointmentDateTime).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' }) : 'N/A';
      prompt += `\n- ${date} at ${time} with ${apt.doctorName || 'Doctor'} at ${apt.clinicName || 'Clinic'}`;
      if (apt.reason) prompt += `\n  Reason: ${apt.reason}`;
    });
  }

  // Add prescriptions if consented
  if (consent.prescriptions && context.prescriptions && context.prescriptions.length > 0) {
    prompt += `\n\n[PRESCRIPTIONS - Current Medications]`;
    context.prescriptions
      .filter((rx: any) => {
        // Only show active prescriptions (no end date or end date in future)
        if (!rx.endDate) return true;
        const endDate = new Date(rx.endDate);
        return endDate > new Date();
      })
      .slice(0, 10)
      .forEach((rx: any) => {
        prompt += `\n- ${rx.medications?.[0]?.name || 'Medication'}`;
        if (rx.medications?.[0]?.dosage) prompt += ` (${rx.medications[0].dosage})`;
        if (rx.medications?.[0]?.frequency) prompt += ` - ${rx.medications[0].frequency}`;
        if (rx.prescribedBy) prompt += `\n  Prescribed by: ${rx.prescribedBy}`;
      });
  }

  // Add medical history if consented
  if (consent.history && context.medicalHistory && context.medicalHistory.length > 0) {
    prompt += `\n\n[MEDICAL HISTORY]`;
    context.medicalHistory.slice(0, 10).forEach((record: any) => {
      prompt += `\n- ${record.condition || record.diagnosis || 'Condition'}`;
      if (record.date) {
        const date = new Date(record.date).toLocaleDateString();
        prompt += ` (${date})`;
      }
      if (record.status) prompt += ` - Status: ${record.status}`;
    });
  }

  // Add state-specific instructions
  prompt += `\n\n[CURRENT STATE: ${conversationState}]`;
  
  if (conversationState === 'INITIAL') {
    prompt += `\n[THIS TURN]: Greet the user warmly and ask how you can help.`;
  } else if (conversationState === 'ACTIVE') {
    prompt += `\n[THIS TURN]: Ask 1-2 clarifying questions. Don't provide guidance yet unless you have enough information (3-5 exchanges).`;
  } else if (conversationState === 'RESOLVING') {
    prompt += `\n[THIS TURN]: You have enough information. Provide:
1. Brief summary of what user shared
2. General guidance (3-4 bullet points)
3. When to see a doctor
4. What to monitor
Then ask: "Does this help, or do you have more questions?"`;
  } else if (conversationState === 'CLOSING') {
    prompt += `\n[THIS TURN]: User indicated they're done. Provide:
1. Brief summary
2. Next steps
3. Offer to help with something else
Keep it concise (2-3 sentences max).`;
  }

  return prompt;
}

// Fetch context from backend
async function fetchContext(
  patientId: string,
  consent: ConsentState,
  authToken: string
): Promise<{
  prescriptions?: any[];
  medicalHistory?: any[];
  appointments?: any[];
  patientName?: string;
}> {
  const context: any = {};

  try {
    // Always fetch appointments (non-sensitive)
    try {
      const appointmentsRes = await fetch(`${BACKEND_API_URL}/api/v1/appointments?status=SCHEDULED`, {
        headers: {
          'Authorization': `Bearer ${authToken}`,
          'Content-Type': 'application/json',
        },
      });
      if (appointmentsRes.ok) {
        const appointmentsData = await appointmentsRes.json();
        // Unwrap response envelope
        const appointments = appointmentsData.data || appointmentsData;
        context.appointments = Array.isArray(appointments) ? appointments : [];
      }
    } catch (e) {
      console.error('Failed to fetch appointments:', e);
    }

    // Fetch prescriptions if consented
    if (consent.prescriptions) {
      try {
        const prescriptionsRes = await fetch(`${BACKEND_API_URL}/api/v1/prescriptions/patient/${patientId}`, {
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json',
          },
        });
        if (prescriptionsRes.ok) {
          const prescriptionsData = await prescriptionsRes.json();
          const prescriptions = prescriptionsData.data || prescriptionsData;
          context.prescriptions = Array.isArray(prescriptions) ? prescriptions : [];
        }
      } catch (e) {
        console.error('Failed to fetch prescriptions:', e);
      }
    }

    // Fetch medical history if consented
    if (consent.history) {
      try {
        const historyRes = await fetch(`${BACKEND_API_URL}/api/v1/medical-records/patient/${patientId}`, {
          headers: {
            'Authorization': `Bearer ${authToken}`,
            'Content-Type': 'application/json',
          },
        });
        if (historyRes.ok) {
          const historyData = await historyRes.json();
          const history = historyData.data || historyData;
          context.medicalHistory = Array.isArray(history) ? history : [];
        }
      } catch (e) {
        console.error('Failed to fetch medical history:', e);
      }
    }
  } catch (error) {
    console.error('Error fetching context:', error);
  }

  return context;
}

// Detect conversation state transitions from user message
function detectStateTransition(
  userMessage: string,
  currentState: ConversationState,
  messageCount: number
): ConversationState {
  const lowerMessage = userMessage.toLowerCase();

  // Check for explicit endings
  if (
    lowerMessage.includes('thanks') ||
    lowerMessage.includes('thank you') ||
    lowerMessage.includes('that\'s all') ||
    lowerMessage.includes('that is all') ||
    lowerMessage.includes('goodbye') ||
    lowerMessage.includes('bye') ||
    lowerMessage.includes('done')
  ) {
    return 'CLOSING';
  }

  // If we've had enough exchanges and user is asking follow-ups, move to RESOLVING
  if (currentState === 'ACTIVE' && messageCount >= 6) {
    return 'RESOLVING';
  }

  // Initial -> Active on first user message
  if (currentState === 'INITIAL') {
    return 'ACTIVE';
  }

  return currentState;
}

export async function POST(request: Request) {
  try {
    if (!process.env.GROQ_API_KEY) {
      console.error('GROQ_API_KEY is not configured');
      return NextResponse.json(
        {
          reply: 'The AI assistant is not configured. Please contact support or check server configuration.',
          error: 'Groq API key is not configured on the server.',
        },
        { status: 500 },
      );
    }

    // Extract auth token from request headers
    const authHeader = request.headers.get('authorization');
    const authToken = authHeader?.replace('Bearer ', '') || '';

    const body = await request.json();
    const {
      messages,
      attachments,
      consent = { prescriptions: false, history: false },
      conversationState: incomingState = 'INITIAL',
      patientId,
    } = body as {
      messages: IncomingMessage[];
      attachments?: AttachmentPayload[];
      consent?: ConsentState;
      conversationState?: ConversationState;
      patientId?: string;
    };

    if (!messages || !Array.isArray(messages) || messages.length === 0) {
      return NextResponse.json(
        {
          reply: 'Please provide a valid message.',
          error: 'Messages array is required.',
        },
        { status: 400 },
      );
    }

    // Get the last user message for state detection
    const lastUserMessage = [...messages].reverse().find((m) => m.role === 'user');
    const messageCount = messages.filter((m) => m.role === 'user').length;

    // Detect state transition
    let conversationState: ConversationState = incomingState;
    if (lastUserMessage) {
      conversationState = detectStateTransition(lastUserMessage.content, incomingState, messageCount);
    }

    // Fetch context if patient ID and token are available
    let context: any = {};
    if (patientId && authToken) {
      context = await fetchContext(patientId, consent, authToken);
    }

    // Build enhanced system prompt
    const systemPrompt = buildSystemPrompt(consent, context, conversationState);

    // Convert messages to OpenAI format
    const formattedMessages = messages.map((message) => ({
      role: message.role === 'assistant' ? 'assistant' : 'user',
      content: message.content,
    }));

    // Add system message at the beginning
    const messagesWithSystem = [
      { role: 'system', content: systemPrompt },
      ...formattedMessages,
    ];

    // Handle attachments
    if (attachments && attachments.length > 0) {
      const latestMessage = messagesWithSystem[messagesWithSystem.length - 1];
      const attachmentTexts = attachments.map(
        (file) => `[User attached an image: ${file.name}. Please acknowledge this attachment.]`,
      );
      latestMessage.content = `${latestMessage.content}\n\n${attachmentTexts.join('\n')}`;
    }

    // Call Groq API
    const response = await fetch(GROQ_API_URL, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${process.env.GROQ_API_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        model: MODEL_NAME,
        messages: messagesWithSystem,
        temperature: 0.7,
        max_tokens: 1024,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      console.error('Groq API error response:', errorData);
      const errorMsg =
        errorData.error?.message ||
        errorData.message ||
        `Groq API error: ${response.status} ${response.statusText}`;
      throw new Error(errorMsg);
    }

    const data = await response.json();
    const text = data.choices?.[0]?.message?.content;

    // Check if response indicates conversation should end
    const responseLower = text?.toLowerCase() || '';
    if (
      responseLower.includes('is there anything else') ||
      responseLower.includes('anything else i can help') ||
      (conversationState === 'CLOSING' && messageCount > 1)
    ) {
      // Keep in CLOSING, or move to ENDED if user explicitly ended
      if (lastUserMessage?.content.toLowerCase().includes('thanks') || 
          lastUserMessage?.content.toLowerCase().includes('goodbye')) {
        conversationState = 'ENDED';
      }
    }

    return NextResponse.json({
      reply: text ?? "I'm sorry, I couldn't generate a response this time.",
      conversationState,
    });
  } catch (error: any) {
    console.error('Chatbot error:', error);
    console.error('Error details:', {
      message: error?.message,
      code: error?.code,
      status: error?.status,
      statusText: error?.statusText,
      response: error?.response,
      stack: error?.stack,
    });

    // Provide more specific error messages
    let errorMessage = 'Something went wrong while generating a response.';
    const errorMsg = error?.message?.toLowerCase() || '';
    const errorCode = error?.code?.toLowerCase() || '';

    if (errorMsg.includes('api_key') || errorCode.includes('api_key') || error?.status === 401) {
      errorMessage = 'Invalid API key. Please check your Groq API configuration.';
    } else if (errorMsg.includes('quota') || errorMsg.includes('rate limit') || error?.status === 429) {
      errorMessage = 'API rate limit exceeded. Please try again in a moment.';
    } else if (errorMsg.includes('network') || errorMsg.includes('fetch') || errorMsg.includes('econnrefused')) {
      errorMessage = 'Network error. Please check your internet connection and try again.';
    } else if (errorMsg.includes('permission') || errorCode.includes('permission')) {
      errorMessage = 'API key does not have permission to access this model.';
    } else if (errorMsg.includes('decommissioned') || errorMsg.includes('no longer supported')) {
      errorMessage = 'The AI model has been updated. Please refresh the page and try again.';
    }

    return NextResponse.json(
      {
        reply: errorMessage,
        error: error?.message || 'Unknown error occurred.',
        errorCode: error?.code,
        errorStatus: error?.status,
      },
      { status: 500 },
    );
  }
}

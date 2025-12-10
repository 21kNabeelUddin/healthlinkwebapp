# HealthLink+ AI Chatbot Architecture
## Designed for Patient Portal - Medical Assistant with Minimal RAG

---

## 1. Core Philosophy

**Goal**: Create a conversational medical assistant that:
- Provides empathetic, step-by-step guidance
- Knows when to end conversations gracefully
- Maintains context across multi-turn dialogues
- Accesses patient data only with explicit consent
- Uses minimal RAG (no vector embeddings) - just structured data injection

**Key Principle**: **Prompting is everything**. The model's behavior is 90% determined by the system prompt and conversation structure.

---

## 2. Conversation State Management

### 2.1 Conversation Lifecycle

```
[INITIAL] → [ACTIVE] → [RESOLVING] → [CLOSING] → [ENDED]
    ↓          ↓            ↓            ↓
  Greeting  Q&A Loop    Summarizing  Goodbye
```

**States:**
- **INITIAL**: First message, greeting phase
- **ACTIVE**: Active conversation, asking/answering questions
- **RESOLVING**: AI has enough info, providing summary/guidance
- **CLOSING**: Conversation naturally ending, offering follow-up
- **ENDED**: Conversation complete (can restart with new topic)

### 2.2 Conversation End Detection

**Triggers for ending:**
1. **User explicitly ends**: "thanks", "that's all", "goodbye", "I'm done"
2. **Resolution complete**: AI provided guidance, user acknowledged
3. **Inactivity timeout**: 5 minutes of no response (soft end)
4. **Topic completion**: Clear resolution to the medical question
5. **Escalation needed**: User needs urgent care → end with clear next steps

**Ending Protocol:**
- Summarize what was discussed
- Provide clear next steps
- Offer to help with something else
- Use closing phrases: "Is there anything else I can help you with today?"

---

## 3. Conversation Flow Design

### 3.1 Multi-Turn Dialogue Structure

```
Turn 1: User asks question
  ↓
Turn 2: AI acknowledges + asks 1-2 clarifying questions
  ↓
Turn 3: User responds
  ↓
Turn 4: AI asks follow-up OR provides guidance (if enough info)
  ↓
Turn 5+: Continue until resolution OR user ends
```

### 3.2 Conversation Patterns

**Pattern A: Information Gathering (Symptom Assessment)**
```
User: "My head hurts"
AI: "I'm sorry to hear that. To help you better, can you tell me:
     1. When did the headache start?
     2. Is it a sharp pain or dull ache?"
     
User: "Started this morning, it's a dull ache"
AI: "Thank you. A few more questions:
     1. Have you taken any pain medication?
     2. Does anything make it better or worse?"
     
[... continues until enough context ...]

AI: "Based on what you've shared, here's what might help:
     [guidance]
     However, if [red flags], please see a doctor immediately."
```

**Pattern B: Medical History Query**
```
User: "What medications am I currently taking?"
AI: [Checks consent] → [Fetches prescriptions] → 
    "Based on your medical records, you're currently taking:
     1. [Medication] - [Dosage] - [Frequency]
     2. [Medication] - [Dosage] - [Frequency]
     
     Is there anything specific about these medications you'd like to know?"
```

**Pattern C: Appointment Assistance**
```
User: "When is my next appointment?"
AI: [Fetches appointments] →
    "Your next appointment is:
     - Date: [Date]
     - Time: [Time]
     - Doctor: [Name]
     - Clinic: [Name]
     
     Would you like help preparing for this appointment?"
```

---

## 4. Prompting Architecture

### 4.1 System Prompt Structure

```
[ROLE DEFINITION]
You are the HealthLink+ AI companion, acting as a careful triage assistant.

[CONSTRAINTS]
- You are NOT a doctor
- You cannot diagnose or prescribe
- You provide general guidance only
- Always recommend professional evaluation when appropriate

[CONVERSATION STYLE]
- Empathetic and supportive
- Ask 1-2 questions at a time (don't overwhelm)
- Wait for answers before proceeding
- Use bullet points for clarity
- Be concise but thorough

[CONVERSATION MANAGEMENT]
- Track conversation state: INITIAL → ACTIVE → RESOLVING → CLOSING
- When you have enough information, move to RESOLVING state
- In RESOLVING: Provide summary + guidance + next steps
- In CLOSING: Offer to help with something else
- End conversations gracefully when:
  * User says goodbye/thanks
  * Topic is resolved
  * User needs urgent care (provide clear escalation path)

[CONTEXT AWARENESS]
- You have access to user's medical history (if consented)
- You have access to prescriptions (if consented)
- You have access to upcoming appointments
- Use this context to provide personalized responses
- Never share sensitive info unless user explicitly asks

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
```

### 4.2 Dynamic Prompt Injection

**Base System Prompt** (always included)
```
[Core role, constraints, style guidelines]
```

**Context Injection** (when user consents):
```
[USER MEDICAL CONTEXT]
- Current Medications: [list from prescriptions]
- Medical History: [relevant conditions from history]
- Upcoming Appointments: [next appointment details]
- Allergies: [if any]
```

**Conversation State Injection**:
```
[CURRENT STATE: ACTIVE]
[CONVERSATION SUMMARY SO FAR]:
- User mentioned: [symptom/concern]
- Asked about: [questions asked]
- Provided: [information shared]
```

**Turn-Specific Instructions**:
```
[THIS TURN]:
- User just said: "[last message]"
- Your task: [acknowledge + ask 1-2 questions OR provide guidance]
- State transition: [if moving to RESOLVING, indicate so]
```

---

## 5. Minimal RAG Implementation

### 5.1 Data Sources (No Vector Embeddings)

**Source 1: Prescriptions**
- Fetch from: `/api/v1/prescriptions/patient/{patientId}`
- Format: Structured list of medications with dosages
- Include: Medication name, dosage, frequency, start date, end date

**Source 2: Medical History**
- Fetch from: `/api/v1/medical-history/patient/{patientId}`
- Format: Structured list of conditions, diagnoses, procedures
- Include: Condition name, date, status, notes

**Source 3: Appointments**
- Fetch from: `/api/v1/appointments` (filtered by patient)
- Format: Upcoming appointments
- Include: Date, time, doctor, clinic, reason

### 5.2 Context Injection Strategy

**When to Inject Context:**
1. **Always**: User's name, upcoming appointments (non-sensitive)
2. **With Consent**: Prescriptions, medical history, past appointments

**How to Inject:**
```
[PATIENT CONTEXT]
Name: [Patient Name]

[UPCOMING APPOINTMENTS]
- [Date] at [Time] with Dr. [Name] at [Clinic]
  Reason: [Reason]

[PRESCRIPTIONS] (if consented)
Current Medications:
1. [Medication Name]
   - Dosage: [Amount] [Unit]
   - Frequency: [How often]
   - Started: [Date]
   - Prescribed by: Dr. [Name]

2. [Medication Name]
   ...

[MEDICAL HISTORY] (if consented)
Conditions:
- [Condition Name] (Diagnosed: [Date], Status: [Active/Resolved])
- [Condition Name] (Diagnosed: [Date], Status: [Active/Resolved])

Past Procedures:
- [Procedure] on [Date]
```

**Context Refresh:**
- Fetch fresh data at conversation start
- Re-fetch if user asks about recent changes
- Cache for session duration (don't re-fetch every turn)

---

## 6. User Consent Mechanism

### 6.1 Consent Dialog Design

**Location**: Patient Chatbot Page (`/patient/chatbot`)

**UI Component**:
```
┌─────────────────────────────────────────┐
│  🔒 Privacy & Data Access              │
├─────────────────────────────────────────┤
│  To provide personalized assistance,    │
│  I can access your:                     │
│                                          │
│  ☐ Prescriptions                        │
│    (Current medications, dosages)       │
│                                          │
│  ☐ Medical History                      │
│    (Past conditions, diagnoses)       │
│                                          │
│  [ ] I understand this is optional      │
│  [Allow Access] [Skip for Now]          │
└─────────────────────────────────────────┘
```

**Consent States:**
- **Not Asked**: Show dialog on first visit
- **Granted**: Store in session/localStorage, include context in prompts
- **Denied**: Don't show again (unless user manually enables)
- **Revocable**: User can toggle in settings anytime

### 6.2 Consent Storage

**Frontend**:
- Store in `localStorage`: `chatbot_consent_prescriptions`, `chatbot_consent_history`
- Send consent flags in API request

**Backend**:
- Include consent flags in request
- Only fetch data if consent granted
- Never fetch without explicit consent

---

## 7. Conversation Management Logic

### 7.1 Turn-by-Turn Flow

**Turn Processing:**
```
1. Receive user message
2. Check conversation state
3. Fetch context (if consented)
4. Build prompt with:
   - System instructions
   - Conversation history
   - Current context
   - State-specific instructions
5. Call AI model
6. Parse response
7. Update conversation state
8. Return response
```

### 7.2 State Transitions

**INITIAL → ACTIVE:**
- Trigger: User asks first question
- Action: Acknowledge + start information gathering

**ACTIVE → RESOLVING:**
- Trigger: AI has enough information (3-5 exchanges)
- Action: Provide summary + guidance + next steps

**RESOLVING → CLOSING:**
- Trigger: User acknowledges guidance OR asks follow-up
- Action: Offer additional help OR end conversation

**CLOSING → ENDED:**
- Trigger: User says goodbye OR no response for 5 minutes
- Action: Final message + conversation summary

**Any State → ENDED:**
- Trigger: User explicitly ends OR red flag detected
- Action: Immediate closure with appropriate message

### 7.3 Conversation History Management

**Keep in Context:**
- Last 10-15 messages (to maintain context)
- System messages (state transitions)
- User consent status

**Don't Keep:**
- Very old messages (>20 turns)
- Irrelevant context (previous topics)

**History Format:**
```
[
  { role: "system", content: "[State: INITIAL]" },
  { role: "assistant", content: "Hi! How can I help you today?" },
  { role: "user", content: "My head hurts" },
  { role: "assistant", content: "I'm sorry to hear that..." },
  ...
]
```

---

## 8. Medical Assistance Guidelines

### 8.1 Symptom Assessment Flow

**Step 1: Acknowledge**
- Empathize with the concern
- Set expectations (AI assistant, not a doctor)

**Step 2: Gather Context (1-2 questions per turn)**
- Onset: When did it start?
- Severity: Rate pain 1-10
- Pattern: Constant or comes/goes?
- Triggers: What makes it better/worse?
- Associated symptoms: Any other symptoms?
- History: Has this happened before?

**Step 3: Assess Urgency**
- Check for red flags
- If red flags → escalate immediately
- If not urgent → continue assessment

**Step 4: Provide Guidance**
- General self-care tips
- OTC options (if safe)
- When to see a doctor
- What to monitor

**Step 5: Close**
- Summarize key points
- Remind to see doctor if needed
- Offer follow-up

### 8.2 Medication Queries

**If User Asks About Medications:**
1. Check consent for prescriptions
2. If consented: Fetch and list medications
3. Explain each medication's purpose (if known)
4. Warn about interactions (if applicable)
5. Remind to consult doctor for changes

**If User Asks About Side Effects:**
1. Provide general information
2. Emphasize: "If you experience [serious side effects], contact your doctor immediately"
3. Suggest discussing with prescribing doctor

### 8.3 Appointment Assistance

**Pre-Appointment Preparation:**
- List what to bring
- Suggest questions to ask doctor
- Remind about appointment time/location

**Post-Appointment Follow-up:**
- Ask how appointment went
- Offer to help with follow-up questions
- Remind about medication changes (if any)

---

## 9. Error Handling & Edge Cases

### 9.1 API Failures

**If RAG data fetch fails:**
- Continue conversation without context
- Inform user: "I'm having trouble accessing your records right now, but I can still help with general questions."

**If AI model fails:**
- Retry once with simpler prompt
- If still fails: "I'm experiencing technical difficulties. Please try again in a moment."

### 9.2 Conversation Edge Cases

**User sends empty message:**
- Ignore or ask: "I didn't catch that. Could you repeat?"

**User sends very long message:**
- Acknowledge: "That's a lot of information. Let me address the main points..."
- Break down into manageable responses

**User asks about multiple topics:**
- Address one topic at a time
- "You mentioned several things. Let's start with [topic 1]. We can discuss the others after."

**User gets frustrated:**
- Acknowledge: "I understand this is frustrating. Let me try a different approach..."
- Offer to connect with human support

---

## 10. Implementation Phases

### Phase 1: Core Conversation (Week 1)
- ✅ Basic chatbot with Groq
- ✅ Conversation state management
- ✅ Multi-turn dialogue handling
- ✅ Conversation end detection
- ✅ Improved prompting

### Phase 2: Consent & RAG (Week 2)
- Consent dialog UI
- Consent storage (localStorage)
- Prescription data fetching
- Medical history data fetching
- Context injection into prompts

### Phase 3: Medical Assistance (Week 3)
- Symptom assessment flow
- Medication query handling
- Appointment assistance
- Red flag detection
- Escalation protocols

### Phase 4: Polish & Testing (Week 4)
- Error handling
- Edge case handling
- User testing
- Prompt refinement
- Performance optimization

---

## 11. Prompt Engineering Best Practices

### 11.1 System Prompt Structure

**Order Matters:**
1. Role definition (first)
2. Constraints (critical)
3. Style guidelines
4. State management rules
5. Context usage instructions
6. Red flags (prominent)

### 11.2 Few-Shot Examples in Prompt

Include examples of good conversations:
```
[EXAMPLE CONVERSATION 1: Symptom Assessment]
User: "My head hurts"
AI: "I'm sorry to hear that. To help you better, can you tell me:
     1. When did the headache start?
     2. Is it a sharp pain or dull ache?"

[EXAMPLE CONVERSATION 2: Ending Gracefully]
User: "Thanks, that helps"
AI: "You're welcome! Remember to see a doctor if the symptoms persist or worsen.
     Is there anything else I can help you with today?"
```

### 11.3 Dynamic Instructions Per Turn

**For Information Gathering:**
```
[THIS TURN]: Ask 1-2 clarifying questions. Don't provide guidance yet.
```

**For Providing Guidance:**
```
[THIS TURN]: You have enough information. Provide:
1. Summary of what user shared
2. General guidance (3-4 bullet points)
3. When to see a doctor
4. What to monitor
Then ask: "Does this help, or do you have more questions?"
```

**For Ending:**
```
[THIS TURN]: User indicated they're done. Provide:
1. Brief summary
2. Next steps
3. Offer to help with something else
Keep it concise (2-3 sentences max).
```

---

## 12. Technical Architecture

### 12.1 Frontend Components

```
/patient/chatbot/page.tsx
├── ConsentDialog (one-time, on first visit)
├── ChatInterface
│   ├── MessageList
│   ├── InputArea
│   └── AttachmentUpload
└── ConversationState (manages state locally)
```

### 12.2 Backend API Route

```
/api/chatbot/route.ts
├── POST handler
│   ├── Validate request
│   ├── Check user authentication
│   ├── Fetch context (if consented):
│   │   ├── Prescriptions API call
│   │   ├── Medical History API call
│   │   └── Appointments API call
│   ├── Build prompt:
│   │   ├── System prompt
│   │   ├── Conversation history
│   │   ├── Context injection
│   │   └── State-specific instructions
│   ├── Call Groq API
│   ├── Parse response
│   ├── Update conversation state
│   └── Return response
```

### 12.3 Data Flow

```
User Message
    ↓
Frontend: Add to conversation history
    ↓
API Request: {
  messages: [...history],
  consent: { prescriptions: true, history: true },
  state: "ACTIVE"
}
    ↓
Backend: Fetch context (if consented)
    ↓
Backend: Build prompt with context
    ↓
Backend: Call Groq API
    ↓
Backend: Parse response + update state
    ↓
Frontend: Display response + update UI
```

---

## 13. Conversation Examples

### Example 1: Symptom Assessment (Full Flow)

```
[INITIAL]
User: "My head hurts"

[ACTIVE]
AI: "I'm sorry to hear that. To help you better, can you tell me:
     1. When did the headache start?
     2. Is it a sharp pain or dull ache?"

User: "Started this morning, it's a dull ache"

AI: "Thank you. A few more questions:
     1. Have you taken any pain medication?
     2. Does anything make it better or worse?"

User: "No medication yet. It gets worse when I move"

[RESOLVING]
AI: "Based on what you've shared:
     
     **Summary:**
     - Dull headache since this morning
     - Worsens with movement
     - No medication taken yet
     
     **General Guidance:**
     - Rest in a quiet, dark room
     - Try a cold compress on your forehead
     - Stay hydrated
     - Consider OTC pain relief (ibuprofen or acetaminophen)
     
     **When to See a Doctor:**
     - If pain becomes severe (8+/10)
     - If it persists for more than 24 hours
     - If you experience vision changes or nausea
     
     Does this help, or do you have more questions?"

User: "That helps, thanks"

[CLOSING]
AI: "You're welcome! Remember to see a doctor if the symptoms persist or worsen.
     Is there anything else I can help you with today?"

User: "No, that's all"

[ENDED]
AI: "Take care! Feel free to come back if you need anything else."
```

### Example 2: Medication Query (With Consent)

```
[ACTIVE]
User: "What medications am I currently taking?"

AI: [Checks consent: granted]
    [Fetches prescriptions]
    
    "Based on your medical records, you're currently taking:
     
     1. **Metformin** - 500mg twice daily
        - Started: 3 months ago
        - Prescribed by: Dr. Ahmed
     
     2. **Lisinopril** - 10mg once daily
        - Started: 6 months ago
        - Prescribed by: Dr. Ahmed
     
     Is there anything specific about these medications you'd like to know?"

User: "What's Metformin for?"

AI: "Metformin is commonly prescribed for:
     - Managing type 2 diabetes
     - Improving blood sugar control
     
     **Important Notes:**
     - Take with meals to reduce stomach upset
     - Common side effects: mild nausea, diarrhea (usually temporary)
     - If you experience severe side effects, contact Dr. Ahmed
     
     For specific questions about your dosage or any concerns, I'd recommend discussing with Dr. Ahmed at your next appointment.
     
     Is there anything else about your medications?"
```

---

## 14. Key Design Decisions

### 14.1 Why Minimal RAG (No Vector Embeddings)?

**Reasons:**
1. **Time constraints**: Vector DB setup is complex
2. **Data volume**: Patient data is structured and small
3. **Precision over recall**: We want exact matches, not semantic similarity
4. **Simplicity**: Easier to debug and maintain
5. **Cost**: No additional infrastructure needed

**Trade-off**: Less "intelligent" search, but more predictable and faster.

### 14.2 Why State Management?

**Reasons:**
1. **Conversation flow**: Natural progression from questions → answers → closure
2. **Context awareness**: AI knows where it is in the conversation
3. **Better endings**: Explicit state transitions prevent awkward endings
4. **User experience**: More natural, less repetitive

### 14.3 Why Explicit Consent?

**Reasons:**
1. **Privacy**: Medical data is sensitive
2. **Trust**: Users control their data
3. **Compliance**: Better for HIPAA/GDPR
4. **Transparency**: Users know what data is accessed

---

## 15. Success Metrics

### 15.1 Conversation Quality
- Average conversation length: 5-10 turns
- Resolution rate: >80% of conversations reach natural conclusion
- User satisfaction: Positive feedback on helpfulness

### 15.2 Medical Safety
- Red flag detection: 100% of urgent cases escalated
- No false diagnoses: 0 instances of AI claiming to diagnose
- Appropriate guidance: All advice within AI assistant scope

### 15.3 Technical Performance
- Response time: <3 seconds per turn
- Error rate: <5% of conversations
- Context accuracy: 100% when consent granted

---

## 16. Next Steps

1. **Review this architecture** with team
2. **Refine prompts** based on medical domain expertise
3. **Implement Phase 1** (core conversation)
4. **Test with real users** (internal testing first)
5. **Iterate on prompts** based on actual conversations
6. **Add RAG** (Phase 2) once core conversation works well
7. **Polish** based on user feedback

---

**Key Takeaway**: The success of this chatbot depends 90% on prompt engineering. The RAG is just context injection - the real intelligence comes from how we structure the conversation and guide the AI model's behavior through carefully crafted prompts.


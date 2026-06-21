# EthioLearn Architecture & System Design Document

## 1. Product Architecture
EthioLearn is an **Offline-First Education App** designed for Ethiopian students, built natively on Android using Kotlin and Jetpack Compose.
- **Client Layer**: Modern Jetpack Compose UI following Material Design 3 guidelines for high accessibility and simple navigation.
- **State Management**: Clean architecture via ViewModels utilizing `StateFlow` and Kotlin Coroutines.
- **Data Persistence**: Room Database (SQLite abstraction) acting as the single source of truth for all content and user progress.
- **Content Pre-packaging**: Core curriculum (Grades 9 & 10 Mathematics initially) is shipped with the APK or downloaded as a modular asset pack upon first launch.
- **AI Tutor Layer**: Integration with Gemini 3.1 Pro (via REST) using "High Thinking" configuration to debug student questions when they are online, serving as a fallback personalized tutor. 

## 2. Complete Database Schema (Room SQLite)
```sql
CREATE TABLE Grades (id INTEGER PRIMARY KEY, name TEXT);
CREATE TABLE Subjects (id INTEGER PRIMARY KEY, grade_id INTEGER, name TEXT, FOREIGN KEY(grade_id) REFERENCES Grades(id));
CREATE TABLE Units (id INTEGER PRIMARY KEY, subject_id INTEGER, unit_number INTEGER, title TEXT, FOREIGN KEY(subject_id) REFERENCES Subjects(id));
CREATE TABLE Topics (id INTEGER PRIMARY KEY, unit_id INTEGER, title TEXT, FOREIGN KEY(unit_id) REFERENCES Units(id));
CREATE TABLE Lessons (id INTEGER PRIMARY KEY, topic_id INTEGER, summary TEXT, key_concepts TEXT, important_notes TEXT, formulas TEXT, FOREIGN KEY(topic_id) REFERENCES Topics(id));
CREATE TABLE Examples (id INTEGER PRIMARY KEY, topic_id INTEGER, question TEXT, step_by_step_solution TEXT, FOREIGN KEY(topic_id) REFERENCES Topics(id));
CREATE TABLE PracticeQuestions (id INTEGER PRIMARY KEY, topic_id INTEGER, difficulty TEXT, question TEXT, correct_answer TEXT, explanation TEXT, FOREIGN KEY(topic_id) REFERENCES Topics(id));
CREATE TABLE QuizQuestions (id INTEGER PRIMARY KEY, topic_id INTEGER, question TEXT, options_json TEXT, correct_option_index INTEGER, FOREIGN KEY(topic_id) REFERENCES Topics(id));
CREATE TABLE Progress (id INTEGER PRIMARY KEY, user_id INTEGER, topic_id INTEGER, completed_lessons BOOLEAN, quiz_score INTEGER, timestamp INTEGER);
```

## 3. Navigation Structure
- **Home (`/`)**: Main entry point, dashboard overview of progress.
- **Grade Selection (`/grades`)**: Choose Grade 9 or Grade 10.
- **Subject Selection (`/grades/{id}/subjects`)**: E.g., Mathematics.
- **Unit Browser (`/subjects/{id}/units`)**: Display units as per Ethiopian curriculum.
- **Topic Browser (`/units/{id}/topics`)**: Topics under the selected unit.
- **Topic Detail (`/topics/{id}`)**: Hosts a Tabs layout or bottom navigation:
  - Learn Mode (Summary, Concepts)
  - Example Mode (Step-by-step)
  - Practice Mode
  - Quiz Mode
- **Progress Dashboard (`/progress`)**: Detailed statistics and performance tracking.

## 4. Screen List
1. `HomeScreen`: Welcome back, resume where you left off, progress abstract.
2. `GradeSubjectScreen`: Consolidated grade and subject pathway.
3. `UnitsScreen`: List of curricular units.
4. `TopicsScreen`: List of topics in a unit.
5. `LearnScreen`: Formatted text block with MathJax/formulas rendered cleanly.
6. `ExamplesScreen`: Accordion-style worked examples.
7. `PracticeScreen`: Interactive question cards with immediate submit & feedback mechanism.
8. `QuizScreen`: 10-question gauntlet with final score review.
9. `AITutorModal`: A floating dialog connecting to Gemini for advanced debugging.

## 5. Wireframe Descriptions
- **Unit Browser**: A vertical scroll view of cards. Each card has a unit number, title, and a circular progress bar. 
- **LearnScreen**: A clean, white (or true black in dark mode) reading pane. Generous line height (1.5). Content is broken into logical sections (e.g. "Key Concepts" styled as highlighted callout boxes).
- **PracticeScreen**: A single question is shown. A text input or multiple-choice options are presented. A "Reveal Answer & Explanation" secondary button is available if the student is stuck.

## 6. UI Design System
- **Color Palette (Primary)**: Deep Indigo (`#3F51B5`) and Teal (`#009688`) to convey calm, academic tone without glare.
- **Typography**: Clean sans-serif (Inter or Roboto). Minimum font size `16sp` for readability on low-DPI screens.
- **Shapes**: Rounded corners (`8dp`) for cards to modernize the look.
- **Spacing**: 16dp global margins to ensure adequate whitespace and avoid a cluttered layout, crucial for long study sessions.

## 7. User Journey
1. **Onboarding**: Student installs the app. Selects "Grade 10" -> "Mathematics". App sets this as default.
2. **First Session**: Navigates to Unit 1: Relations and Functions.
3. **Execution**: Reads the "Learn Mode".
4. **Validation**: Enters "Practice Mode". Solves 5 questions.
5. **Testing**: Takes the Unit Quiz. Scores 8/10.
6. **AI Support**: If stuck on Question 9, taps "Ask AI Tutor" -> Gemini Pro processes the complex query with High Thinking and explains the reasoning.

## 8. Development Roadmap
- **Phase 1 (MVP)**: Room DB schema, Compose UI shell, mock data for Math Grade 9 Unit 1. Basic Offline UI navigation.
- **Phase 2**: Complete Grades 9 & 10 Math data entry. Implement Quiz scoring system and local Progress persistence.
- **Phase 3 (AI Integration)**: Gemini 3.1 Pro integration for step-by-step math solver tool for edge cases.
- **Phase 4 (Scale)**: Expand to Physics, Biology, Chemistry. Add sync to Firebase if user is online for backup.

## 9. Folder Structure
```text
/app/src/main/java/com/aistudio/ethiolearn/
├── data/
│   ├── db/ (Room AppDatabase, DAOs)
│   ├── model/ (Room Entities)
│   └── repository/ (Repository pattern bridging data)
├── domain/ (Use Cases, AI generation models)
├── ui/
│   ├── core/ (Theme, Type, UI constants)
│   ├── components/ (Reusable Compose buttons, cards, NavTopBar)
│   ├── screens/ (Home, Unit, Topic, Learn, Quiz)
│   └── viewmodels/ (State holders)
└── MainActivity.kt
```

## 10. Scalability Plan
- **Modular Asset Delivery**: Instead of shipping a 100MB APK with 4 subjects' worth of videos/images, use Android App Bundles (AAB) or a background worker (WorkManager) to download curriculum JSONs/DBs when the user has intermittent internet.
- **Database Indexing**: Add Room indices on `topic_id` and `unit_id` to ensure lightning-fast O(log N) lookups on low-end devices.

## 11. Security Plan
- User progress data is stored locally in the private app sandbox. 
- Any remote API connections (like the AI Tutor) use HTTPS. 
- API Keys (Gemini) are injected at build time, securely managed by CI/Secrets, NOT checked into GitHub source control.

## 12. MVP Definition
An Android app that has a fully working offline Math curriculum (Grade 9 outline as proof-of-concept), including unit viewing, topic learning text, step-by-step examples, and a functional localized 5-question quiz mechanism with tracking.

## 13. API Integration Specs
- **Model**: `gemini-3.1-pro-preview`
- **Configuration**: `ThinkingConfig(thinkingLevel = "high")`
- **Endpoint**: REST `v1beta/models/gemini-3.1-pro-preview:generateContent`
- **Flow**: User invokes AI Tutor -> Request built with Student's query + current Topic Context -> Gemini generates high-level reasoning step-by-step response -> Response parsed and shown to student.

## 14. Performance Optimization Strategy
- **Image handling**: Text-heavy. If images are needed, use WEBP format.
- **Memory**: Use Jetpack Compose `LazyColumn` across the app to lazily load unit items instead of inflating hundreds of views.
- **DB Operations**: All Room operations are suspended and dispatched on `Dispatchers.IO` to ensure uncompromised 60fps scrolling on the main thread for budget ~$100 Android phones common in the target demographic.

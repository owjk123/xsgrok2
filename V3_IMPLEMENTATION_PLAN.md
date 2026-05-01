# xsgrok2 v3.0 Implementation Plan

## P0 Changes (Must Have)

### 1. Chapter Free Generation & Regeneration
- Generate any chapter number, not just next
- Regenerate existing chapter
- Insert chapter at position
- Custom chapter titles
- Delete single chapter

### 2. Chapter Content Editing
- Edit generated content in reader
- Save edited content
- AI-assisted rewrite (selected text / full chapter)

### 3. Dynamic Settings Management
- Edit worldSetting, keyCharacters, outline at any time
- Regenerate settings

### 4. Structured Chapter Instructions
- Replace userNote with structured instruction:
  - coreEvent (核心事件)
  - characterChanges (人物变化)
  - mood (情绪氛围)
  - foreshadowing (伏笔回收)
  - newThreads (新线埋设)
  - wordCountTarget (目标字数)
  - forbiddenElements (禁止元素)

### 5. Lorebook (World Entries)
- Add keyword-triggered entries
- Auto-inject relevant entries into generation context

## P1 Changes (Should Have)

### 6. Batch Generation
- Generate multiple chapters at once (draft mode)

### 7. Generation Parameters
- Temperature slider
- Word count target per chapter

### 8. Outline-Driven Generation
- Parse outline into chapter plans
- Show chapter plan from outline

## Data Model Changes

### Novel (add fields)
- lastSettingVersion: Int = 0

### Chapter (add fields)  
- customTitle: String = "" (overrides "第N章")
- status: String = "generated" // "draft", "generated", "edited"
- generationMode: String = "new" // "new", "regenerate", "rewrite"

### New: LorebookEntry
- id, novelId, keyword, content, importance (1-5), enabled, createdAt

### New: ChapterInstruction  
- id, chapterId, novelId, coreEvent, characterChanges, mood, foreshadowing, newThreads, wordCountTarget, forbiddenElements, createdAt

### Database version: 1 → 3 (fallbackToDestructiveMigration stays)

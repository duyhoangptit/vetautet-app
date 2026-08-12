Skills = Claude biết làm một việc như thế nào.
Agents = Claude giao việc cho chuyên gia nào.
Hooks/settings = Claude được phép làm gì và khi nào bị chặn.
docs = project này có những nguyên tắc gì.


Claude
│
├── Read source             ✅
├── Analyze logs            ✅
├── Generate code           ✅
├── Run unit test           ✅
├── Run static analysis     ✅
│
├── kubectl                 ❌
├── deploy                  ❌
├── restart production      ❌
├── terraform apply         ❌
└── arbitrary production action ❌


```                 YOUR SOFTWARE
                       │
        ┌──────────────┴──────────────┐
        │                             │
     src/                         .claude/
        │                             │
   executable code             AI behavior
        │                             │
        │                    ┌────────┼────────┐
        │                    │        │        │
        │                 Skills   Agents   Guardrails
        │                    │        │        │
        └────────────────────┴────────┴────────┘
                             │
                         Claude Code
```
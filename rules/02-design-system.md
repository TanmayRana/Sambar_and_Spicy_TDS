# Design system

Use the BistroFlow-inspired, clean application shell with light and dark semantic tokens. Light: canvas #F8FAFC, card #FFFFFF, muted #F1F5F9, border #E2E8F0, primary text #0F172A, secondary text #475569. Dark: canvas #0B1120, card #1E293B, muted #0F172A, border #334155, primary text #F8FAFC, secondary text #94A3B8. Primary blue #2563EB, positive green #16A34A, hold orange #EA580C, low-stock amber #D97706, critical red #DC2626. Use Inter and JetBrains Mono; keep receipt paper white. Maintain explicit status text, visible focus, 44px primary targets and tabular numbers. Respect reduced motion and responsive layouts.

Use semantic Tailwind theme tokens and the shared primitives in `@/components/ui/*` for new UI. Use `@/components/shared/*` for repeated cross-section patterns. Keep existing legacy styles until their screens have been migrated and verified.

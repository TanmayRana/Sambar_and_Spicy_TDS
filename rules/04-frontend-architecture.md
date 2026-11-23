# Frontend architecture

Use Next.js App Router, TypeScript strict mode, Tailwind, Radix/shadcn components, TanStack Query, React Hook Form and Zod. Phase 1 data access goes through a typed API interface and localStorage mock adapter, never arrays hardcoded in UI components.

Build new presentation with `@/components/ui/*` primitives and reuse cross-section patterns from `@/components/shared/*`; preserve accessible names and native form behavior where existing workflows depend on it.

# Project instructions

Read the rules in `rules/` before changing this project. The SQL schema in `docs/Sambar_and_Spicy_Corrected_MySQL_Schema.sql` is the contract source of truth. Keep changes within the single-outlet, counter-billing scope. Phase 1 uses a typed API interface with a localStorage mock adapter; never put mock data arrays in UI components. Client-side role checks are not security: enforce authorization on the server in later phases.

# AI usage

- Tool: OpenAI Codex, used as an implementation assistant during the timed engineering exercise.
- Key prompt: “Build the attached Project Tapewatch assignment; prioritize a working, testable implementation and explain the steps.”
- Codex generated the initial Spring Boot structure, API, wash-trade rules, replay scripts, and documentation. I reviewed the code and should independently walk through each rule and validate the result before submission.
- Review notes: I caught a WT-2 ground-truth owner mapping error by checking planted accounts against `accounts.csv` and corrected it. I also caught that the initial test `ObjectMapper` setup did not register Java time support and corrected the test setup. Redis-backed windows, Redis integration, load numbers, and UI escalation remain incomplete; I will only report test and replay results after running them locally.

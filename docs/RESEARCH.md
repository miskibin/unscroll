# Research: reducing scrolling & improving attention without frustration

A synthesis of the evidence behind this app's redesign. Multi-source, peer-reviewed where possible, with confidence flags. Compiled 2026-06.

> **Two things to internalize first**
> 1. **Honest effect sizes are small.** The best causal studies (large Facebook-deactivation RCTs) find wellbeing gains around **0.09 SD** (Allcott et al. 2020); the reducing-use → depression meta-analysis is **g≈0.25**. Durability past a few weeks is largely unproven. Realistic goal: *durable small wins + identity change*, not a cure.
> 2. **Most scary framing is overstated or myth** — and our old in-app copy leaned on exactly the pop-science the literature debunks. Accuracy is a feature: it builds the trust that durable change needs.

---

## 1. How the brain actually works (myth vs. real)

**Real & well-established**
- **Dopamine = "wanting," not "pleasure."** It encodes a *reward-prediction error* and drives *incentive salience* (the urge to pursue), dissociable from actual enjoyment — you can compulsively want what you no longer like. Schultz, Dayan & Montague 1997, *Science* (https://www.gatsby.ucl.ac.uk/~dayan/papers/sdm97.pdf); Berridge 2007, *Psychopharmacology* (https://link.springer.com/article/10.1007/s00213-006-0578-x). **HIGH.**
- **Habits are context-cued and goal-independent.** Once formed, behavior fires automatically from cues (unlocking the phone, a time of day, a preceding action) even against current goals. Wood & Neal 2007, *Psychological Review* (https://dornsife.usc.edu/wendy-wood/wp-content/uploads/sites/183/2023/10/wood.neal_.2007psychrev_a_new_look_at_habits_and_the_interface_between_habits_and_goals.pdf). **HIGH.**
- **Novelty is intrinsically rewarding** — novel stimuli activate the dopaminergic midbrain, which is why an endless stream of *new* content is compelling. Bunzeck & Düzel 2006, *Neuron* (https://www.cell.com/neuron/fulltext/S0896-6273(06)00557-5). **HIGH in lab**; the leap to "infinite feeds = engineered addiction" is inference.
- **Task-switching has real costs**, and **attention residue** from an interrupted task degrades the next one. APA on multitasking (https://www.apa.org/topics/research/multitasking); Leroy 2009, *OBHDP* (https://ideas.repec.org/a/eee/jobhdp/v109y2009i2p168-181.html). **HIGH / MEDIUM-HIGH.** This is the strongest "attention" finding.
- **Variable-ratio reinforcement** (unpredictable rewards) sustains the most persistent behavior — the slot-machine principle. **HIGH for the principle; MEDIUM** that platforms literally run optimized schedules (well-motivated analogy, not measured fact).

**Myths to stop repeating**
- ❌ **"Dopamine detox / reset."** Dopamine isn't a toxin or a reservoir you flush; the framing is incoherent. The legitimate kernel is **stimulus control** (a real CBT technique) — just don't call it "dopamine." Harvard Health 2020 (https://www.health.harvard.edu/blog/dopamine-fasting-misunderstanding-science-spawns-a-maladaptive-fad-2020022618917). **HIGH.**
- ❌ **"8-second attention span, worse than a goldfish."** Fabricated; traced to a no-source stat site. BBC 2017 (https://www.bbc.com/news/health-38896790). **HIGH — pure myth.**
- ❌ **"Short video rewires/destroys your attention."** The famous media-multitasking study (Ophir 2009, *PNAS*, https://www.pnas.org/doi/10.1073/pnas.0903620106) **largely failed to replicate** (pooled d≈0.13–0.17, near-null on objective tests; Parry & le Roux 2021, https://cyberpsychology.eu/article/view/13303). Short-form→attention harm is **correlational and likely bidirectional** (Thorell et al. 2022, https://www.ncbi.nlm.nih.gov/pmc/articles/PMC11272698/).
- ⚠️ **Gloria Mark's "47-second" figure is real but means *self-interruption / task-switching* increased — not that human attention capacity is biologically shrinking.** *Attention Span* (2023), https://gloriamark.com/attention-span/.
- ⚠️ **"Social media addiction" is not a clinical diagnosis** (DSM-5 / ICD-11 recognize only gambling & gaming disorder); average population harm is **small** with debated subgroup effects. Orben & Przybylski 2019, *PNAS* (https://www.pnas.org/doi/10.1073/pnas.1902058116).

---

## 2. Why blunt blocking / willpower backfires

- **Restriction triggers reactance** — removing a freedom makes the forbidden thing *more* desirable; mandates beat worse than choice-preserving nudges. Brehm reactance theory (https://en.wikipedia.org/wiki/Reactance_(psychology)); JBEE 2023 (https://ideas.repec.org/a/eee/soceco/v106y2023ics2214804323000733.html).
- **All-or-nothing limits are fragile — the "what-the-hell effect."** One lapse against a strict goal triggers wholesale abandonment. (Abstinence-violation effect, https://www.sciencedirect.com/topics/psychology/abstinence-violation.)
- **Willpower rationales rest on shaky ground.** Ego depletion **failed a 23-lab replication** and a 36-lab test found ~zero effect. Hagger et al. 2016 RRR (https://en.wikipedia.org/wiki/Ego_depletion); Vohs et al. 2021 (https://pubmed.ncbi.nlm.nih.gov/34520296/). **Don't design around "resist the urge."**
- **Blockers get circumvented and self-weakened.** Blocking is the dominant strategy (~74% of 367 tools; Lyngs et al. 2019, CHI, https://arxiv.org/abs/1902.00157), but users **predictably weaken their own restrictions while believing they'll re-tighten them** (Kovacs et al. 2021, https://arxiv.org/pdf/2101.11743), and effects are small/short (Roffarello & De Russis 2023, TOCHI, https://dl.acm.org/doi/10.1145/3571810).
- **Repeated identical interventions habituate;** rotating restores effect but raises attrition unless explained (Kovacs et al. 2018, HabitLab, https://hci.stanford.edu/publications/2018/habitlab/habitlab-cscw18.pdf).
- **Nagging notifications are weak** — disabling notifications didn't even reduce screen time (Media Psychology 2024, https://www.tandfonline.com/doi/full/10.1080/15213269.2024.2334025).
- **Effectiveness ↔ frustration tradeoff** — stronger obstacles work better but anger users; removing them leaves resentment (JMIR 2022, https://www.ncbi.nlm.nih.gov/pmc/articles/PMC9066336/).

> **Implication for "earn scroll time by quiz":** friction is good, but making *scrolling the prize* reinforces the "wanting." Beware the **overjustification effect** — extrinsic rewards can undermine the intrinsic motivation that makes change durable (Deci, Koestner & Ryan 1999). Reframe so the reward is the *alternative*, not the scroll.

---

## 3. What actually works (ranked by strength × willpower-independence)

1. **Implementation intentions ("if-then" plans)** — biggest cheap lever, **d≈0.65** across 94 tests (Gollwitzer & Sheeran 2006, https://www.scirp.org/reference/referencespapers?referenceid=1138676). Automates the action when the cue fires.
2. **Defaults & friction** — defaults shift behavior without coercion; **grayscale cut screen time ~38 min/day** in two studies (Holte & Ferraro 2020, https://www.tandfonline.com/doi/abs/10.1080/03623319.2020.1737461; Dekker & Baumgartner 2024, https://journals.sagepub.com/doi/10.1177/20501579231212062). *Note: forcing device-wide grayscale needs `WRITE_SECURE_SETTINGS` (adb-only) — can't be done from a normal app, so we recommend it rather than automate it.*
3. **Disrupt the cue / substitute the routine** — easier than out-motivating the habit (Wood & Rünger 2016, https://dornsife.usc.edu/wendy-wood/wp-content/uploads/sites/183/2023/10/wood.runger.2016.pdf).
4. **Self-monitoring + feedback** — among the most effective BCTs (Michie / HTA 2015, https://www.ncbi.nlm.nih.gov/books/NBK327619/); passive dashboards alone are weak.
5. **Autonomy-supportive design (Self-Determination Theory)** — satisfying autonomy/competence/relatedness drives *durable, intrinsic* change; controlling design undermines it (Ng et al. 2012, https://selfdeterminationtheory.org/SDT/documents/2012-NgNtoumanis_PPS.pdf). **Master principle: be the user's ally, not their warden.**
6. **Notification batching (3×/day)** — RCT: more attentive, happier, fewer unlocks; *zero* notifications backfired (Fitz et al. 2019, https://www.sciencedirect.com/science/article/abs/pii/S0747563219302596).
7. **Commitment devices** — modest, best when stakes are *public*; temporary incentives produced *persistent* reductions (Allcott et al. 2022, *AER*, https://www.aeaweb.org/articles?id=10.1257/aer.20210867).
8. **JITAI (only when receptive)** — intervene at the moment of need *and* receptivity; mistimed prompts cause disengagement (Nahum-Shani et al. 2018, https://academic.oup.com/abm/article/52/6/446/4733473).
9. **Mindfulness / urge-surfing** — reliably improves *distraction & self-awareness*, less reliably cuts raw usage (Throuvala et al. 2020, https://www.mdpi.com/1660-4601/17/13/4842).
10. **Gamification** — only **small** effects, design-dependent, *backfires when controlling* (Sailer & Homner 2020, https://link.springer.com/article/10.1007/s10648-019-09498-w).

---

## 4. The product blueprint (impact vs. frustration)

**🟢 Tier 1 — high impact, low frustration (build first)**
- **Capture intention at entry, not punishment.** One tap: *"What are you here for?"* → message / check one thing / just browsing / bored. Purposeful answers get a fast path (kills reactance + false positives); aimless answers get the full pause. This is the d≈0.65 implementation-intention lever *and* self-monitoring.
- **Auto-grayscale** the device during sessions (recommend; needs adb `WRITE_SECURE_SETTINGS`).
- **Honor the user's own rules; always leave a visible, low-shame exit** (the emergency disable *reduces* reactance — keep it).
- **Rewrite all copy to be accurate & autonomy-supportive** (drop dopamine-detox / goldfish claims).

**🟡 Tier 2 — high impact, medium frustration (optional)**
- **Stopping cues / "scroll budget"** — calm interstitial at natural breakpoints ("4 min in — keep going or done?"). A *cue*, not a lock.
- **If-then plan setup ritual**, surfaced at the moment of the cue (habit substitution).
- **Reframe the cooldown from punishment → the user's own focus block,** with graceful lapse handling (a slip ≠ failure → defeats the what-the-hell effect).
- **Rotate the friction** with a one-line "why it changes" explainer (avoids the attrition penalty).

**🟠 Tier 3 — promising, weaker evidence (experiment, measure)**
- Replace "earn scroll time" with "earn by doing the *alternative*."
- Optional public commitment device / accountability buddy.
- JITAI targeting your known weak moments (e.g., late-night).
- Weekly autonomy-supportive self-monitoring digest.

**🔴 De-prioritize / avoid:** un-escapable hard blocking as the primary mechanism; nagging notifications; heavy points/badges gamification; any "dopamine detox" branding; all-or-nothing streaks.

---

### One-paragraph thesis
The building-block neuroscience is real, but the "engineered addiction / fried dopamine / shrinking attention" story is largely overstated. Blunt blocking fails because it provokes reactance, gets circumvented, and shatters on a single lapse. What durably works is **autonomy-supportive friction**: make the unwanted action take one more deliberate step, help the user pre-commit with *their own* if-then plans, reflect behavior back without shame, and keep an honest exit so they never feel trapped. Aim for many small, sustainable wins and an identity shift ("I use this on purpose") — not a knockout blow.

**Confidence:** strongest — implementation intentions, grayscale/friction, task-switching costs, autonomy support, notification batching, blocker-abandonment dynamics. Contested/weak — ego depletion (don't design around it), strong thought-suppression rebound, gamification magnitude, JITAI efficacy, durability of all short interventions.

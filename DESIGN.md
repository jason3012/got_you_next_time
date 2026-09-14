---
name: SettleUp
description: A field-note money trail for friends who get one another next time.
colors:
  route-terracotta: "#D57B4E"
  action-terracotta: "#AE5B39"
  resolved-sage: "#ABA784"
  journal-paper: "#F3EBDF"
  graphite-ink: "#1B1712"
  warm-sheet: "#FFFAF2"
  error-ink: "#A33E2B"
  success-ink: "#697052"
typography:
  display:
    fontFamily: "Archivo Variable, Arial, sans-serif"
    fontSize: "clamp(2.5rem, 7vw, 5.4rem)"
    fontWeight: 790
    lineHeight: 1
    letterSpacing: "-0.02em"
  title:
    fontFamily: "Archivo Variable, Arial, sans-serif"
    fontSize: "clamp(1.45rem, 4vw, 2rem)"
    fontWeight: 730
    lineHeight: 1.1
    letterSpacing: "-0.035em"
  body:
    fontFamily: "Archivo Variable, Arial, sans-serif"
    fontSize: "1rem"
    fontWeight: 400
    lineHeight: 1.5
  annotation:
    fontFamily: "Drawably Pen, Archivo Variable, sans-serif"
    fontSize: "1.05rem"
    fontWeight: 400
    lineHeight: 1.2
rounded:
  tactile: "12px"
  sheet: "18px"
  modal: "24px"
spacing:
  xs: "8px"
  sm: "12px"
  md: "16px"
  lg: "24px"
  xl: "48px"
components:
  button-primary:
    backgroundColor: "{colors.action-terracotta}"
    textColor: "{colors.journal-paper}"
    rounded: "{rounded.tactile}"
    padding: "14px 24px"
    height: "52px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.graphite-ink}"
    rounded: "{rounded.tactile}"
    padding: "12px 18px"
    height: "48px"
  field:
    backgroundColor: "{colors.warm-sheet}"
    textColor: "{colors.graphite-ink}"
    rounded: "{rounded.tactile}"
    padding: "12px 14px"
    height: "52px"
  panel:
    backgroundColor: "{colors.resolved-sage}"
    textColor: "{colors.graphite-ink}"
    rounded: "{rounded.sheet}"
    padding: "22px"
---

# Design System: SettleUp

## Overview

**Creative North Star: "The Friends' Field Notebook"**

SettleUp should feel like a shared page passed around a table: candid, useful, lightly imperfect, and easy to understand. Its warmth comes from cream paper, graphite marks, sparse terracotta routes, and intentionally simple comic faces—not from decorative excess. Notion-like restraint keeps the raw sketch language legible and trustworthy around real money.

The system pairs a bold rounded grotesk with exact, quiet sans-serif data and a handwritten face used only for annotations. Motion tells the product story: separate purchases trace toward a shared state, then phone views reveal the whole friend circle. Product screens inherit the material more quietly so transactions and balances stay primary.

**Key Characteristics:**

- Real transaction data is the visual and functional center of gravity.
- Roughness lives in drawn controls, dividers, routes, and sparse annotations.
- People are minimal comic marks or initials, never photographic or AI-realistic portraits.
- Phone layouts are canonical; wider layouts add breathing room rather than more decoration.

## Colors

The palette reads like cream sketch paper, soft graphite, fired clay route ink, and muted olive resolution.

### Primary

- **Route Terracotta**: Traces relationships, annotations, and small moments of emphasis.
- **Action Terracotta**: The deeper variant reserved for primary actions and high-value wayfinding.

### Secondary

- **Resolved Sage**: Marks connected, balanced, or socially calm states without reading like financial green.

### Neutral

- **Journal Paper**: The default page field.
- **Warm Sheet**: A slightly brighter layer for mobile navigation, dialogs, and inputs.
- **Graphite Ink**: The only standard text, border, and icon color.

**The Sparse Route Rule.** Terracotta should point, trace, or activate; it should not wash entire content surfaces.

**The Calm Money Rule.** Amounts use graphite by default. Success and error colors communicate a state, never decorate ordinary currency.

## Typography

**Display Font:** Archivo Variable (with Arial fallback)
**Body Font:** Archivo Variable (with Arial and sans-serif fallbacks)
**Annotation Font:** Drawably Pen (with Archivo Variable fallback)

**Character:** Archivo’s sturdy, gently rounded forms carry the direct confidence of the first comp without making the interface feel corporate. Its lighter weights keep exact amounts and dense transaction metadata clear. Drawably Pen supplies a fresh margin-note voice in very small doses.

### Hierarchy

- **Display** (700, responsive 2.5rem–5.4rem, 1): One strong promise or page title.
- **Title** (700, responsive 1.45rem–2rem, 1.1): Section and dialog headings.
- **Body** (400, 1rem, 1.5): Instructions and supporting copy, kept near 56 characters where practical.
- **Data** (600–700, tabular numerals): Currency and balances; exactness outranks personality.
- **Annotation** (400, about 1.05rem): Short route labels or notebook context, never paragraphs.

**The One Bold Gesture Rule.** Use Archivo to establish hierarchy, not on every component label.

## Layout

The mobile layout uses a single padded column with a persistent 68px bottom navigation and 44px minimum touch targets. Product content is capped at 1080px on wider screens. Sections breathe vertically in 44–76px intervals, while transaction rows remain compact enough to scan.

Landing composition begins as a portrait-ratio field note. The next section pins native scroll and zooms out to a five-phone ring; the animation never hijacks wheel or touch input. At 680px and below, multi-column explainers collapse, transaction actions gain a full row, and dialogs become bottom sheets. Reduced-motion users see the final composition without scrubbed movement.

**The Phone Is Canon Rule.** Desktop may spread content across available width, but it must not change the task order established on a phone.

## Elevation & Depth

The world is flat by default. Hierarchy comes from ink rules, paper tone, and overlap. A hard 3–4px sketch offset is reserved for floating phone objects and the persistent mobile navigation; dialogs use a soft ambient shadow only because they must separate from an overlay.

**The Paper-First Rule.** Do not stack generic white cards. Use a rule, spacing, or a small tonal shift before adding elevation.

## Shapes

Controls and containers use gently irregular 12–24px corners, two-pixel graphite outlines, and occasional dashed routes. Circular friend marks are deliberately imperfect. Rectangles should feel hand-cut, not pill-shaped; full circles belong to people, connection points, and resolved status marks.

## Components

### Buttons

- **Shape:** Tactile, slightly irregular corners with Drawably-generated strokes.
- **Primary:** Deep terracotta with journal-paper text, 52px high, and one clear verb.
- **Hover / Focus:** Preserve color; use the global 3px terracotta focus ring and subtle tactile response.
- **Outline:** Transparent paper with graphite text for secondary or reversible actions.

### Cards / Containers

- **Corner Style:** Irregular 18px rounding with a 2px graphite border.
- **Background:** Paper by default; muted sage-paper is reserved for bank connection state.
- **Shadow Strategy:** Flat except for true floating surfaces.
- **Internal Padding:** 18–24px on phones.

### Inputs / Fields

- **Style:** Semantic native fields wrapped by Drawably, on warm sheet paper, at least 52px tall.
- **Focus:** A visible terracotta outline outside the hand-drawn stroke.
- **Error / Disabled:** Plain-language inline error note; never rely on color alone.

### Navigation

The landing header is spare: wordmark, sign-in, and a three-line route jump. Authenticated phone views use a three-item bottom navigation with Lucide line icons and text labels. The active item receives a warm-paper patch, not a saturated tab color.

### Transaction Row

A simple merchant mark leads into merchant, account, and date; the exact amount aligns opposite. “Share” is the row's action. Imported or pending state must remain visible in words.

### Friend Mark

Use a single initial or two initials inside an imperfect graphite circle with one earthy fill. It is an identity cue, not a portrait simulation.

## Do's and Don'ts

### Do:

- **Do** start authenticated experiences with connected banks and reviewable transaction data.
- **Do** use Drawably on visible actions and fields while retaining semantic buttons, inputs, labels, and selects.
- **Do** keep amounts aligned, tabular, and readable at a glance.
- **Do** let route motion explain relationships and honor reduced-motion preferences.
- **Do** represent friends with minimal comic marks or initials.

### Don't:

- **Don't** present a generic fintech phone mockup, glassmorphism, glossy gradient, or polished AI portrait.
- **Don't** let manual expense entry visually outrank transaction import.
- **Don't** split every section into an interchangeable rounded card.
- **Don't** add handwritten type to dense data, body copy, or error messages.
- **Don't** invent testimonials, partner claims, or product metrics.

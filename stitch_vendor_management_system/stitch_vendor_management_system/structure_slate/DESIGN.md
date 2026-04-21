# Design System Specification: High-End Editorial VMS

## 1. Overview & Creative North Star
**Creative North Star: "The Architectural Ledger"**
This design system moves beyond the generic "SaaS dashboard" by embracing the principles of high-end editorial layout and architectural precision. While a Vendor Management System (VMS) requires high-density data, it does not require visual clutter. We treat data as content, using intentional white space, sophisticated tonal layering, and a "Manrope-on-Inter" typographic pairing to create an experience that feels authoritative yet effortless.

The system breaks the "template" look by eschewing traditional borders and boxes in favor of **Structural Asymmetry**. Large-scale headlines and "ghost" containers create a sense of breathing room, ensuring that even the most complex procurement workflows feel like a curated digital workspace rather than a spreadsheet.

---

## 2. Colors & Tonal Depth
Our palette is rooted in a deep, sophisticated navy (`primary: #4f6073`) and a slate-inflected neutral scale. We do not use "pure" blacks or grays; every neutral is tinted with the primary hue to maintain a cohesive atmospheric depth.

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders for sectioning or containment. 
Visual boundaries must be defined solely through background color shifts. For example, a `surface-container-low` component should sit directly on a `surface` background. The change in hex value provides all the separation necessary.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical layers—like stacked sheets of heavy-stock paper. 
- **Base Layer:** `surface` (#f7f9fb)
- **Secondary Sectioning:** `surface-container-low` (#f0f4f7)
- **Interactive Cards:** `surface-container-lowest` (#ffffff)
- **Elevated Overlays:** `surface-bright` (#f7f9fb) with Glassmorphism.

### The "Glass & Gradient" Rule
To elevate the VMS from "functional" to "premium," use Glassmorphism for floating navigation bars or filter drawers. 
- **Glass Spec:** Apply `surface-container-lowest` at 70% opacity with a `24px` backdrop-blur.
- **Signature Textures:** For primary CTAs and Hero headers, use a subtle linear gradient from `primary` (#4f6073) to `primary_dim` (#435467) at a 135-degree angle. This adds "soul" and prevents the interface from looking flat.

---

## 3. Typography: The Editorial Voice
We use a high-contrast typographic scale to guide the eye through dense vendor data.

*   **Display & Headlines (Manrope):** Our "Voice." Used for page titles and high-level metrics. Manrope’s geometric yet warm proportions provide an editorial, modern feel.
    *   *Headline-lg:* 2rem (Architectural scale for dashboard headers).
*   **Body & UI (Inter):** Our "Engine." Inter is used for all functional data, labels, and long-form text. Its high x-height ensures legibility in high-density tables.
    *   *Body-md:* 0.875rem (Standard for data rows).
    *   *Label-sm:* 0.6875rem (Uppercase with 0.05em letter spacing for table headers).

---

## 4. Elevation & Depth
In this system, depth is a function of light and layer, not CSS effects.

### The Layering Principle
Achieve hierarchy by "stacking" tiers. 
- **Example:** Place a `surface-container-lowest` card (White) on top of a `surface-container-low` (Pale Grey) background. The contrast is sufficient to imply a 4mm lift without a single pixel of shadow.

### Ambient Shadows
When a component must float (e.g., a modal or a primary dropdown), use **Ambient Shadows**:
- **Color:** A 10% opacity tint of `on_surface` (#2a3439).
- **Blur:** 32px to 64px.
- **Y-Offset:** 8px.
- *Avoid dark grey drop shadows; light doesn't work that way.*

### The "Ghost Border" Fallback
If accessibility requirements demand a border (e.g., input fields), use a **Ghost Border**: `outline_variant` (#a9b4b9) at **20% opacity**. It should be felt, not seen.

---

## 5. Components

### Buttons: The Tactile Action
- **Primary:** Gradient-fill (Primary to Primary-Dim), `DEFAULT` (8px) rounded corners. Text is `on_primary` (#f5f8ff).
- **Secondary:** `surface-container-high` background with `on_primary_container` text. No border.
- **Tertiary:** Text-only using `primary` color, with a subtle `surface-container-highest` background shift on hover.

### Structured Data Tables (High-Density)
- **Rule:** Forbid the use of horizontal or vertical divider lines. 
- **Separation:** Use `3.5` (0.75rem) vertical white space between rows.
- **Alternating Tones:** Use a subtle shift to `surface-container-low` on hover to highlight the active row.
- **Header:** `label-sm` (Inter), uppercase, `on_surface_variant` (#566166).

### Cards
- **Construction:** Use `surface-container-lowest` (#ffffff). 
- **Corner Radius:** `DEFAULT` (0.5rem / 8px) for internal cards; `lg` (1rem) for main dashboard widgets.
- **Padding:** Always use `spacing.5` (1.1rem) to prevent data from feeling cramped.

### Search & Inputs
- **Style:** "Soft Inset." Use `surface-container-highest` with a `none` border. 
- **Focus State:** Transition to a `ghost-border` of `primary` at 40% opacity.

### Component Addition: The "Status Pill"
For Vendor Status (Active, Pending, Risk), do not use heavy solid blocks of color. Use a `tertiary_container` background with `on_tertiary_container` text for a sophisticated, muted indicator that doesn't distract from primary data.

---

## 6. Do’s and Don’ts

### Do:
- **Use Asymmetry:** Place a large `headline-lg` title on the left and a cluster of `title-sm` metrics on the far right to create a dynamic, editorial balance.
- **Embrace White Space:** Use `spacing.8` (1.75rem) or `spacing.10` (2.25rem) between major modules.
- **Nesting:** Always place lighter surfaces on darker backgrounds to pull content forward.

### Don't:
- **No 1px Lines:** Never use a #CCCCCC line to separate sections. Use a background color change.
- **No Pure Grey Shadows:** Shadows must always be tinted with the `on_surface` blue-grey.
- **No Default Inter for Headlines:** Always use Manrope for headlines to maintain the "Architectural" brand identity.
- **No High-Contrast Borders:** Avoid any border that has 100% opacity.
# Papyrus — UI design brief

Use this brief to design high-fidelity mobile mockups. The app already works; only the visual design is being redone.

## The app in one paragraph

Papyrus is a free, open-source Android app for keeping personal documents organized on the phone: ID cards, passports, insurance policies, medical tests, contracts, receipts, warranties. Users add PDFs, Word files and photos, sort them into categories, add a note and an optional expiry date, and find any document in seconds with search and filters. **Everything stays on the phone**: no account, no cloud, no internet. The only way data leaves is an export/backup the user starts themselves. It will be published on F-Droid and GitHub.

## Audience and mood

- Everyday people (not tech experts), ages 25–70, managing family paperwork. Italian and English speakers.
- They open the app when they *need* a document: at a pharmacy, a police check, a car rental. Speed and calm matter more than features.
- Mood: **calm, trustworthy, private, tidy**, like a well-organized paper folder. Warm, not corporate. The name "Papyrus" suggests paper; a subtle warm/paper accent is welcome but must not look old-fashioned.
- Avoid: busy dashboards, gamification, stock-photo illustrations, anything that suggests "cloud" or "sync".

## Platform and constraints

- Android phone, portrait. Design at **412 × 915** (Pixel-size). Provide **light and dark** versions.
- Built with **Material Design 3 (Jetpack Compose)**. Stick to Material 3 components and patterns (top app bar, search bar, chips, cards, list items, FAB, bottom sheets, dialogs, menus) so the design can be implemented faithfully. Custom colors, shapes and typography are welcome.
- Fonts must be free/open-licensed (e.g. Google Fonts under OFL). Icons: **Material Symbols**.
- Accessibility: text contrast at least WCAG AA, touch targets at least 48 dp, layouts must survive large system font sizes and longer Italian text (about 30% longer than English).
- Previews exist only for **PDFs (first page) and photos**. Word and other files have no preview and need a nice fallback (e.g. a type icon or badge).

## Screens to design

### 1. Library (home): the main screen
- Top app bar: app name/logo, overflow menu (⋮) with *Categories*, *Export backup*, *Restore backup*.
- **Search field** ("Search title, notes, category"). Results update as you type and ignore accents.
- **Filter row** (horizontally scrollable chips): a *Filters* chip that shows a count when active, e.g. "Filters (2)", then *All*, then one chip per category.
- **Document list**. Each item shows: thumbnail (PDF page / photo / type fallback), title, category, file size, date added, and, when set, the expiry: "Expires 12 Mar 2027", or **"Expired 15 Oct 2026" in a warning style**.
- Floating action button **"+" to add documents** (opens the system file picker, multi-select).
- States to show:
  - **First launch, empty**: friendly message + "Tap + to add…" + a secondary action **"Restore from a backup"**.
  - Category selected but empty.
  - **No search results** with a "Clear search and filters" action.
  - Importing in progress (progress indicator) and a confirmation ("3 documents added").
- Feel free to explore: a list/grid toggle, colored category markers, or an **"Expiring soon" section** at the top.

### 2. Filters (bottom sheet)
- **Type**: multi-select PDF, Word, Images, Other.
- **Date added**: "Any date" or a picked range (opens a date-range picker), with a clear button.
- **Sort by** (single choice): Newest first, Oldest first, Title A–Z, Expiring soonest.
- Changes apply immediately.

### 3. Document details
- Back, **Delete** (asks for confirmation: "Delete “Passport”? This can't be undone."), **Save**.
- Large **preview** (tap to open), plus two actions: **Open** (in another app) and **Share**.
- Editable fields: **Title** (required, error if empty), **Category** (dropdown incl. "No category"), **Note** (multi-line), **Expiry date** (date picker, removable).
- File info: type, size, date added.

### 4. Categories
- List of categories, each with rename and delete actions. "+" to add.
- Dialogs: add/rename (name field, error "A category with this name already exists"), delete ("Its documents will be kept, without a category").
- Starter categories on first launch: Identity, Health, Home, Car, Work, Finance.

### 5. Messages
- Snackbars, e.g. "Backup saved with 12 documents", "4 documents restored · 2 already here", "This file isn't a Papyrus backup. Nothing was changed."

## Sample content for the mockups

| Title | Category | Type | Size | Added | Expiry |
|---|---|---|---|---|---|
| Passport – Andrea | Identity | PDF | 2.1 MB | 3 Jan 2026 | 12 May 2031 |
| ID card | Identity | Photo | 1.4 MB | 3 Jan 2026 | 20 Nov 2026 (soon) |
| Car insurance 2026 | Car | PDF | 640 kB | 15 Feb 2026 | Expired 15 Oct 2026 |
| Blood test results | Health | PDF | 320 kB | 9 Sep 2026 | — |
| Rental contract | Home | Word | 88 kB | 1 Jul 2025 | 30 Jun 2029 |
| Laptop receipt | Finance | Photo | 2.8 MB | 22 Mar 2026 | Warranty 22 Mar 2028 |

## What to deliver

Light and dark mockups of: Library (filled), Library (first-launch empty), Library (no results), Filters sheet, Document details, Categories (with one dialog open). Plus a small style sheet: color palette, type scale, corner radii, spacing.

Design a new "Folders" view for the Library of "Papyrus", an offline Android document manager (attach the existing Papyrus mockups and style sheet: sage #526351, mint #D5E8D0, paper #EEE5CE, surface #FAF8F3, Nunito Sans, Phosphor icons with duotone category icons, 8/12/16/28 radii). 412×915, light + dark.

Problem: categories are horizontal filter chips; with 10+ categories, finding one means scrolling a long row of chips.

The Library top bar has a view switch that today toggles List and Grid. Add a third option, Folders (show the switch as a small 3-option control or a menu: List / Grid / Folders).

Folders view:
- Grid of folder cards, 2 columns, one per category plus "No category" (only if it has documents).
- Each card: the category's duotone icon on its tint color (sage, pink, sand, blue, olive, lavender), name, document count ("12 documents"), and a small warning dot/count if documents inside are expired or expiring within 30 days.
- Optionally a peek of 2–3 document thumbnails, if it stays clean.
- Search field stays on top: typing searches all documents and shows results as a list (folders hidden).
- "+" FAB as on the other views.

Inside a folder:
- Top bar with back arrow, the category icon and name, document count.
- The documents of that category with the same rows/tiles as List/Grid, sort control, and Filters.
- "+" adds documents straight into this category.
- Empty folder state: illustration, "Nothing here yet", "Add documents" button.

Sample categories: Identity (2), Health (5, 1 expiring), Home (3), Car (4, 1 expired), Work (7), Finance (11), Pets (2), Travel (1), Tickets (0), No category (3).

Rules: 48dp touch targets, WCAG AA contrast, room for 30% longer Italian labels (Cartelle, Senza categoria, documenti), bottom navigation bar stays visible.

Deliver: Library in Folders view, inside a folder (with documents), empty folder, the view switch close-up, each light + dark.

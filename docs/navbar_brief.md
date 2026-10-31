Add a bottom navigation bar to "Papyrus", an offline Android document manager (attach the existing Papyrus mockups and style sheet: sage #526351, paper #EEE5CE, surface #FAF8F3, warning #9B442F, Nunito Sans, Material Symbols Rounded, 8/12/16/28 radii). Design at 412×915, light + dark.

Goal: make the app feel more complete and inviting, and move today's hamburger menu items into clear destinations.

Navigation bar (Material 3 NavigationBar, 4 items, icon + label, pill-shaped active indicator in mint #D5E8D0):
1. Library: the existing document list (search, filter chips, list/grid). "+" FAB sits above the bar.
2. Expiring: documents with an expiry date, grouped "Expired", "Next 30 days", "Later". Show a small badge on the tab with the number expired or expiring within 30 days.
3. Categories: the existing categories screen (colored icons, document counts, "Add category").
4. Backup: "Export backup" and "Restore backup" as large tappable cards, last backup date ("Last backup: 12 Oct 2026" or "Never"), a gentle reminder if no backup in 30 days, the privacy note "Everything stays on this device. No account. No cloud.", and an About row (version, open-source license, source code link).

Remove the hamburger menu; the top bar keeps the title and the list/grid toggle on Library.

Rules: labels always visible, 48dp touch targets, WCAG AA contrast, room for 30% longer Italian labels (Libreria, In scadenza, Categorie, Backup). Hide the bar on Document details (it has a back arrow).

Sample data: Passport (Identity, expires 12 May 2031); ID card (Identity, expires in 3 weeks); Car insurance (Car, expired 15 Oct 2026); Laptop receipt (Finance, warranty until 22 Mar 2028).

Deliver: Library with bar, Expiring, Categories with bar, Backup, each light + dark, plus a close-up of the bar showing selected, unselected and badge states.

Design high-fidelity Android mockups (412×915, light + dark) for "Papyrus", a free open-source document manager. Users keep personal documents (passport, ID, insurance, medical tests, contracts, receipts) as PDFs, Word files and photos, organized by category, with notes and expiry dates. 100% offline and private: no account, no cloud.

Users: everyday people 25–70, Italian/English, who open the app when they need a document fast. Mood: calm, trustworthy, tidy, a warm paper accent but modern. No dashboards, no cloud imagery.

Use Material Design 3 components, Material Symbols icons, an open-license Google Font. WCAG AA contrast, 48dp touch targets, room for 30% longer Italian text. Only PDFs and photos have previews; Word files need a type-icon fallback.

Screens:
1. Library: app bar with menu (Categories, Export backup, Restore backup); search field; scrollable chips "Filters (n)", All, then categories; document list: thumbnail, title, category · size · date, expiry ("Expires 12 May 2031", or "Expired 15 Oct 2026" in warning style); "+" FAB. Optional: "Expiring soon" section, grid view, category colors.
2. Library states: empty first launch with "Restore from a backup" link; no search results with "Clear filters".
3. Filters bottom sheet: Type (PDF, Word, Images, Other), Date added range, Sort (Newest, Oldest, Title A–Z, Expiring soonest).
4. Document details: large preview, Open and Share buttons, editable Title, Category dropdown, Note, Expiry date; Delete and Save in app bar.
5. Categories: list with rename/delete, "+" to add, name dialog.

Sample data: Passport (Identity, PDF, expires 12 May 2031); ID card (Identity, photo, expires in 3 weeks); Car insurance (Car, PDF, expired 15 Oct 2026); Blood test (Health, PDF); Rental contract (Home, Word); Laptop receipt (Finance, photo).

Deliver all screens plus a style sheet: palette, type scale, corner radii, spacing.

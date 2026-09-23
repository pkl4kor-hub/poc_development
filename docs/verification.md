# Verification Results

Verified locally on 2026-09-23 using Node 24.21.0, its built-in SQLite/test runner, and the integrated Chromium browser controlled with Playwright.

## Automated Checks

- `npm.cmd test`: all 16 tests passed in the final suite; domain and HTTP tests were also run after their respective modules were implemented.
- `npm.cmd run check`: storefront and both HTTP server syntax checks passed.
- Editor diagnostics: no errors reported.
- Eight local PNGs: nonempty, correct signature, 960 x 720 dimensions; additionally decoded with System.Drawing during asset preparation.
- Catalog search, category filtering, stock filtering, price sorting and specifications passed.
- Cart add/set/remove, quantity bounds, unavailable stock, tax and delivery totals passed.
- Approved/declined payments, immutable order snapshots, cleared cart and inventory decrement passed.
- Idempotent successful replay created no second order and did not decrement stock twice.
- Changed checkout payload with a reused key was rejected.
- Stale stock, stale totals, invalid customer data and empty cart were rejected.
- Session isolation prevented another session from reading an order or cart.
- Reopening a file-backed database preserved order, stock and session data.
- Competing carts for the final unit allowed only one purchase.
- A simulated order-insert failure after inventory mutation rolled back stock and retained the cart.
- HTTP session validation, malformed JSON, content type, CORS, missing resources and OpenAPI availability passed.

## Browser Journey

| Step | Observed result |
| --- | --- |
| Homepage | API-derived count: 8 products, 5 categories, 7 available products |
| Browse tools | Catalog rendered with local images and prices |
| Search/filter | `brushless` plus `Drills & drivers` returned GSR 18V-55 |
| Product details | Six specification fields and 12 units of stock displayed |
| View image | Local image decoded at 960 x 720; enlargement dialog opened and closed |
| Add to cart | Cart badge changed to 1 |
| Cart edit | Increase to 2, decrease to 1, then $169.42 standard-delivery total |
| Checkout | Fictional details accepted; express delivery changed the quote to $178.92 |
| Declined payment | Visible error; stock stayed at 12; zero orders; cart stayed at 1 |
| Approved payment | Confirmed order created for $178.92 |
| Inventory update | GSR 18V-55 stock changed from 12 to 11 |
| Order confirmation | Generated order ID, line item, customer address and totals displayed; cart cleared |
| Order history | Same order displayed after browser reload |

The browser verification created one intentional sample order in the local demo database. Its ID is `WB-34BB535A-65A9-4188-AB84-340F8406AE17`. This is a verification record, not a value embedded in application behavior. The tested browser session retains that order; other browser sessions will have their own history.

## Browser Edge Cases And Layout

- Desktop viewport: 1440 x 1000. Product details and history had no horizontal overflow; desktop screenshot inspected.
- Mobile viewport: 390 x 844. Homepage, catalog, cart, checkout, payment review and order details had no horizontal overflow; catalog and order screenshots inspected.
- Narrow viewport: 320 x 740. Checkout had no horizontal overflow.
- Empty search displayed an empty state and clear-filter action.
- Out-of-stock GOP 30-28 add button was disabled; stock filter returned seven tools.
- Forced product-image request failure displayed a readable model-named fallback; the normal asset loaded again after removing the test interception.
- Required delivery fields blocked progression when empty.
- Editing delivery from the payment review preserved entered details.
- Removing the final cart item displayed the empty-cart state and badge zero.
- The keyboard skip link focused main content without changing the application route.
- Fixed a singular item label found during browser verification and rechecked it.

The browser reported the expected HTTP 402 for the intentional declined payment and a failed image request during the forced-fallback test. These are deliberate negative tests, not unhandled application crashes. No cross-browser or production accessibility audit is claimed.

## Constraints And Remaining Limitations

Actual product photos could not be retrieved because external network/DNS access failed. Every product instead has a clearly disclosed local bitmap placeholder, as permitted by the requirement. See [image-sources.md](image-sources.md).

Backend tests are committed as runnable Node test files. Browser checks were executed through editor tools; there is no installed Playwright dependency or standalone browser test runner. Reproduce the journey using [the README](../README.md). No production payments, email, fulfillment, deployment, Docker or external infrastructure were introduced.
# Image Sources

## Status

Prepared on 2026-09-23. All eight product assets are locally generated PNG
placeholders, not actual Bosch product photography. External retrieval was
blocked in this environment. Each bitmap names the intended model and category
and visibly states "PRODUCT IMAGE UNAVAILABLE" and "Local demo placeholder - not
product photography". The generic missing-image symbol is not a product drawing.

No external photographs were downloaded, no other products were substituted,
and no optional workshop hero was created. Use the true `.png` extensions below;
no `.webp` files were created. The catalog references these local PNG assets.

## Asset Mapping

All files are under `frontend/images/`. Public paths assume the frontend directory
is served as the static root. Every source is local generation using Windows
System.Drawing; there is no external source URL for these generated assets.

| Intended Product | Public Path | Local Asset | Bytes | Source |
| --- | --- | --- | ---: | --- |
| Bosch GSR 18V-55 cordless drill / driver | `/images/gsr-18v-55.png` | [gsr-18v-55.png](../frontend/images/gsr-18v-55.png) | 32140 | Local labeled placeholder |
| Bosch GKS 190 hand-held circular saw | `/images/gks-190.png` | [gks-190.png](../frontend/images/gks-190.png) | 31512 | Local labeled placeholder |
| Bosch GST 90 BE jigsaw | `/images/gst-90-be.png` | [gst-90-be.png](../frontend/images/gst-90-be.png) | 30322 | Local labeled placeholder |
| Bosch GWS 7-115 angle grinder | `/images/gws-7-115.png` | [gws-7-115.png](../frontend/images/gws-7-115.png) | 31210 | Local labeled placeholder |
| Bosch GEX 125-1 AE random orbit sander | `/images/gex-125-1-ae.png` | [gex-125-1-ae.png](../frontend/images/gex-125-1-ae.png) | 31924 | Local labeled placeholder |
| Bosch GLM 40 laser measure | `/images/glm-40.png` | [glm-40.png](../frontend/images/glm-40.png) | 30101 | Local labeled placeholder |
| Bosch GBH 2-26 rotary hammer | `/images/gbh-2-26.png` | [gbh-2-26.png](../frontend/images/gbh-2-26.png) | 30453 | Local labeled placeholder |
| Bosch GOP 30-28 multi-cutter | `/images/gop-30-28.png` | [gop-30-28.png](../frontend/images/gop-30-28.png) | 31138 | Local labeled placeholder |

## Retrieval Attempt

Exact attempted official product-page URL:

https://www.bosch-professional.com/gb/en/products/gsr-18v-55-06019H5200

- PowerShell Invoke-WebRequest: connection closed unexpectedly.
- Node 24 fetch: `getaddrinfo ENOTFOUND www.bosch-professional.com`.
- Web-fetch tool: failed to extract meaningful content.

The page contents and any image media URLs could not be verified. This URL is a
retrieval-attempt record, not the source of the placeholder pixels. Requests for
the other seven models were not pursued after the domain-level network failure.

## Rights And Replacement

These assets are local demo placeholders, not manufacturer-supplied images or
evidence of Bosch endorsement. Product names identify the intended catalog items.
If replaced with official manufacturer imagery, that imagery is intended for the
local demo only; this is not a claim to redistribution rights. Public access to a
manufacturer image does not grant permission to redistribute it. Record the exact
verified page and media URLs and confirm applicable rights before other use.

## Validation

All eight files passed executable validation:

- Nonempty files, with byte counts recorded above.
- PNG signature exactly `89 50 4E 47 0D 0A 1A 0A`.
- Decoded successfully using `System.Drawing.Image.FromFile`.
- Decoded format GUID matched `System.Drawing.Imaging.ImageFormat.Png`.
- Decoded dimensions exactly 960 x 720 pixels for every asset.

The GSR 18V-55 placeholder was also visually inspected: text is readable and the
placeholder disclosure is visible. No packages, SVG assets, or application code
changes were required.
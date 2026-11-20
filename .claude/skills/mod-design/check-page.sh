#!/bin/sh
# Checks that a design page is self-contained: one HTML file that loads
# nothing besides itself, so that it reads offline, from GitHub Pages or from
# a copy, with no CDN, font service or runtime behind it.
#
# Usage: check-page.sh <index.html>
# Exit codes: 0 self-contained, 1 something is loaded, 2 usage.
#
# Allowed: `data:` URIs, `#` fragments, and links people follow (`<a href>`).
# Refused: any other `src`, `srcset`, `poster`, `data` or `href` of an element
# that loads it, CSS `url()` and `@import`, and network calls from scripts.

page=${1:-}
if [ -z "$page" ] || [ ! -f "$page" ]; then
  echo "usage: check-page.sh <index.html>" >&2
  exit 2
fi

# One line, so that a tag split over several lines is still one match.
text=$(tr '\r\n\t' '   ' <"$page")
q="[\"']"
quoted="(\"[^\"]*\"|'[^']*'"

# A value that loads nothing: a data URI or a fragment.
loads_nothing="^$q?[[:space:]]*(data:|#)"
# Elements that load what one of their attributes names.
tags='<(link|script|img|image|iframe|frame|embed|object|audio|video|source|track|input|use|feimage|base)[[:space:]][^>]*>'
attrs="[[:space:]](src|srcset|poster|data|href|xlink:href)[[:space:]]*=[[:space:]]*$quoted|[^[:space:]>]+)"
network="fetch\(|import\(|importScripts\(|XMLHttpRequest|new (WebSocket|EventSource)|sendBeacon\(|window\.claude|import[[:space:]][^;\"']*from[[:space:]]*$q"

problems=$(
  printf '%s\n' "$text" | grep -oiE "$tags" | grep -oiE "$attrs" |
    sed -E 's/^[[:space:]]*[^=]*=[[:space:]]*//' | grep -viE "$loads_nothing" |
    sed 's/^/loads a resource: /'
  printf '%s\n' "$text" | grep -oiE "url\([[:space:]]*$quoted|[^)]*)" |
    sed -E 's/^url\([[:space:]]*//' | grep -viE "$loads_nothing" |
    sed 's/^/loads a resource from CSS: /'
  printf '%s\n' "$text" | grep -oiE '@import[^;]*' | sed 's/^/imports a stylesheet: /'
  printf '%s\n' "$text" | grep -oE "$network" | sed 's/^/calls the network: /'
)

if [ -n "$problems" ]; then
  printf '%s\n' "$problems" | while IFS= read -r line; do echo "$page: $line"; done
  echo "$page is not self-contained: inline what it loads (styles, scripts, SVG), or embed it as a data: URI" >&2
  exit 1
fi
echo "$page is self-contained"

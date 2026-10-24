# Spreadsheet report MVP

The designer intentionally uses the existing Vue 3 stack and no third-party spreadsheet runtime. This avoids adding a license or unstable component API for the small editable-grid scope.

## Supported

- Persist and reopen cell content, basic style, merges, row heights, column widths and dataset bindings.
- Bind a cell to an owned SQL dataset; render parameters are forwarded to the existing named-parameter, read-only query engine.
- Deterministic `DOWN` and `RIGHT` expansion, a 200-row render cap, `SUM`/`COUNT` grouped bindings, and addition formulas such as `=A1+B1`.
- Existing API-key roles and resource ownership apply. Only ADMIN/EDITOR can save; owners and admins can read/render.
- Expansion into an existing cell or the non-anchor area of a merge fails with HTTP 422 instead of silently overwriting content.

## Deliberate limits

Formulas support addition of already-rendered scalar cells only. They are evaluated before dataset expansion and references are absolute in the final coordinate space. Merges are not automatically stretched or shifted. Nested regions, pagination/printing, Excel import/export, cross-sheet references and arbitrary expression evaluation are out of scope.

## Example

With the Compose `sales` data and an existing dataset selecting `region, amount`, create a template whose A4 binding uses field `region` with `DOWN`, and B4 uses field `amount` with `DOWN`. Save, preview with the SQL parameters JSON, then reopen it from the template list. The API endpoints are `POST /api/v1/report-templates`, `GET /api/v1/report-templates/{id}`, `PUT /api/v1/report-templates/{id}`, and `POST /api/v1/report-templates/{id}/render`.

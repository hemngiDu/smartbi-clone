# Spreadsheet report MVP

The Vue 3 designer and Java 21 renderer implement a small spreadsheet model without a third-party spreadsheet runtime.

## Binding regions and compatibility

Every expanding binding may have a `regionId`. Cells in one region must use the same dataset, expansion direction, grouping and aggregate settings. A `DOWN` region shares one anchor row; a `RIGHT` region shares one anchor column. The renderer executes the dataset once per region and takes every bound field from the same result row, so adjacent fields cannot drift apart.

Templates saved before `regionId` remain valid. For legacy JSON, expanding cells are grouped when they have the same dataset, direction and anchor row (`DOWN`) or anchor column (`RIGHT`). Re-saving in the designer can add an explicit ID; no database migration is required because definitions remain JSON.

```json
{"datasetId":"...","field":"region","expansion":"DOWN","aggregate":"NONE","regionId":"sales-detail"}
```

## Layout rules

Expansion is planned before any cell is emitted. A result of N records inserts N-1 rows or columns immediately after the region anchor. Original static cells, row heights, column widths and both merge endpoints are mapped through the same ordered insertion list. Region cells are then written at their mapped anchor plus the result index. Duplicate output coordinates and writes into the non-anchor portion of a merge fail with HTTP 422.

The design and preview grids use the saved/rendered pixel dimensions and CSS grid spans, so merges, row heights and column widths are visible and survive save/reopen. Overlapping or out-of-bounds merge definitions are rejected.

## Formulas

Formulas run only after layout and dataset expansion complete, independent of template traversal order. References in the saved template are mapped through the same insertions as cells. Supported expressions are cell addition (`=A1+B1`, including forward references) and a rectangular basic range (`=SUM(A1:B9)`). Empty SUM cells count as zero. Invalid references, values, syntax and dependency cycles return `#REF!`, `#VALUE!`, `#FORMULA!` and `#CYCLE!` respectively.

## Security and limits

The existing API-key roles, ownership checks, parameterized read-only query engine and 200-row report render cap remain in force. Only ADMIN/EDITOR can save; owners and admins can read/render. Nested/cross-sheet regions, pagination/printing, Excel import/export and arbitrary expression evaluation remain out of scope.

# GDmodifier diagnostics

Run **Actions → Diagnose GMD** before using the live uploader.

The diagnostic workflow does not contact Geometry Dash servers. It only checks whether the selected `.gmd` file can be read, parsed, and converted into a dry-run upload payload.

## What PASS means

A PASS means:

- the file exists
- the file is not empty
- the file parses as XML/plist-style `.gmd`
- required keys like `k2` and `k4` exist
- a dry-run payload can be built

If diagnosis passes but live upload fails, the problem is probably server-side, runner/IP blocking, account/auth rejection, or endpoint behavior.

## What FAIL means

A FAIL means the local file or repo setup is broken before networking even matters.

Common failures:

| Failure | Meaning |
|---|---|
| file does not exist | `gmd_path` is wrong |
| file is empty | the upload is not a real `.gmd` file |
| XML/plist parse error | the file format is not supported by this tool |
| missing `k2` or `k4` | the file is not a normal level export |
| levelString too short | the level data is missing or corrupted |

## Use order

1. Upload your file to `uploads/level.gmd`.
2. Run **Diagnose GMD**.
3. If it passes, try **Upload GMD to Geometry Dash**.
4. If upload fails, try changing the runner from Ubuntu to Windows or macOS.
5. If direct upload still fails, use **Import GMD** instead.

The direct uploader is the fragile route. The diagnostic workflow exists so failure stops being pure fog and starts being actual information, which is allegedly what computers were invented for.

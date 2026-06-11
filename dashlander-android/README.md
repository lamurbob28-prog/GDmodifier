# Dashlander Console

Dashlander Console is the native Android replacement for the old Termux/Python GDmodifier upload workflow.

The goal is simple: keep the Geometry Dash upload workflow usable from a phone without needing Termux, shell commands, Python, or ChatGPT babysitting every single upload like a tiny exhausted mechanic.

## What lives inside the app

The APK contains the main upload logic:

- Inspect `.gmd` metadata.
- Parse both plist-style `<dict>` exports and compact `<d>` exports.
- Build a create-new Geometry Dash upload payload with `levelID=0`.
- Rename the online level before upload.
- Choose public or unlisted upload.
- Require `UPLOAD` confirmation before sending.
- Ask for the GD password only at upload time.
- Avoid saving the GD password.
- Upload directly to Boomlings.
- Try multiple Boomlings URL/header fallbacks when the first request gets blocked.
- Save `last_upload_debug.json` after every attempt.
- View/copy the debug log and last level ID from the app.

## What the app still depends on

Dashlander does **not** depend on ChatGPT to run.

It currently depends on this GitHub repository only for input files:

- Owner: `lamurbob28-prog`
- Repo: `GDmodifier`
- Branch: `main`
- Folder: `uploads/`

The app reads `.gmd` files from:

```text
https://api.github.com/repos/lamurbob28-prog/GDmodifier/contents/uploads?ref=main
```

and downloads the selected file from raw GitHub content.

So the repo is only the file shelf. The upload brain is inside the APK.

## What can break in the future

Dashlander should keep working after a ChatGPT Pro subscription expires, because the APK and repository are independent.

The main future risks are:

- Boomlings changes upload parameters.
- Geometry Dash changes account authentication.
- GitHub raw/API access changes.
- Android blocks older debug APK installs more aggressively.
- GitHub Actions runner images change.
- The app still auto-selects the first `.gmd` in `uploads/` until a proper file picker is added.

## Build path

The workflow is:

```text
.github/workflows/build-dashlander-android.yml
```

It builds the debug APK on pushes to `dashlander-console` and on manual runs.

The workflow now avoids the Node-20 `upload-artifact` dependency. Instead, it stages the APK and publishes it to a rolling GitHub Release named:

```text
dashlander-console-latest
```

The APK asset is:

```text
dashlander-console-debug.apk
```

## Normal use

1. Put exactly the `.gmd` file you want into `main/uploads/`.
2. Open Dashlander Console.
3. Tap **Inspect GitHub uploads**.
4. Edit **Online level name** if desired.
5. Check or uncheck **Unlisted upload**.
6. Tap **Build upload preview**.
7. Confirm the preview.
8. Type `UPLOAD`.
9. Tap **UPLOAD to Geometry Dash**.
10. Copy the returned level ID or view the debug receipt.

## Maintenance notes

If uploads suddenly fail, check the app log first. The useful lines are:

- `lookup status=...`
- `upload status=...`
- `verify status=...`
- `SUCCESS. Level ID: ...`
- `WARNING: upload returned ID, but lookup did not resolve it yet.`

If GitHub builds fail, check the workflow first, then Android Gradle Plugin and Gradle versions.

If `.gmd` parsing fails, check whether the file root is `<plist>`, `<dict>`, or `<d>`. Dashlander currently supports those.

## Current status

Alpha, but functional.

Tested working features include:

- GitHub upload folder inspection.
- Compact `<d>` `.gmd` parsing.
- Create-new uploads.
- Rename before upload.
- Public/unlisted control.
- Debug receipt viewing.
- Last level ID copying.

# Dashlander Console

Native Android replacement for the Termux-based GDmodifier workflow.

Current goals:

- Read only tracked `.gmd` files from the repo uploads folder.
- Inspect level metadata before sending anything.
- Require explicit confirmation before upload.
- Save a debug receipt after every attempt.
- Avoid a shell, Termux, Python, and local stale-file confusion.

Current alpha status:

- Android project scaffold exists.
- GitHub uploads inspection is implemented.
- GMD plist metadata parsing is implemented.
- Create-new payload preview is implemented.
- Boomlings upload client is implemented.
- Live upload UI wiring is implemented behind explicit confirmation.

Status: alpha build validation in progress.

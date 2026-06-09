# How to use GDmodifier

This is the practical guide for using GDmodifier from Android Termux.

The working method is local Termux upload. GitHub Actions is useful for storing code and running dry diagnostics, but live upload from GitHub runners may be blocked by the Geometry Dash/Boomlings network path.

## The simple rule

Use this for real uploads:

```text
Android phone -> Termux -> local upload wizard -> Geometry Dash level ID
```

Do not rely on this for real uploads:

```text
GitHub Actions -> Geometry Dash upload endpoint
```

## Daily use: upload a level with the same account

1. Put the `.gmd` file in your Android Downloads folder.
2. Open Termux.
3. Go to the repo:

```sh
cd ~/GDmodifier
git pull
```

4. Copy the file into the repo uploads folder. Replace the filename if needed:

```sh
mkdir -p uploads
cp ~/storage/downloads/DaB00bsPlat2.gmd uploads/DaB00bsPlat2.gmd
```

5. Run the wizard:

```sh
python uploader/local_upload_wizard.py
```

6. If you are using the same account and same file as before, press Enter for the defaults.
7. When it asks for the burner password, type it and press Enter. The password will not visibly appear while typing.
8. Wait for either a success level ID or an error.

## If you want to upload a different file

Copy the new file from Downloads into `uploads/`:

```sh
cp ~/storage/downloads/NewLevel.gmd uploads/NewLevel.gmd
```

Run the wizard:

```sh
python uploader/local_upload_wizard.py
```

At this prompt:

```text
GMD path [uploads/DaB00bsPlat2.gmd]:
```

type:

```text
uploads/NewLevel.gmd
```

At this prompt:

```text
Level name override [DaB00bsPlat2]:
```

type the level name you want, or press Enter if the default is fine.

## If you want to use a different Geometry Dash account

First get the new accountID:

```sh
GD_USERNAME='NewUsernameHere' python uploader/probe_gd_network.py
```

Look for the account ID in the output.

Then run:

```sh
python uploader/local_upload_wizard.py
```

At the prompts, type the new account values instead of pressing Enter:

```text
GD username [SchugglyBear]: NewUsernameHere
GD accountID [42450747]: NewAccountIDHere
GD burner password: type the password for that account
```

Only use burner accounts. Do not use your main account for experimental upload tools.

## What the common prompts mean

| Prompt | What it means | What to do |
|---|---|---|
| GMD path | The file inside the repo to upload | Use `uploads/YourFile.gmd` |
| GD username | The account that will upload the level | Use the burner username |
| GD accountID | The numeric account ID | Get it with `probe_gd_network.py` |
| Visibility | Whether the level is public or unlisted | Usually `public` |
| Upload mode | Protocol variant order | Usually `modern-first` |
| Force stock song | Removes custom song metadata | Usually `true` |
| Song ID override | Manually force a song ID | Usually blank |
| Audio track override | Stock audio track number | Usually `0` |
| Level name override | Name sent during upload | Use the desired level name |

## Settings that worked

Use these defaults unless something breaks:

```text
Visibility: public
Upload mode: modern-first
Force stock song: true
Song ID override: blank
Audio track override: 0
```

## If upload fails

If the wizard returns `-1`, rerun it and change:

```text
Upload mode [modern-first]: legacy-first
```

If it shows Cloudflare, 403, or error 1005, switch networks and retry:

```text
Wi-Fi -> mobile data
mobile data -> Wi-Fi
```

If the `.gmd` cannot be found, check files:

```sh
ls -lh uploads
ls -lh ~/storage/downloads
```

If the password prompt confuses you, remember: when it asks for the password, type it immediately and press Enter. It will not show on screen.

## Updating the tool later

Before using the wizard, run:

```sh
cd ~/GDmodifier
git pull
```

That grabs the newest version from GitHub.

## What happens to old uploads

Each direct upload creates a new uploaded level ID. Uploading a different file does not replace the old one. Using a different account uploads under that different account. Old uploaded levels remain attached to the account that uploaded them.

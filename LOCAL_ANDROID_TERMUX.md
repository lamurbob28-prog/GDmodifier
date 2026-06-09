# Local Android upload path with Termux

GitHub Actions is blocked by the Geometry Dash/Boomlings network path in some cases. If the probe shows HTTP 403 or Cloudflare error 1005, do not keep retrying from Actions. Run the uploader from your own Android device/network instead.

## Install Termux

Install the current Termux app from F-Droid or the official Termux GitHub releases. Avoid the old Google Play build.

## First-time setup

Open Termux and run:

```sh
pkg update -y
pkg install -y git python
termux-setup-storage
```

Allow storage permission when Android asks.

## Clone the repo

```sh
cd ~
git clone https://github.com/lamurbob28-prog/GDmodifier.git
cd GDmodifier
```

## Copy your .gmd into the repo

If the file is in Android Downloads:

```sh
mkdir -p uploads
cp /sdcard/Download/DaB00bsPlat2.gmd uploads/DaB00bsPlat2.gmd
```

If your file has a different name, change the command and the `GMD_PATH` below.

## Run the local diagnosis

```sh
GMD_PATH='uploads/DaB00bsPlat2.gmd' python uploader/diagnose_gmd.py
```

If this fails, fix the file/path first.

## Run a network probe locally

```sh
GD_USERNAME='SchugglyBear' python uploader/probe_gd_network.py
```

If this works locally, your phone network can reach the GD endpoint even though GitHub Actions could not.

## Run the uploader locally

Use a burner Geometry Dash account. Put the burner password into the environment only for this terminal session:

```sh
export GD_PASSWORD='PUT_BURNER_PASSWORD_HERE'
export GMD_PATH='uploads/DaB00bsPlat2.gmd'
export GD_USERNAME='SchugglyBear'
export GD_ACCOUNT_ID=''
export VISIBILITY='public'
export UPLOAD_MODE='modern-first'
export FORCE_STOCK_SONG='true'
export SONG_ID_OVERRIDE=''
export AUDIO_TRACK_OVERRIDE='0'
python uploader/upload_gmd.py
```

If the username lookup fails but you know the accountID, set it manually:

```sh
export GD_ACCOUNT_ID='YOUR_ACCOUNT_ID_HERE'
python uploader/upload_gmd.py
```

## Retry order

1. Run local diagnosis.
2. Run local network probe.
3. Upload with `FORCE_STOCK_SONG=true`.
4. Retry with `UPLOAD_MODE=legacy-first`.
5. Retry with `FORCE_STOCK_SONG=false` if you want the custom song metadata preserved.

## Important

Do not commit your password. Do not put it into a repo file. Environment variables vanish when the Termux session closes, which is exactly the point.

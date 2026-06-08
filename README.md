# GDmodifier

A phone-friendly Geometry Dash `.gmd` uploader built around **GitHub Actions**.

This repo exists because stock Android Geometry Dash does not let you tap a `.gmd` file and import it locally. Lovely little monument to suffering. This tool takes the realistic path:

```text
.gmd file in this repo → GitHub Actions workflow → Boomlings upload endpoint → Geometry Dash level ID
```

Then you open Geometry Dash on your phone and search the returned level ID.

## Safety rule

Use a **burner Geometry Dash account**.

Do **not** use your main GD password. Store only the burner password in a repository secret named:

```text
GD_PASSWORD
```

The workflow reads that secret at runtime. Never commit passwords, account files, or private credentials into the repo.

## Big privacy warning

This repo is currently public. If you upload `.gmd` files into a public repo, other people may be able to see/download them. Before uploading weird/private/unreleased `.gmd` files, make the repo private or accept that it is public cargo in the digital street.

## Setup, mobile version

### 1. Add the password secret

In this repo:

1. Go to **Settings**.
2. Go to **Secrets and variables** → **Actions**.
3. Tap **New repository secret**.
4. Name it exactly:

```text
GD_PASSWORD
```

5. Value: your **burner** Geometry Dash password.
6. Save.

### 2. Upload your `.gmd`

Upload your `.gmd` into the `uploads/` folder.

The easiest path is:

```text
uploads/level.gmd
```

Simple filenames make GitHub mobile less likely to behave like a shopping cart with one bad wheel.

### 3. Run the uploader

1. Open the **Actions** tab.
2. Choose **Upload GMD to Geometry Dash**.
3. Tap **Run workflow**.
4. Fill in:

| Field | What to type |
|---|---|
| `gmd_path` | `uploads/level.gmd` or your actual path |
| `gd_username` | burner GD username |
| `gd_account_id` | leave blank first, or paste known accountID |
| `level_name_override` | optional |
| `description_override` | optional |
| `visibility` | `public` is easiest to find by ID |
| `upload_mode` | `modern-first` first, `legacy-first` if modern fails |

5. Tap **Run workflow**.

### 4. Get the result

Open the finished workflow run. If it works, the summary/log will show:

```text
SUCCESS. Level ID: 123456789
```

Open Geometry Dash → Search → enter that number.

## If account lookup fails

The workflow tries to find account ID using the GD user search endpoint. If that gets blocked or fails, use a manual account ID lookup and rerun with `gd_account_id` filled.

The account ID is not the same as player ID. In raw GD user data, account ID is key `16`.

## If upload fails

Check the workflow summary/artifact.

Common outcomes:

| Result | Meaning |
|---|---|
| `-1` | Boomlings rejected the upload, usually auth/accountID/payload issue |
| HTML / `<!doctype html>` | Cloudflare or upstream block page |
| Missing `GD_PASSWORD` | You did not create the repository secret |
| Missing `.gmd` | `gmd_path` is wrong |

Try `legacy-first` if `modern-first` fails.

## Files

```text
.github/workflows/upload-gmd.yml   manual GitHub Actions workflow
uploader/upload_gmd.py             workflow entry point
uploader/gd_proto.py               Geometry Dash protocol helper code
uploads/.gitkeep                   placeholder upload folder
```

## Reality check

This does **not** import locally into stock Android Geometry Dash. It uploads through the online level system and returns a level ID. That is the stable architecture after Cloudflare, Apps Script, and Colab all decided to cosplay as broken machinery.

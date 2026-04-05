## Introduction:
This section is for fast file transfer onto Aviary machines. A script was already provided for you to run in the [scripts](scripts) directory.

## How to run:
For fast file transferring to Aviary navigate to the [scripts](scripts) directory and execute the scripts there. You have to pass two parameters into your script in order to run:

- `UMNetId`: Your university's UMNetID.
- `PwdPath`: The path where you want your files to go to. To look for this, go to your desired directory on Aviary and run `pwd` there to get the relative path.

Once you got the desired parameters, run the script as follows:

```
.\UploadToAviary -UMNetId [your UMNetID] -PwdPath [your desired path on Aviary]
```

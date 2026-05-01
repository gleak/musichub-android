# MusicHub launcher icon · playwave · lime

## Files
- mipmap-{mdpi..xxxhdpi}/ic_launcher.png        — legacy launcher (squircle clip)
- mipmap-{mdpi..xxxhdpi}/ic_launcher_round.png  — round legacy launcher
- mipmap-{mdpi..xxxhdpi}/ic_launcher_foreground.png — adaptive foreground (108dp)
- mipmap-{mdpi..xxxhdpi}/ic_launcher_background.png — adaptive background (108dp)
- mipmap-anydpi-v26/ic_launcher.xml             — adaptive icon manifest
- drawable/ic_launcher_monochrome.svg            — Android 13+ themed (convert to vector XML in Studio)
- svg/                                            — vector sources

## How to drop into your Android project
Copy the contents of this folder into:
    app/src/main/res/

Make sure mipmap-* folders are merged with your existing ones. Android Studio will pick up the adaptive XML automatically.

## Density spec
- mdpi    48×48 px
- hdpi    72×72 px
- xhdpi   96×96 px
- xxhdpi  144×144 px
- xxxhdpi 192×192 px

Adaptive layers are exported at 2.25× each density (108dp area).

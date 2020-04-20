# What is Subtitle Merger?
It is a desktop application that allows you to merge two subtitles into one. It is helpful if you learn a new language 
and want to see bilingual subtitles or if you watch videos with someone who wants to see subtitles in a different
language. The application has two modes - the first one is very simple and allows you to choose two files with subtitles
and merge them. The second one is much more powerful and helpful - you can select one or several videos or
the whole directory, see the subtitles for these videos, merge selected and inject them to videos or save in external
files nearby.

##### Table of Contents  
[What file do I download?](#what_file)  
[Why the size of the application is so big?](#size)  

<a name="what_file"></a>
## What file do I download?
If you don't want to work with videos or if you have Ffmpeg installed download the version without the Ffmpeg since
it has smaller size. But if you want functionality and don't want to deal with Ffmpeg yourself just download the version
with it.
Ffmpeg is a very powerful and useful open source utility that this application uses to work with videos.

<a name="size"></a>
## Why the size of the application is so big?
As of april 2020, size of the application varies from 45 megabytes (version without Ffmpeg) to almost 90 megabytes
(version with Ffmpeg) although the application itself is simple enough. 
The pure .jar for the application is approximately 350 KB. After adding required libraries to the classpath and making
fat jar, size rises to 5.5 MB. Other size is taken by the custom JRE with JavaFX, it's the size I've came up with.
So basically all the size is taken by the JRE and Ffmpeg.
  

some icons have been taken from
* https://www.flaticon.com/authors/freepik, 
* https://www.flaticon.com/authors/smashicons
* https://www.flaticon.com/authors/dave-gandy,
* https://www.flaticon.com/authors/pixel-perfect
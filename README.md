<a name="download_links"></a>
# Download links
* ![Windows logo](/readme_images/windows_logo.png) Windows (x64) [87.8 MB](#set_in_release)
* ![Linux logo](/readme_images/linux_logo.png) Linux (x64) [80.6 MB](#set_in_release)

[Why is the application size so big?](#size)

# What is Subtitle Merger?
It is a desktop application that allows you to merge two subtitles into one. It is helpful if you learn a new language 
and want to see bilingual subtitles or if you watch videos with someone who wants to see subtitles in a different
language.<br/>
The main feature of the application is the ability to work with videos - you can select one or several videos, see the
list of subtitles for each video, merge selected subtitles and inject the result right back into the videos or save to
external files nearby.
![Overview](/readme_images/overview.png)


# Table of Contents  
* [What formats are supported?](#formats)  
* [Hot to use it?](#how_to_use)
    * [To merge two subtitle files into a third](#subtitle_files_tab)
    * [To work with videos](#videos_tab)
* [Other questions](#other_question)
    * [What merge mode should I choose?](#merge_mode)
    * [Why are there question marks instead of subtitle sizes?](#why_question_marks)
    * [What about the encoding?](#encoding)
    * [Why is the the application size so big?](#size)  
* [Additional features](#additional_features)
    * [Mark merged subtitles as default](#mark_as_default)
    * [Merge subtitles in plain text](#plain_text)


<a name="formats"></a>
## What formats are supported?
Currently the application supports SubRip subtitles (**.srt**) and Matroska videos (**.mkv**). It also supports
SubStation Alpha subtitles (**.ssa**, **.ass**) if they are already in videos and will convert them with Ffmpeg to 
SubRip internally.


<a name="how_to_use"></a>
## How to use it?
First of all, you need to [download the archive with the application](#download_links). After downloading the archive 
you should unpack it to any folder of your choice. After unpacking to start the application you should launch:
* ![Windows logo](/readme_images/windows_logo.png) *subtitle-merger.exe* on Windows
* ![Linux logo](/readme_images/linux_logo.png) *run.sh* on Linux.

The application has two main tabs to work with subtitles, the first one simply merges subtitles from two files into a
third one. The second tab is much more powerful and allows to work with videos.


<a name="subtitle_files_tab"></a>
### To merge two subtitle files into a third
Open the first tab called "Subtitle files". On that tab you just need to select a file with subtitles that will be
displayed at the top of the video and another file with subtitles that will go at the bottom. You also need to select 
the file where the result will be written.
<br/>
Note that there are three eye icons, clicking them will open the preview windows.
<br/>
[What about the encoding?](#encoding)
![Subtitle files](/readme_images/subtitle_files.png)


<a name="videos_tab"></a>
### To work with videos
Open the second tab called "Videos". When you open the tab for the first time you will see that three required settings
are missing.
![Missing settings](/readme_images/missing_settings.png)
After setting them ([what merge mode should I choose?](#merge_mode)) you will be able to open one or several files
(better for movies) or the whole directory (better for TV shows with many episodes).
![Please choose](/readme_images/please_choose.png)
After you see the videos you should do the following:
1. If you've chosen the whole directory you will need to mark the files you want to work with directly.
2. The next step is to select the subtitles to merge for each video, you can do so either by
    * Pressing the "Auto-select subtitles" button. The application will try to select subtitles for merging based on the 
    preferred languages that you've set on the settings tab. If there are more than one subtitles for one of the
    preferred languages, the application will load them and choose the biggest one.
    * Clicking the radio buttons next to the subtitles if auto-selecting is not possible for some videos or if you want
    to select subtitles manually.
Note that initially you can't see the subtitle sizes (the reasons for that are described [here](#why_question_marks)) 
but you can always load the subtitles by pressing the corresponding buttons. 
3. Press the "Merge" button, this is the final step.

![Table manual](/readme_images/table_manual.png)


<a name="other_question"></a>
### Other questions
Below are the most expected questions about the application.


<a name="merge_mode"></a>
#### What merge mode should I choose?
There are two merge options:
1. Modify original videos. With this option the application will inject merged subtitles into original video files. It
is the most convenient option since everything will look as before except for the new merged subtitles in the subtitle 
list. This approach requires extra disk space during the merging process (equal to the size of the largest video to
process) because the application will create a temporary video first and then will overwrite the original video with 
this temporary one. Note that the original files will be overwritten! And if something goes wrong during the merge you
may lose your video files.
2. Create separate subtitle files. With this option the application will create separate subtitle files next to the 
videos. This option is not so convenient because after merging you will have to select the subtitle files manually when
watching videos. But this option is safe because the original videos won't be modified in any way. And it also doesn't
require almost any extra disk space.


<a name="why_question_marks"></a>
#### Why are there question marks instead of subtitle sizes?
You may have noticed that table with videos and subtitle lists are loaded pretty fast but there are question marks 
instead of subtitle sizes. That's because with Ffmpeg it's very fast to get the basic video information including a list
of subtitles with their languages and titles but unfortunately, size of subtitles isn't returned among the basic
information about the video. So the application has to load the whole subtitle stream and that takes much more time
(around 10 seconds for each of the subtitles for a several gigabyte video) and because of that loading the subtitles 
happens only on demand.


<a name="encoding"></a>
#### What about the encoding?
By default all input files are considered to be UTF8-encoded. If that's not the case you can change the encoding to the
one you like in the preview window after pressing the eye icon. Feel free to do it because that's safe and does not
modify files. Application encodes resulting files with UTF8, it's not configurable. Also note that you can't change the 
encoding of subtitles that are already in the video, they are always UTF8-encoded.
![Preview encoding](/readme_images/preview_encoding.png)


<a name="size"></a>
#### Why is the application size so big?
The size of the application varies from 80 megabytes (the Linux version) to almost 90 megabytes (the Windows version)
although the application itself is relatively simple.<br/>
There are two main reasons for it:
1. 40 megabytes are taken by the custom Java Runtime Environment (JRE) which is included because starting from version 9
Java no longer has JREs, only development kits, so we can't ask users to install Java by themselves.
2. 35-45 megabytes are taken by [Ffmpeg](https://ffmpeg.org/). It is a very useful open source utility that the 
application uses internally to work with videos. 

The "pure" jar with the application takes approximately 350 KB. After adding required libraries and making a "fat" jar, 
the size rises up to 5.5 MB. All other space is taken by the JRE and Ffmpeg.


<a name="additional_features"></a>
### Additional features
The application has several additional features that can be managed through settings.


<a name="mark_as_default"></a>
#### Mark merged subtitles as default
The application can mark merged subtitles as default which means that they will be selected by video players by default 
automatically so you don't need to select them manually when watching videos. It works only if you select the "Modify
original videos" mode. 
![Mark as default](/readme_images/mark_as_default.png)

 
<a name="plain_text"></a>
#### Merge subtitles in plain text
If you want subtitles to be "pure", without any colors, different font sizes and so on you can choose that option on the 
settings tab.
![Plain text](/readme_images/plain_text.png)
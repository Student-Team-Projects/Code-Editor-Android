# vsCodeOnMinimal

In our project, "CodeEditor for Android," our primary objective was to explore the capabilities of Android's file system and ensure seamless file handling within our application. Specifically, we aimed to develop an app capable of reading and writing files and directories without encountering any issues.

Here's a summary of our findings and solutions:

## Privacy Regulations in Android

Recent privacy regulations in the Android ecosystem have significantly restricted access to user personal data and application source data. Applications are now prohibited from accessing private data belonging to other apps. However, access to public data remains available for reading and modification. Additionally, within an app's Internal Storage, it retains full control over its files.

## File Modification and Saving Solution

To solve the problem of changing and saving files, we implemented the following approach:

1. **File Location**: Identify the desired file in the non-private external storage of the device.
2. **File Transfer**: Copy the file to the local files of our application's private section.
3. **Editing**: Modify the file as required.
4. **Storage**: Save the edited file securely within the private data of our application.

We accomplished the first step by using the built-in Android file explorer, granting it read permissions in the External Storage. Subsequently, we saved the selected file in our Internal Storage, where we had already implemented convenient and effective data manipulation techniques.

## Visual Interface and Usability

Our CodeEditor has a visually attractive interface like in modern IDEs. This interface not only provides a pleasant user experience but also serves as a solid foundation for the potential development of a full-fledged IDE on the Android platform. Even in its current form, our CodeEditor is well-equipped for efficient file management and editing tasks.

In conclusion, our project has demonstrated effective strategies for navigating Android's file system regulations while providing users with a robust and user-friendly file editing experience.

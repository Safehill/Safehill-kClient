## Installation instructions

1. Install IntelliJ Idea
2. Either run configuration "Build Fat JAR" or run the following from the project root
    ```bash
    ./gradlew build jar
    ```
3. Copy the jar built in `./build/libs/safehill-kcrypto-0.1.jar`
4. Paste it to `app/libs` in the Android Studio project 
5. Right click on it in Android Studio and select "Add as a Library"
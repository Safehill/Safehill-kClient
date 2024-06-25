## Installation instructions

1. Clone the repository:
   ```bash
   git clone https://github.com/Safehill/Safehill-kCrypto.git
   ```
2. In the target app, add ```includeBuild("PATH")``` in settings.gradle.kts where ```PATH``` is the path to the cloned KCrypto project.
3. Add ```implementation("com.safehill:Safehill-kCrypto")``` in app level build.gradle.kts.
4. Sync the project.
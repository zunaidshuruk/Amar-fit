# Amar.Fit

## Setup Instructions

1. **Environment Variables (.env)**
   Create a `.env` file in the root directory (you can copy `.env.example` if it exists) and add your necessary API keys or configuration values.
   ```bash
   cp .env.example .env
   ```

2. **Compile the App**
   To compile the app and verify the build setup locally, run the following command using the included Gradle wrapper:
   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

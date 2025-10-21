# EmailReaderApplication

## Overview
EmailReaderApplication connects to your Gmail account using IMAP and reads emails. It is built with Java and Spring Boot.

## Prerequisites
- Java 17 or higher
- Maven
- A Gmail account

## Gmail Setup
1. **Enable IMAP in Gmail:**
   - Go to Gmail > Settings > See all settings > Forwarding and POP/IMAP > Enable IMAP > Save changes.
2. **Create an App Password:**
   - Go to your Google Account > Security > App passwords.
   - Select 'Mail' as the app and 'Other' for device (enter any name).
   - Copy the generated password. Do NOT use your regular Gmail password.

## Configuration
Edit `src/main/resources/application.properties`:
```
spring.application.name=EmailReaderApplication
mail.protocol=imaps
mail.host=imap.gmail.com
mail.port=993
mail.username=your-email@gmail.com
mail.password=your-app-password
```
Replace `your-email@gmail.com` and `your-app-password` with your credentials.

## Running the Application
1. Open a terminal in the project directory.
2. Build the project:
   ```
   mvn clean install
   ```
3. Run the application:
   ```
   mvn spring-boot:run
   ```

## Troubleshooting
- **Authentication failed:** Ensure IMAP is enabled and you are using an App Password.
- **Connection issues:** Check your internet connection and firewall settings.
- **Other errors:** Review the logs for details.

## License
This project is for educational purposes.


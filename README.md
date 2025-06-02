# Full-Stack TODO Application with OAuth2 Authentication

This project is a modern, full-stack TODO list application that allows users to manage their tasks.
It features robust authentication capabilities using Google, Facebook, and Microsoft via OAuth2/OpenID Connect.

**Frontend:** React (with Vite) and Tailwind CSS
**Backend:** Java Spring Boot

## Core Features

*   **User Authentication:** Sign up/log in via Google, Facebook, Microsoft.
*   **TODO Management:** Create, Read, Update, Delete (CRUD) TODO items.
*   **User-Specific Tasks:** Users only see and manage their own tasks.

## Prerequisites

*   **Node.js and npm:** For the frontend. Node.js version should ideally be >= 20.x (due to react-router-dom dependency, though it might work on v18 with warnings). You can download it from [nodejs.org](https://nodejs.org/).
*   **Java Development Kit (JDK):** Version 17 or later for the backend. You can get it from [Adoptium](https://adoptium.net/) or your preferred OpenJDK distributor.
*   **Apache Maven:** For building the backend project. Usually bundled with IDEs, or download from [maven.apache.org](https://maven.apache.org/).

## Project Structure

```
/
├── backend/      # Spring Boot application
├── frontend/     # React application
└── README.md     # This file
```

## Setup and Configuration

### 1. Clone the Repository

```bash
git clone <repository_url>
cd <repository_directory>
```

### 2. Backend Setup (`/backend` directory)

Navigate to the backend directory:
```bash
cd backend
```

#### a. Configure OAuth2 Credentials

You need to obtain OAuth2 client IDs and secrets from Google, Facebook, and Microsoft.

**General Redirect URI:**
When configuring your OAuth2 providers, you will need to specify a redirect URI. For local development, this will typically be:
`http://localhost:8080/login/oauth2/code/{registrationId}`
Where `{registrationId}` is `google`, `facebook`, or `microsoft`.

So, the redirect URIs will be:
*   `http://localhost:8080/login/oauth2/code/google`
*   `http://localhost:8080/login/oauth2/code/facebook`
*   `http://localhost:8080/login/oauth2/code/microsoft`

**Configuration File:**
Open `src/main/resources/application.properties` and update the following placeholder values with your actual credentials:

```properties
# Google
spring.security.oauth2.client.registration.google.client-id=<YOUR_GOOGLE_CLIENT_ID>
spring.security.oauth2.client.registration.google.client-secret=<YOUR_GOOGLE_CLIENT_SECRET>

# Facebook
spring.security.oauth2.client.registration.facebook.client-id=<YOUR_FACEBOOK_CLIENT_ID>
spring.security.oauth2.client.registration.facebook.client-secret=<YOUR_FACEBOOK_CLIENT_SECRET>

# Microsoft
spring.security.oauth2.client.registration.microsoft.client-id=<YOUR_MICROSOFT_CLIENT_ID>
spring.security.oauth2.client.registration.microsoft.client-secret=<YOUR_MICROSOFT_CLIENT_SECRET>
```

**Provider Specific Instructions:**

*   **Google:**
    1.  Go to the [Google Cloud Console](https://console.cloud.google.com/).
    2.  Create a new project or select an existing one.
    3.  Navigate to "APIs & Services" > "Credentials".
    4.  Click "Create Credentials" > "OAuth client ID".
    5.  Choose "Web application" as the application type.
    6.  Add the authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`.
    7.  Copy the "Client ID" and "Client Secret".

*   **Facebook:**
    1.  Go to [Facebook for Developers](https://developers.facebook.com/).
    2.  Create a new App or select an existing one (choose "Consumer" or "Business" type, then "Set up Facebook Login").
    3.  In your App's dashboard, go to "Facebook Login" > "Settings".
    4.  Add the valid OAuth redirect URI: `http://localhost:8080/login/oauth2/code/facebook`.
    5.  Under "App Settings" > "Basic", find your "App ID" (Client ID) and "App Secret" (Client Secret).

*   **Microsoft (Azure Active Directory):**
    1.  Go to the [Azure Portal](https://portal.azure.com/).
    2.  Search for and select "Azure Active Directory".
    3.  Go to "App registrations" > "New registration".
    4.  Give your application a name.
    5.  Set "Supported account types" (e.g., "Accounts in any organizational directory (Any Azure AD directory - Multitenant) and personal Microsoft accounts (e.g. Skype, Xbox)").
    6.  Set the Redirect URI: Select "Web" and enter `http://localhost:8080/login/oauth2/code/microsoft`.
    7.  Register the application.
    8.  Copy the "Application (client) ID".
    9.  Go to "Certificates & secrets" > "New client secret". Add a description and expiration, then copy the "Value" of the secret (this is your client secret).

#### b. Build the Backend

```bash
./mvnw clean install
```
(On Windows, use `mvnw.cmd clean install`)

#### c. Run the Backend

```bash
./mvnw spring-boot:run
```
Or, run the compiled JAR from the `target` directory:
`java -jar target/todoapp-0.0.1-SNAPSHOT.jar` (filename might vary)

The backend will start on `http://localhost:8080`.

### 3. Frontend Setup (`/frontend` directory)

Navigate to the frontend directory (from the project root):
```bash
cd ../frontend
# Or from backend: cd ../frontend
# Or from root: cd frontend
```

#### a. Install Dependencies

```bash
npm install
```

#### b. Run the Frontend Development Server

```bash
npm run dev
```
The frontend will be accessible at `http://localhost:5173` (Vite's default).

#### c. Build for Production (Optional)

```bash
npm run build
```
This creates a `dist` folder with optimized static assets.

## Accessing the Application

*   **Frontend Application:** [http://localhost:5173](http://localhost:5173)
*   **Backend API Base URL:** [http://localhost:8080](http://localhost:8080)
*   **H2 Database Console:** [http://localhost:8080/h2-console](http://localhost:8080/h2-console)
    *   **JDBC URL:** `jdbc:h2:mem:tododb`
    *   **User Name:** `sa`
    *   **Password:** (leave blank)

## Development Notes

*   **CORS:** The backend is configured to allow requests from `http://localhost:5173` (the frontend dev server).
*   **CSRF:** Spring Security's CSRF protection is enabled. The frontend `apiService.js` attempts to read the `XSRF-TOKEN` cookie and send it back as an `X-XSRF-TOKEN` header.
*   **Backend User Endpoint:** The frontend relies on `/api/user/me` on the backend to fetch authenticated user details.
*   **Node Version:** `react-router-dom` may show `EBADENGINE` warnings if your Node.js version is below 20.x. The application might still work, but for best compatibility, consider using Node.js v20 or higher.

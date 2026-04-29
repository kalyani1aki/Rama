Update Google OAuth to allow your IP

Step 1 — Go to Google Cloud Console

👉 https://console.cloud.google.com/apis/credentials

Step 2 — Find your OAuth 2.0 Client

Click on your OAuth 2.0 Client ID (the one used by the Rama app)

Step 3 — Add the IP to Authorized Origins

Under Authorized JavaScript origins, click Add URI and add:

http://52.59.57.14

Step 4 — Add the redirect URI

Under Authorized redirect URIs, click Add URI and add:

http://52.59.57.14

http://rama-app.duckdns.org/
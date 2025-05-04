Sherlook Search Engine - README
===============================

---------------------------
Build Instructions
---------------------------

1. Ensure you have Maven installed for the backend.
2. To compile the backend and apply formatting:

   mvn spotless:apply && mvn clean install -DskipTests

This will produce a jar file at:
   target/sherlook-1.0-SNAPSHOT.jar

---------------------------
How to Run the Backend
---------------------------

You must run the following commands in order:

1. Crawl websites:
   java -jar target/sherlook-1.0-SNAPSHOT.jar crawl

2. Index the crawled data:
   java -jar target/sherlook-1.0-SNAPSHOT.jar index

3. Run the PageRank algorithm:
   java -jar target/sherlook-1.0-SNAPSHOT.jar page-rank

4. Serve the engine:
   java -jar target/sherlook-1.0-SNAPSHOT.jar serve

---------------------------
How to Run the Client
---------------------------

1. Navigate to the client directory:
   cd client

2. Copy the example environment variables to create the `.env` file:
   cp .env.example .env

3. Install the necessary dependencies:
   npm install

4. Run the client development server:
   npm run dev

5. Open your browser and navigate to `localhost:5173` (or the host printed in the terminal).

---------------------------
Notes
---------------------------

- Make sure required configuration files (e.g., application.properties for number of threads etc...) are properly set.


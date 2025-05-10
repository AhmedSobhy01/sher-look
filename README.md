<div align="center">
  <img src="https://github.com/user-attachments/assets/86f89cb6-9fc0-4157-86a0-ceb8c1d0850f" alt="Logo" />
</div>

# Sherlook Search Engine 🔎

Sherlook Search Engine is a fast, efficient search engine designed to crawl, index, and rank web pages while providing smart query suggestions and a responsive web interface.

## Overview 🚀

The project is divided into several modules, each providing a critical function:

-   **Web Crawler** 🤖
-   **Indexer** 📚
-   **Query Processor** 🔍
-   **Phrase Searching** 📝
-   **Boolean Operators Support 🔀**
-   **Ranker** 📊
-   **Web Interface** 💻

## Modules Description

### Web Crawler 🤖

-   **Functionality:**  
    The crawler starts with a seed set of URLs, downloads HTML documents, and extracts hyperlinks recursively.
-   **Key Requirements:**
    -   Ensure each page is visited only once by normalizing URLs.
    -   Only crawl specific document types (HTML).
    -   Maintain state to resume crawling without revisiting pages.
    -   Respect web administrators' exclusions (using Robots.txt).
    -   Offer a multithreaded implementation with customizable thread counts.

### Indexer 📚

-   **Functionality:**  
    Indexes downloaded HTML documents to map words (with their importance in titles, headers, or body) to corresponding documents.
-   **Key Requirements:**
    -   Persistence: The index is stored in the database.
    -   Fast retrieval of documents when queried based on specific words.
    -   Support incremental updates with newly crawled content.
-   **Performance:**
    -   Processes approximately 6000 documents in less than 2 minutes.

### Query Processor 🔍

-   **Functionality:**  
    Handles user search queries by preprocessing and finding relevant documents based on word stemming. For example, the query “travel” matches variants like “traveler” and “traveling.”

### Phrase Searching 📝

-   **Functionality:**  
    Supports quoted phrase searching to return only pages containing the exact word order. For instance, searching for `"football player"` returns only those pages with the exact phrase.

### Boolean Operators Support 🔀

-   Supports Boolean operators (AND/OR/NOT) with a maximum of two operations per query, e.g., `"Football player" OR "Tennis player"`

### Ranker 📊

-   **Functionality:**  
    Ranks search results based on relevance and page popularity.
-   **Relevance:**  
    Calculated using factors such as tf-idf or appearance in titles/headers.
-   **Popularity:**  
    Measured using algorithms like PageRank, independent of the query.
-   **Performance:**
    -   First hit rendered in 20–50 ms
    -   Subsequent hits in less than 5 ms

### Web Interface 💻

-   **Functionality:**  
    Provides an interactive search interface that:
    -   Displays results similar to Google/Bing (with title, URL, and snippet with bolded query words).
    -   Shows query processing time.
    -   Implements pagination (e.g., 200 results over 20 pages).
    -   Offers interactive query suggestions based on popular completions.

## Build Instructions ⚙️

1. **Ensure Maven is installed** for compiling the backend.
2. **Compile and format the backend:**

    ```sh
    mvn spotless:apply && mvn clean install -DskipTests
    ```

    The build produces a jar file at: sherlook-1.0-SNAPSHOT.jar

## How to Run the Backend 🏃‍♂️

Execute the following commands in order:

1. **Crawl Websites:**

    ```sh
    java -jar target/sherlook-1.0-SNAPSHOT.jar crawl
    ```

2. **Index the Crawled Data:**

    ```sh
    java -jar target/sherlook-1.0-SNAPSHOT.jar index
    ```

3. **Run the PageRank Algorithm:**

    ```sh
    java -jar target/sherlook-1.0-SNAPSHOT.jar page-rank
    ```

4. **Serve the Engine:**

    ```sh
    java -jar target/sherlook-1.0-SNAPSHOT.jar serve
    ```

## How to Run the Client 💻

1. **Navigate to the Client Directory:**

    ```sh
    cd client
    ```

2. **Create the Environment File:**

    ```sh
    cp .env.example .env
    ```

3. **Install Dependencies:**

    ```sh
    npm install
    ```

4. **Start the Development Server:**

    ```sh
    npm run dev
    ```

5. **Open Your Browser:**

    Navigate to [http://localhost:5173](http://localhost:5173) (or the host printed in the terminal).

## Running with Docker 🐳

If you prefer to use Docker to manage services, follow these instructions:

### Build and Run Services

```bash
# Build and start all services
docker-compose up --build

# Run in detached mode
docker-compose up -d

# Build and start only the client
docker-compose up --build client

# Build and start only the backend
docker-compose up --build app
```

### Running Different Modes

```bash
# Run the crawler in a container
docker-compose run --rm app crawl

# Run the indexer in a container
docker-compose run --rm app index

# Calculate page ranks in a container
docker-compose run --rm app page-rank

# Run the backend server
docker-compose up app

# Run the frontend client
docker-compose up client
```

## Notes ✍️

-   Ensure that required configuration files (e.g., `application.properties`) are correctly set.
-   Both the backend and client need to be running for a complete search experience.

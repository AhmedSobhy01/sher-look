const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

export async function fetchSearchResults(query, page = 1, resultsPerPage = 10) {
    try {
        const url = `${API_URL}/search?query=${encodeURIComponent(query)}&page=${page}&resultsPerPage=${resultsPerPage}`;

        const response = await fetch(url, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
            },
            mode: "cors",
            credentials: "include",
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`API error: ${response.status} - ${errorText}`);
        }

        return await response.json();
    } catch (error) {
        console.error("Search API error:", error);
        throw error;
    }
}

export function transformSearchResults(apiResponse, query) {
    return {
        query: query,
        timeTaken: apiResponse.timeMs,
        totalDocuments: apiResponse.totalDocuments,
        totalPages: apiResponse.totalPages,
        currentPage: apiResponse.page || 1,
        data: apiResponse.results.map((doc, index) => ({
            id: index + 1,
            url: doc.url || doc.documentUrl,
            title: doc.title || doc.documentTitle || "Untitled",
            description: doc.snippet || doc.description || "",
            highlights: Array.isArray(doc.highlights) ? doc.highlights : [],
        })),
    };
}

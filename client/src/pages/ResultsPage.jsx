import React, { useState, useEffect } from "react";
import SearchBar from "@/components/SearchBar";
import { useNavigate, useSearchParams } from "react-router";
import Logo from "@/assets/Logo.png";
import HighlightText from "@/components/HighlightedText";
import { redirect } from "react-router-dom";
import SkeletonLoader from "@/components/SkeletonLoader";
import { fetchSearchResults, transformSearchResults } from "@/services/api";

const ResultsPage = function () {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const [data, setData] = useState({ data: [], timeTaken: 0, query: "", totalDocuments: 0 });
    const [isLoading, setIsLoading] = useState(true);
    const [isLoaded, setIsLoaded] = useState(false);
    const [showResults, setShowResults] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        const searchTerm = searchParams.get("query");

        if (!searchTerm || searchTerm.trim() === "") {
            navigate("/");
            return;
        }

        const fetchData = async () => {
            setIsLoading(true);
            setError(null);

            try {
                const apiResponse = await fetchSearchResults(searchTerm);
                const transformedData = transformSearchResults(apiResponse, searchTerm);
                setData(transformedData);
            } catch (err) {
                console.error("Failed to fetch search results:", err);
                setError("Failed to fetch search results. Please try again later.");
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, [searchParams, navigate]);

    useEffect(() => {
        setIsLoaded(true);
        if (!isLoading) {
            const timer = setTimeout(() => setShowResults(true), 300);
            return () => clearTimeout(timer);
        } else {
            setShowResults(false);
        }
    }, [isLoading]);

    if (data.data && data.data.length > 0) {
        data.data.sort((a, b) => a.id - b.id);
    }

    return (
        <div className="bg-gray-50 min-h-screen">
            <header className={`flex items-center justify-left mb-6 shadow-sm py-4 px-8 bg-white transition-all duration-500 ${isLoaded ? "opacity-100" : "opacity-0"}`}>
                <img src={Logo} className="h-12 w-auto object-cover cursor-pointer ml-8 hidden sm:block mr-2 md:mr-4 lg:mr-8 md:ml-12 lg:ml-0 hover:scale-105 transition-transform" onClick={() => navigate("/")} />
                <div className="max-w-2xl w-full">
                    <SearchBar shadow={false} value={searchParams.get("query")} />
                </div>
            </header>

            <main className="max-w-3xl mr-8 ml-8 sm:ml-16 md:ml-20 lg:ml-42 pb-12">
                {isLoading ? (
                    <>
                        <div className="animate-pulse h-4 bg-gray-200 rounded w-48 mb-6"></div>
                        <SkeletonLoader count={5} />
                    </>
                ) : error ? (
                    <div className="p-4 bg-red-50 border border-red-200 rounded-md text-red-700">{error}</div>
                ) : (
                    <>
                        <p className={`text-gray-600 text-sm mb-6 transition-all duration-300 ${isLoaded ? "animate-fade-in" : "opacity-0"}`}>
                            About <span className="font-medium">{data.totalDocuments || data.data.length}</span> results
                            <span className="font-medium">
                                {" "}
                                "{data.query}" took <span className="font-semibold">{data.timeTaken < 100 ? `${data.timeTaken}ms` : `${(data.timeTaken / 1000).toFixed(2)}s`}</span>
                            </span>
                        </p>

                        {data.data.length === 0 ? (
                            <div className="text-center py-12">
                                <p className="text-xl text-gray-600 mb-4">No results found</p>
                                <p className="text-gray-500">Try different keywords</p>
                            </div>
                        ) : (
                            <ul className="space-y-6">
                                {data.data.map((result, index) => (
                                    <li key={result.url} className={`card ${showResults ? "animate-slide-up" : "opacity-0 translate-y-8"}`} style={{ animationDelay: `${index * 100}ms` }}>
                                        <a href={result.url} target="_blank" rel="noopener noreferrer" className="block group">
                                            <p className="text-xl font-semibold group-hover:text-blue-600 transition-colors duration-200">{result.title}</p>
                                            <p className="text-emerald-600 text-sm truncate mt-1">{result.url}</p>
                                        </a>
                                        <div className="mt-2">
                                            <HighlightText text={result.description} className="text-gray-600 text-sm leading-relaxed" />
                                        </div>
                                    </li>
                                ))}
                            </ul>
                        )}
                    </>
                )}
            </main>
        </div>
    );
};

ResultsPage.action = async function ({ request }) {
    let formData = await request.formData();
    let query = formData.get("query");
    if (query) return redirect(`/search?query=${query}`);
};

export default ResultsPage;
